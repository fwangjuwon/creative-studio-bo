# ns-aimage-dev GCP 환경 구성 아키텍처 및 작업 순서

> **프로젝트**: ns-aimage-dev
> **최초 작성일**: 2026-03-19
> **상태**: 진행 중

---

## 전체 아키텍처

```
[사용자 - 사내망 / VPN]
        ↓ HTTPS
[Cloud Storage (FO, Vue.js 정적 파일)]      [Cloud Run (BO, Spring Boot)]
        ↓ API 호출 (CORS)                          ↓
                                        ┌──────────────────────────┐
                                        │  ns-infr-vpc-prod        │
                                        │  VPC Connector           │
                                        │       ↓                  │
                                        │  Cloud SQL (PostgreSQL)  │
                                        │  Private IP (PSA)        │
                                        └──────────────────────────┘
                                                   ↓ 직접 Egress
                                        [Gemini API - googleapis.com]

[Gitea - NS IDC 내부망 (172.21.9.0/24)]
        ↓ Webhook (인터넷 Outbound)
[Cloud Build Trigger]
        ↓ git clone (Cloud VPN 경유)
[Cloud Build Private Pool (172.31.56.0/23)]
        ↓ docker build / npm build
[Artifact Registry] → [Cloud Run 배포]
[Cloud Storage (FO 버킷)]
```

---

## 구성 요소 목록

| 서비스 | 용도 | 프로젝트 |
|--------|------|---------|
| Cloud Run (`ns-aimage-bo`) | Spring Boot BO API | ns-aimage-dev |
| Cloud Storage (`ns-aimage-fo`) | Vue.js FO 정적 파일 서빙 | ns-aimage-dev |
| Cloud Storage (`ns-aimage-images`) | NanaBanana 생성 이미지 저장 | ns-aimage-dev |
| Cloud SQL (`ns-aimage-dev-db`) | PostgreSQL 15 DB | ns-aimage-dev |
| Artifact Registry (`ns-aimage-docker`) | Docker 이미지 저장 | ns-aimage-dev |
| Secret Manager | API Key, JWT Secret, DB 접속 정보 | ns-aimage-dev |
| Cloud Build Private Pool (`ns-aimage-cloudbuild-pool-01`) | CI/CD 빌드 워커 | ns-aimage-dev |
| VPC Connector (`ns-aimage-vpc-connector`) | Cloud Run ↔ Cloud SQL 내부 통신 | ns-infr-host-402505 |
| Cloud SQL PSA 서브넷 (`ns-aimage-dev-private-access`) | Cloud SQL Private IP 할당 | ns-infr-host-402505 |
| Cloud Build 서브넷 (`ns-aimage-dev-build-access`) | Private Pool 워커 IP 대역 | ns-infr-host-402505 |

---

## 전체 진행 단계

---

### PHASE 0. 사전 협의 및 준비
| 항목 | 내용 |
|------|------|
| 담당 | 개발팀 + 인프라팀 |
| 상태 | ⏳ 진행 중 |

#### 인프라팀 요청 사항

| 요청 항목 | 필요 비트 | 용도 | 참고 레퍼런스 |
|-----------|----------|------|--------------|
| `ns-aimage-dev-private-access` 서브넷 생성 | **/24** | Cloud SQL PSA | `ns-aiplatform-dev-private-access` (172.31.22.0/24) |
| `ns-aimage-dev-build-access` 서브넷 생성 | **/23** | Cloud Build Private Pool | `ns-aiplatform-dev-build-access` (172.31.26.0/24) |
| `ns-aimage-vpc-connector` 서브넷 생성 | **/28** | Cloud Run VPC Connector | Serverless VPC Access 전용 |
| `ns-aimage-dev` Shared VPC 서비스 프로젝트 연결 | — | `ns-infr-vpc-prod` 사용 권한 | `ns-aicc-dev` 동일 방식 |
| 방화벽 규칙 추가 | — | Cloud Build Pool → Gitea | TCP 443/3000 허용 |
| VPN 라우팅 설정 | — | Cloud Build Pool ↔ Gitea 내부망 통신 | Cloud Build Pool Worker IP 대역 라우팅 |

#### 서브넷 용도 및 비트 필요 이유 상세

**① `ns-aimage-dev-private-access` — /24 (256개 주소)**

Cloud SQL Private Service Access(PSA)용 서브넷입니다.
Cloud SQL은 Google 관리형 서비스로, GCP 내부적으로 다수의 서비스 인프라(프록시, 모니터링, HA 등)에 IP를 자동 할당합니다.
이 범위는 Cloud SQL 인스턴스 IP 및 Google 내부 관리용 IP 전용으로 사용되며, 다른 리소스와 공유하지 않습니다.
PSA 최소 요구사항이 /24이며, Google 권장 사양입니다.

**② `ns-aimage-dev-build-access` — /23 (512개 주소)**

Cloud Build Private Pool 워커 VM이 사용하는 서브넷입니다.
Private Pool은 빌드 요청이 들어올 때마다 내부 워커 VM을 동적으로 생성하며, 각 워커 VM에 IP를 할당합니다.
동시 빌드 수 증가 대비 및 Google 내부 관리 IP 포함 시 충분한 주소 공간이 필요합니다.
Google Cloud Build Private Pool 최소 요구사항이 /23입니다.

**③ `ns-aimage-vpc-connector` — /28 (16개 주소)**

Cloud Run이 VPC 내부 리소스(Cloud SQL Private IP)에 접근하기 위한 Serverless VPC Access Connector 전용 서브넷입니다.
Cloud Run은 기본적으로 VPC 외부에서 실행되므로, VPC Connector를 통해 내부망 통신을 합니다.
Connector 자체가 내부 VM 인스턴스를 사용하며, Google이 요구하는 최소 크기가 /28입니다.
해당 서브넷은 VPC Connector 외 다른 리소스에서 사용할 수 없습니다.

- [ ] 인프라팀에 서브넷 할당 요청 전달
- [ ] Shared VPC 서비스 프로젝트 연결 확인
- [ ] 방화벽 규칙 및 VPN 라우팅 작업 완료 확인

---

### PHASE 1. GCP 기본 인프라 구성

#### STEP 1. GCP 프로젝트 생성 및 Artifact Registry
| 위치 | `ns-aimage-dev` |
|------|------|
| 상태 | ✅ 완료 |

- [x] GCP 프로젝트 `ns-aimage-dev` 생성
- [x] Artifact Registry API 활성화
- [x] Artifact Registry 레포지토리 `ns-aimage-docker` 생성
  - Format: Docker
  - Region: asia-northeast3
- [x] Service Account `artifact-sa` 생성 + Artifact Registry Writer 권한 부여
- [x] docker push 연결 검증 완료

---

#### STEP 2. Secret Manager 구성
| 위치 | `ns-aimage-dev` |
|------|------|
| 상태 | ⏳ 진행 중 (DB 항목 미등록) |

- [x] `gemini-api-key` — Google AI Studio API Key 등록
- [x] `jwt-secret` — openssl rand -base64 32 생성 값 등록
- [ ] `postgres-host` — Cloud SQL Private IP 확정 후 등록
- [ ] `postgres-db` — `ns_aimage` 등록
- [ ] `postgres-user` — `postgres` 등록
- [ ] `postgres-password` — 등록
- [ ] `gitea-access-token` — Cloud Build Private Pool 구성 후 등록

---

#### STEP 3. Cloud SQL 구성
| 위치 | `ns-aimage-dev` + `ns-infr-host-402505` |
|------|------|
| 상태 | ⏸ PHASE 0 인프라팀 작업 완료 후 진행 |

- [ ] Cloud SQL 인스턴스 생성
  - Instance ID: `ns-aimage-dev-db`
  - Version: PostgreSQL 15
  - Edition: Enterprise / Sandbox
  - Machine: 1 vCPU / 3.75GB (초기) → db-standard-2 (실사용 전 업그레이드)
  - Storage: 10GB SSD + Auto increase 활성화
  - Network: `ns-infr-vpc-prod` + Private Service Access (Automatic IP range)
  - Public IP: Disabled
- [ ] Database `ns_aimage` 생성
- [ ] Secret Manager DB 관련 항목 4개 등록 (`postgres-host`, `postgres-db`, `postgres-user`, `postgres-password`)

---

#### STEP 4. Cloud Storage 구성
| 위치 | `ns-aimage-dev` |
|------|------|
| 상태 | ⏸ 대기 |

- [ ] FO 정적 파일 버킷 `ns-aimage-fo` 생성
  - Region: asia-northeast3
  - 공개 접근: allUsers → Storage Object Viewer
  - 웹사이트 설정: index.html / 404.html
- [ ] 이미지 버킷 `ns-aimage-images` 생성
  - Region: asia-northeast3
  - Lifecycle Policy: Age > 30일 → Delete
- [ ] CORS 설정 (FO 버킷 → BO Cloud Run 도메인 허용)

---

### PHASE 2. 애플리케이션 코드 준비

#### STEP 5. BO 코드 수정 (ns-creative-studio-bo)
| 위치 | 로컬 개발 |
|------|------|
| 상태 | ⏳ 진행 중 |

- [x] `Dockerfile` 작성 (멀티스테이지 빌드 — JDK17 빌드 / JRE17 실행)
- [x] `.dockerignore` 작성
- [x] `src/main/resources/application-gcp.yml` 작성 (GCP Spring 프로파일)
- [ ] `SecurityConfig.java` — CORS 설정 추가 (`CORS_ALLOWED_ORIGINS` 환경변수)
- [ ] `cloudbuild.yaml` 작성
  - git clone (gitea-access-token)
  - docker build → Artifact Registry push
  - Cloud Run 배포
- [ ] NanaBanana 이미지 저장: 로컬 파일시스템 → **GCS SDK** 변경 ⚠️
  - Cloud Run 재시작 시 로컬 파일 소멸 문제 해결 필요
  - `ImageStorageConfig.java` → GCS 버킷 저장으로 변경

---

#### STEP 6. FO 코드 수정 (ns-creative-studio-fo)
| 위치 | 로컬 개발 |
|------|------|
| 상태 | ⏸ 대기 |

- [ ] `src/api/index.js` — API baseURL 환경변수화
  - `baseURL: import.meta.env.VITE_API_BASE_URL || '/api'`
  - 빌드 시 `VITE_API_BASE_URL=https://aimage-api.nsmall.com` 주입
- [ ] `cloudbuild.yaml` 작성
  - git clone (gitea-access-token)
  - npm run build
  - gsutil rsync → `ns-aimage-fo` 버킷

---

### PHASE 3. CI/CD 파이프라인 구성

#### STEP 7. Cloud Build Private Pool 구성
| 위치 | `ns-aimage-dev` + `ns-infr-host-402505` |
|------|------|
| 상태 | ⏸ PHASE 0 인프라팀 작업 완료 후 진행 |

- [ ] Cloud Build Private Pool 생성
  - 이름: `ns-aimage-cloudbuild-pool-01`
  - Region: asia-northeast3
  - Worker IP: 172.31.56.0/23
  - Peered VPC: `ns-infr-host-402505/ns-infr-vpc-prod`
- [ ] `gitea-access-token` Secret Manager 등록
- [ ] Cloud Build 서비스 계정 권한 부여
  - `roles/artifactregistry.writer` — Docker 이미지 push
  - `roles/run.developer` — Cloud Run 배포
  - `roles/secretmanager.secretAccessor` — gitea-access-token 조회
  - `roles/storage.objectAdmin` — FO 버킷 파일 업로드

---

#### STEP 8. Cloud Build Trigger 설정
| 위치 | `ns-aimage-dev` |
|------|------|
| 상태 | ⏸ STEP 5, 6, 7 완료 후 진행 |

- [ ] BO Trigger 생성
  - Type: Webhook
  - Repository: `https://nsm-cloud-git.nsmall.com/ai-playground/ns-creative-studio-bo`
  - Pool: `ns-aimage-cloudbuild-pool-01`
- [ ] FO Trigger 생성
  - Type: Webhook
  - Repository: `https://nsm-cloud-git.nsmall.com/ai-playground/ns-creative-studio-fo`
  - Pool: `ns-aimage-cloudbuild-pool-01`
- [ ] Gitea에서 BO / FO Webhook URL 각각 등록

---

### PHASE 4. Cloud Run 서비스 배포

#### STEP 9. VPC Connector + Cloud Run 생성
| 위치 | `ns-aimage-dev` |
|------|------|
| 상태 | ⏸ STEP 3, 5 완료 후 진행 |

- [ ] VPC Connector 생성
  - 이름: `ns-aimage-vpc-connector`
  - Network: `ns-infr-vpc-prod`
  - Region: asia-northeast3
- [ ] Cloud Run 서비스 `ns-aimage-bo` 생성
  - Region: asia-northeast3
  - Image: `asia-northeast3-docker.pkg.dev/ns-aimage-dev/ns-aimage-docker/ns-aimage-bo:latest`
  - min-instances: 1 (Cold Start 방지)
  - CPU: 2 / Memory: 2GiB
  - 환경변수: `SPRING_PROFILES_ACTIVE=gcp`
  - Secret 연결 (환경변수로 주입):
    - `GEMINI_API_KEY` ← `gemini-api-key`
    - `JWT_SECRET` ← `jwt-secret`
    - `POSTGRES_HOST` ← `postgres-host`
    - `POSTGRES_DB` ← `postgres-db`
    - `POSTGRES_USER` ← `postgres-user`
    - `POSTGRES_PASSWORD` ← `postgres-password`
  - VPC Connector: `ns-aimage-vpc-connector`
  - Allow unauthenticated: true (앱 레벨 JWT 인증 사용)
- [ ] Cloud Run SA에 `roles/cloudsql.client` 역할 부여
- [ ] 도메인 연결: `aimage-api.nsmall.com`

---

### PHASE 5. DB 초기화 및 검증

#### STEP 10. DB 스키마 및 초기 데이터 적용
| 위치 | Cloud SQL |
|------|------|
| 상태 | ⏸ STEP 3, 9 완료 후 진행 |

- [ ] Cloud SQL Auth Proxy 또는 Cloud Shell로 DB 접속
- [ ] `db/schema.sql` 실행 (테이블 DDL)
- [ ] `db/data.sql` 실행 (초기 데이터 — ADMIN 계정, 카테고리/태그 시드)

---

#### STEP 11. 전체 검증 및 오픈
| 상태 | ⏸ 전체 완료 후 |
|------|------|

- [ ] Cloud Run BO API 기동 확인 (`/swagger-ui/index.html`)
- [ ] FO Cloud Storage 서빙 확인
- [ ] FO → BO API 통신 확인 (CORS)
- [ ] 로그인 → 프롬프트 생성 → 이력 조회 E2E 테스트
- [ ] NanaBanana 이미지 생성 → GCS 저장 → URL 반환 확인
- [ ] Cloud Build CI/CD 자동 배포 확인 (Gitea push → 자동 배포)
- [ ] DB 스펙 업그레이드 (db-standard-2) 실사용 전 진행

---

## 전체 진행 현황 요약

| Phase | 내용 | 상태 |
|-------|------|------|
| PHASE 0 | 인프라팀 협의 | ⏳ 진행 중 |
| PHASE 1 — STEP 1 | Artifact Registry | ✅ 완료 |
| PHASE 1 — STEP 2 | Secret Manager | ⏳ 진행 중 |
| PHASE 1 — STEP 3 | Cloud SQL | ⏸ 인프라팀 대기 |
| PHASE 1 — STEP 4 | Cloud Storage | ⏸ 대기 |
| PHASE 2 — STEP 5 | BO 코드 작업 | ⏳ 진행 중 |
| PHASE 2 — STEP 6 | FO 코드 작업 | ⏸ 대기 |
| PHASE 3 — STEP 7 | Cloud Build Pool | ⏸ 인프라팀 대기 |
| PHASE 3 — STEP 8 | Cloud Build Trigger | ⏸ 대기 |
| PHASE 4 — STEP 9 | Cloud Run | ⏸ 대기 |
| PHASE 5 — STEP 10 | DB 초기화 | ⏸ 대기 |
| PHASE 5 — STEP 11 | 검증 및 오픈 | ⏸ 대기 |

---

## ns-aicc-dev 프로젝트 구성 현황 (참고)

> ns-aicc-dev는 IDC SA 서버에서 VPN을 통해 Vertex AI를 호출하는 구조입니다.
> ns-aimage-dev의 Vertex AI 전환 시 동일한 방식으로 적용 가능합니다.

### 아키텍처

```
[SA 서버 - IDC]
  google-genai SDK + Service Account Key 인증
       ↓
[Cloud VPN Tunnel]
       ↓
[ns-infr-vpc-prod]
       ↓
[PSC Forwarding Rule (고정 Private IP)]
       ↓
[asia-northeast3-aiplatform.googleapis.com]
       ↓
[Vertex AI Gemini - ns-aicc-dev]
```

### 구성 완료 단계

| STEP | 내용 | 위치 | 상태 |
|------|------|------|------|
| 1 | Private Google Access 활성화 | `ns-infr-host-402505` | ✅ 완료 |
| 2 | Shared VPC 서비스 프로젝트 연결 (`ns-aicc-dev`) | `ns-infr-host-402505` | ✅ 완료 |
| 3 | PSC 고정 IP 예약 | `ns-infr-host-402505` | ✅ 완료 |
| 4 | PSC 포워딩룰 생성 + Private DNS Zone + A 레코드 추가 | `ns-infr-host-402505` | ✅ 완료 |
| 5 | BGP 광고 추가 (PSC 대역 IDC 라우팅) | `ns-vpn-router-prod` (MSP 요청) | ✅ 완료 |
| 6 | Service Account 생성 + Vertex AI 권한 부여 + Key 발급 | `ns-aicc-dev` | ✅ 완료 |
| 7 | SA 서버 SDK 설치 + Key 배포 + 동작 확인 | IDC / SA 서버 | ✅ 완료 |

### ns-aimage-dev 적용 시 차이점

| 항목 | ns-aicc-dev | ns-aimage-dev (적용 시) |
|------|-------------|------------------------|
| 호출 주체 | IDC SA 서버 | GCP Cloud Run |
| 인증 방식 | Service Account JSON Key | Cloud Run 연결 SA (Key 불필요) |
| 네트워크 경로 | VPN → PSC | Cloud Run → VPC Connector → PSC |
| PSC 설정 | 동일 | 동일 (ns-infr-host-402505에 추가) |
| BGP 광고 | MSP 요청 필요 | 기존 광고 대역 재사용 가능 |

---

## Vertex AI 전환 검토 (실현 가능성)

현재 ns-aimage-dev는 **Google AI Studio API Key** (`generativelanguage.googleapis.com`) 방식으로 Gemini를 호출합니다.
ns-aicc-dev는 **Vertex AI + PSC** 방식으로 VPN을 통해 내부망에서 호출합니다.

### 비교

| 항목 | 현재 방식 (Google AI Studio) | Vertex AI + PSC (ns-aicc 방식) |
|------|------------------------------|-------------------------------|
| 인증 | API Key | Service Account |
| 네트워크 | Cloud Run → 인터넷 직접 Egress | VPN → PSC → Vertex AI |
| 보안 | Secret Manager로 API Key 관리 | IAM 기반, Key 불필요 |
| 비용 | 동일 (모델 단가 동일) | 동일 |
| 구성 복잡도 | 낮음 | 높음 (PSC, DNS, BGP 설정 필요) |
| 코드 변경 | 불필요 | SDK 및 엔드포인트 변경 필요 |

### 결론

**PoC 단계: 현재 방식 유지 권장**
- Cloud Run은 GCP 내부에서 실행되므로 인터넷 Egress가 Google 인프라 내에서 처리됨
- API Key는 Secret Manager로 안전하게 관리
- 추가 인프라 구성 없이 바로 동작

**운영 전환 시: Vertex AI 검토 권장**
- 조직 보안 정책상 API Key 사용 제한이 있을 경우
- ns-aicc-dev의 PSC 구성을 레퍼런스로 동일하게 적용 가능
- 코드 변경: `GeminiServiceImpl` 엔드포인트 및 인증 방식 수정

---

## 월 예상 비용 (50명 / 10회/일 기준)

| 서비스 | 스펙 | 월 비용 |
|--------|------|---------|
| Cloud Run | min=1, 2vCPU, 2GiB | ~$18 |
| Cloud SQL | db-standard-2 | ~$75 |
| Cloud Storage (FO) | 수 MB | ~$0 |
| Cloud Storage (이미지) | ~15GB/월 | ~$0.35 |
| Cloud Build Private Pool | CI/CD | ~$5 |
| Artifact Registry | ~5GB | ~$0.50 |
| Secret Manager | - | ~$0 |
| 인터넷 이그레스 | Gemini API | ~$27 |
| Gemini API (텍스트) | gemini-3-flash-preview | ~$5 |
| Gemini API (이미지) | NanaBanana | ~$230 |
| **합계** | | **~$361/월** |

> NanaBanana 이미지 생성 사용량에 따라 실제 비용은 크게 달라질 수 있음
