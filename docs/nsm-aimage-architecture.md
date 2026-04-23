# 시스템 구성도

> **프로젝트**: ns-aimage (ns-aimage-bo / ns-aimage-fo)
> **최초 작성일**: 2026-03-09
> **상태**: v3.0

---

## 전체 시스템 구성도

```
                          사용자 (사내 망 / VPN)
                                  │
              HTTPS 443            │              HTTPS 443
    ┌─────────────────────────────┘└───────────────────────────┐
    ▼  storage.googleapis.com/…                                ▼  aimage-api.nsmall.com
    │  (LB 없음, raw URL 서빙)                                  │
    │                                                          │
┌───┼──────────────────────────────────────────────────────────┼──────────────────────┐
│   │  GCP Public Cloud Zone (ns-aimage-dev)                   │                      │
│   ▼                                                          ▼                      │
│  ┌────────────────────────────┐      ┌──────────────────────────────────────────┐   │
│  │  CSG ①                    │      │  Cloud Run [ns-aimage-bo]                │   │
│  │  Cloud Storage             │      │  Spring Boot 3.x / OpenJDK 17            │   │
│  │  FO, Vue.js Build Artifacts│      │                                          │   │
│  │  (LB 없이 커스텀 도메인    │      │  ① → Secret Manager (ENV 런타임 주입)    │   │
│  │   불가, raw URL 사용)      │      │  ② → VPC Connector → Cloud SQL 사설통신  │   │
│  └────────────────────────────┘      │  ③ → Internet Egress → Gemini API        │   │
│            ▲                         │  ④ → Internet Egress → CSG② SDK Write   │   │
│            │ CI/CD 배포              │                                          │   │
│            │ (gsutil rsync)          │  ※ CORS: FO(storage URL) ↔ BO 도메인 상이│   │
│            │                         │     Spring WebMvcConfigurer 허용 설정    │   │
│            │                         └──────┬──────────┬──────────┬─────────────┘   │
│            │                      ①        │          │ ②       │ ③         ④     │
│            │                               ▼          ▼          ▼                  │
│            │                    ┌──────────────┐ ┌───────────────────┐              │
│            │                    │Secret Manager│ │  VPC Connector    │              │
│            │                    │GEMINI_API_KEY│ │  (사설 통신 전용)  │              │
│            │                    │JWT_SECRET    │ │         │         │              │
│            │                    └──────────────┘ │         ▼         │              │
│            │                                     │  ┌─────────────┐  │              │
│            │                                     │  │  Cloud SQL  │  │              │
│            │                                     │  │  PostgreSQL │  │              │
│            │                                     │  │  Private IP │  │              │
│            │                                     │  │  TCP 5432   │  │              │
│            │                                     │  └─────────────┘  │              │
│            │                                     └───────────────────┘              │
│            │                                                                        │
│            │           ③ Internet Egress ──────────────────────────────────────►   │
│            │               │  Cloud API / Gemini API (*.googleapis.com)             │
│            │               │  - gemini-3-flash-preview  (텍스트 프롬프트 생성)       │
│            │               │  - gemini-3-pro-preview    (텍스트 고품질)             │
│            │               │  - gemini-3-pro-image-preview  (NanaBanana 이미지)    │
│            │               │  응답(base64 이미지) ◄────────────────────────────     │
│            │               │                                                        │
│            │           ④ Internet Egress ──────────────────────────────────────►   │
│            │               │                                                        │
│            │               ▼                                                        │
│            │           ┌─────────────────────────────────────────────────────┐     │
│            │           │  CSG ②                                               │     │
│            │           │  Cloud Storage (NanaBanana 이미지 버킷)               │     │
│            │           │  /images/{uuid}.jpg|png                              │     │
│            │           │  ┌───────────────────────────────────────────────┐   │     │
│            │           │  │  Lifecycle Policy (GCS 내장, 코드/서버리스 불요)│   │     │
│            │           │  │  Age > 30d → Delete  (1달 보관)               │   │     │
│            │           │  │  Age > 90d → Delete  (3달 보관)               │   │     │
│            │           │  │  Age > 180d → Delete (6달 보관)               │   │     │
│            │           │  └───────────────────────────────────────────────┘   │     │
│            │           └─────────────────────────────────────────────────────┘     │
│            │                                                                        │
│  ┌─────────┴────────────────────────────────────────────────────────────────────┐   │
│  │  GCP CI/CD Zone                                                               │   │
│  │                                                                               │   │
│  │  Cloud Build Private Pool [ns-aimage-cloudbuild-pool-01]                     │   │
│  │  Worker IP: 172.31.56.0/23                                                    │   │
│  │  Peered VPC: ns-infr-host-402505/ns-infr-vpc-prod (Shared VPC)               │   │
│  │                                                                               │   │
│  │  [BO] git clone → ./gradlew clean build -x test → docker build               │   │
│  │        └─ docker push → Artifact Registry → gcloud run deploy → Cloud Run    │   │
│  │                                                                               │   │
│  │  [FO] git clone → npm run build                                               │   │
│  │        └─ gsutil rsync ─────────────────────────────────────────► CSG ① ▲   │   │
│  │                                                                               │   │
│  │  Artifact Registry [ns-aimage-docker]  asia-northeast3                        │   │
│  └───────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────┘
         ▲ ① Webhook                        │ ② git clone
         │ 인터넷 직접 Outbound              │ Cloud VPN 경유
         │ (VPN 불필요)                      │ ┌──────────────────────────────────┐
         │                                   │ │  방화벽 규칙 (인프라팀 선결)       │
         │                                   │ │  172.31.56.0/23 (Pool)           │
         │                                   │ │  → 172.21.9.0/24 (Gitea)         │
         │                                   │ │  TCP 443 / 3000 허용             │
         │                                   │ └──────────────────────────────────┘
┌────────┼───────────────────────────────────┼────────────────────────────────────────┐
│  NS IDC Private Cloud   172.21.9.0/24      │                                        │
│                         │                  │                                        │
│  ┌──────────────────┐   │                  │                                        │
│  │  Gitea           │───┘ ①(인터넷 Outbound)│                                        │
│  │  (내부망 전용)   │◄───────────────────────┘ ②(VPN Clone)                         │
│  │  ns-aimage-bo    │                                                               │
│  │  ns-aimage-fo    │                                                               │
│  └──────────────────┘                                                               │
│                                                                                     │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │  Harbor (Private Registry)  →  ArgoCD                                         │  │
│  │  ZCP 내부 Kubernetes 배포용 이미지 보관 / CD 관리                                │  │
│  │  ※ ns-aimage GCP Cloud Run 배포는 Cloud Build가 처리 (ArgoCD 미관여)           │  │
│  └───────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 온프레미스 → GCP 대응 구조

| 온프레미스 구성 | 스펙 | GCP 대응 서비스 |
|----------------|------|----------------|
| WEB (DMZ) — Nginx | Ubuntu, 2 vCPU / 40GB | Cloud Storage 직접 URL (PoC 기준 LB/CDN 미사용) |
| WAS (DMZ) — Spring Boot 3.x | Ubuntu, 4 vCPU / 100GB, OpenJDK 17 | Cloud Run (ns-aimage-bo) |
| DB Server — Oracle | 4 vCPU / 200GB | Cloud SQL (PostgreSQL) |
| Storage — NAS/File Server | prd | Cloud Storage 이미지 버킷 (Lifecycle Policy로 자동 삭제) |
| 외부 API — Google Gemini API | HTTPS 443 Outbound | Cloud Run 직접 Egress → *.googleapis.com |

> DB는 온프레미스 Oracle에서 **PostgreSQL (TCP 5432)** 로 전환하여 Cloud SQL에 구성

---

## 서비스 기능 구성

ns-aimage-bo는 두 가지 프롬프트 생성 기능을 제공한다.

### 기능 1: 상품 사진 프롬프트 생성

```
사용자
  → 카테고리 선택 (상품군: 대/중/소/세분류 4단계 드릴다운)
  → 태그 선택 (이미지 스타일: 그룹별 단일/복수 선택)
  → POST /api/prompts/goods
  → Gemini API (gemini-3-flash-preview / gemini-3-pro-preview) 호출
  → 영문 이미지 생성 프롬프트 텍스트 반환
  → PromptHistory DB 저장
  → 사용자가 프롬프트 복사 → 외부 이미지 AI에 직접 사용
```

### 기능 2: 인물 사진 프롬프트 생성 + 이미지 직접 생성 (NanaBanana)

```
사용자
  → 단계별 속성 선택 (5단계 Step → Attr → Option 구조, 동적 로드)
  → 추가 옵션 입력 (생성 목적, 추가 지시사항)
  → 스타일 선택 (NANO_BANANA / MID_JOURNEY)
  → POST /api/human-prompts
  → Gemini API (gemini-3-flash-preview) 호출
  → 영문 인물 이미지 프롬프트 텍스트 반환 + PromptHistory DB 저장

  [선택] 이미지 직접 생성 버튼 클릭
  → POST /api/human-prompts/generate-image
  → Gemini Image API (gemini-3-pro-image-preview) 호출  ← NanaBanana
  → base64 이미지 응답 수신
  → Cloud Storage (이미지 버킷) 저장 → /images/{uuid}.jpg|png
  → PromptHistory.image_url 업데이트
  → 이미지 URL 반환 → 브라우저에 이미지 표시
```

### NanaBanana 이미지 생성 상세 흐름

```
┌──────────┐  POST /api/human-prompts/generate-image   ┌──────────────────────┐
│  사용자   │ ──────────────────────────────────────────►│  Cloud Run            │
│  (FO)    │                                            │  (ns-aimage-bo)      │
└──────────┘                                            └──────────┬───────────┘
                                                                   │
                                             HTTPS (직접 Egress)    │
                                                                   ▼
                                                    ┌──────────────────────────┐
                                                    │  Gemini Image API        │
                                                    │  gemini-3-pro-image-     │
                                                    │  preview                 │
                                                    │  *.googleapis.com        │
                                                    └──────────┬───────────────┘
                                                               │ base64 이미지
                                                               ▼
                                                    ┌──────────────────────────┐
                                                    │  Cloud Storage (이미지버킷)│
                                                    │  /images/{uuid}.jpg|png  │
                                                    │  DB image_url 업데이트   │
                                                    │  Lifecycle Policy 적용   │
                                                    └──────────────────────────┘
```

---

## CI/CD 파이프라인

> **연결 방향 구분**
> - Gitea Webhook → Cloud Build : **인터넷 직접 Outbound** (VPN 불필요, Gitea → GCP Public Endpoint)
> - Cloud Build Private Pool → Gitea Clone : **Cloud VPN 경유** (Private Pool → 내부망 Gitea)

```
┌───────────────────────────────────────────────────────────────────────────┐
│  NS Private Cloud (IDC)    172.21.9.0/24                                  │
│                                                                           │
│  ┌─────────────────┐                                                      │
│  │  Gitea          │──► ① Webhook (인터넷 Outbound) ──► Cloud Build Trigger│
│  │  (내부망 전용)  │                                                       │
│  │  - ns-aimage-bo │◄── ② git clone (Cloud VPN 경유) ◄── Cloud Build Pool │
│  │  - ns-aimage-fo │                                                       │
│  └─────────────────┘                                                      │
└───────────────────────────────────────────────────────────────────────────┘
                        │ ② Cloud VPN (172.31.56.0/23 ↔ 172.21.9.0/24)
                        │ Firewall: 172.31.56.0/23 → 172.21.9.0/24 TCP 443/3000
                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│  GCP (ns-aimage-dev)                                                      │
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │  Cloud Build Private Pool                                          │   │
│  │  이름: ns-aimage-cloudbuild-pool-01                                │   │
│  │  리전: asia-northeast3                                              │   │
│  │  Worker IP: 172.31.56.0/23                                         │   │
│  │  Peered VPC: ns-infr-host-402505/ns-infr-vpc-prod (Shared VPC)    │   │
│  │                                                                    │   │
│  │  cloudbuild.yaml                                                   │   │
│  │  ┌──────────────────────────────────────────────────────────────┐  │   │
│  │  │  [BO] git clone (gitea-access-token)                         │  │   │
│  │  │       ./gradlew clean build -x test                          │  │   │
│  │  │       docker build                                           │  │   │
│  │  │       docker push → Artifact Registry                        │  │   │
│  │  │       gcloud run deploy → Cloud Run                          │  │   │
│  │  │                                                              │  │   │
│  │  │  [FO] git clone                                              │  │   │
│  │  │       npm run build                                          │  │   │
│  │  │       gsutil rsync → Cloud Storage FO 버킷                   │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                │ docker push              │ gsutil rsync                   │
│                ▼                         ▼                                │
│  ┌──────────────────────────┐  ┌──────────────────────────────────────┐   │
│  │  Artifact Registry       │  │  Cloud Storage (FO 정적 파일)        │   │
│  │  ns-aimage-docker        │  │  Vue.js Build Artifacts              │   │
│  │  asia-northeast3         │  │  dist/ → 직접 URL 서빙               │   │
│  └─────────────┬────────────┘  └──────────────────────────────────────┘   │
│                │ gcloud run deploy                                         │
│                ▼                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  Cloud Run (ns-aimage-bo)                                            │  │
│  │  min-instances=1 / OpenJDK 17 / Spring Boot 3.x                     │  │
│  │  이미지: asia-northeast3-docker.pkg.dev/ns-aimage-dev/               │  │
│  │          ns-aimage-docker/ns-aimage-bo                               │  │
│  │  Secret Manager → GEMINI_API_KEY, JWT_SECRET 주입                    │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

### 배포 흐름 요약

```
코드 push (Private Gitea)
  → Cloud Build Trigger (Webhook)
    ├── [BO] git clone → gradle build → docker build
    │         └─ Artifact Registry push → gcloud run deploy → Cloud Run
    └── [FO] git clone → npm run build
              └─ gsutil rsync → Cloud Storage FO 버킷
```

### Cloud Build 서비스 계정 권한

| 권한 | 용도 |
|------|------|
| Artifact Registry Writer | Docker 이미지 push |
| Cloud Run Developer | Cloud Run 배포 |
| Secret Manager Secret Accessor | gitea-access-token 조회 |
| Storage Object Admin | FO 버킷 파일 업로드 |

### CI/CD 도구 역할

| 도구 | 위치 | 역할 |
|------|------|------|
| **Gitea** | NS Private Cloud | 소스코드 저장소 (내부망 전용) |
| **Cloud Build** | GCP | CI/CD — 빌드, 이미지 생성, 배포 자동화 |
| **Artifact Registry** | GCP | Container Registry — Cloud Run 배포용 |
| **Harbor** | NS Private Cloud | Private Registry — ZCP 내부 Kubernetes 배포용 |
| **ArgoCD** | NS Private Cloud | ZCP Kubernetes 워크로드 CD (GCP 배포 미관여) |

---

## 이미지 보관 및 자동 삭제

> **Cloud Function 미사용 (서버리스 제외)**
> GCS Object Lifecycle Policy — GCS 버킷에 내장된 기능, 별도 코드/인프라 불필요

### 삭제 방법 비교

| 방법 | 서버리스 | 추가 비용 | 코드 | 채택 |
|------|---------|---------|------|------|
| **GCS Lifecycle Policy** | ❌ GCS 내장 | **$0** | **없음** | **★ 채택** |
| Cloud Scheduler + Cloud Run BO endpoint | ⚠️ managed | ~$0.10/월 | BO endpoint 추가 | 대안 |
| Compute Engine VM + cron | ❌ | ~$5~10/월 | 있음 | 비권장 |
| Cloud Function | ✅ 서버리스 | ~$0 | 있음 | **제외** |

### GCS Lifecycle Policy 동작 흐름

```
Cloud Storage 이미지 버킷
  │
  │  [GCS 내부 — 매일 자동 실행, 외부 트리거 없음]
  │
  ├─ 오브젝트 생성일(Age) 체크
  │
  ├─ Age > 설정일 (30 / 90 / 180)
  │      │
  │      └─ → 자동 Delete  ✓
  │
  └─ Age <= 설정일
         └─ 보관 유지
```

> 설정 위치: GCP Console → Cloud Storage → 버킷 선택 → **Lifecycle 탭** → Add Rule
> 별도 코드, VM, 스케줄러, 서버리스 함수 일절 불필요

### 보관 기간별 비용 (Output 이미지 500MB/일 기준)

| 보관 기간 | Lifecycle Policy Rule | 누적 최대 용량 | 스토리지 비용 | 삭제 비용 |
|---------|----------------------|-------------|------------|---------|
| **1달** | `Age > 30` → Delete | ~15GB | ~$0.35/월 | **$0** |
| **3달** | `Age > 90` → Delete | ~45GB | ~$1.04/월 | **$0** |
| **6달** | `Age > 180` → Delete | ~90GB | ~$2.07/월 | **$0** |

---

## Gemini API Key 보안 흐름

```
Secret Manager (GCP)
  └─ Cloud Run 컨테이너 기동 시 환경변수로 주입
       └─ CI/CD (Cloud Build) 에는 Key 전혀 노출되지 않음
       └─ 텍스트 프롬프트 생성 (gemini-3-flash-preview, gemini-3-pro-preview)
       └─ 이미지 생성 (gemini-3-pro-image-preview) — 동일 Key, 모델명으로 구분
```

---

## 네트워크 구성

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  GCP VPC (ns-aimage-dev)                                                     │
│  Shared VPC Host: ns-infr-host-402505/ns-infr-vpc-prod                      │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  Private (VPC 내부)                                                   │    │
│  │                                                                      │    │
│  │  Cloud SQL (Private IP, TCP 5432)                                    │    │
│  │  Cloud Build Private Pool (172.31.56.0/23)                          │    │
│  │                                                                      │    │
│  │  VPC Connector ── Cloud Run (VPC 내부 통신 전용, Cloud SQL 접근용)    │    │
│  │  ※ Cloud Run의 인터넷 Egress는 VPC Connector 거치지 않음             │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  Cloud Run 인터넷 직접 Egress (Cloud NAT 미사용)                       │    │
│  │  - Gemini API  *.googleapis.com  HTTPS 443                           │    │
│  │  - Cloud Storage  storage.googleapis.com  HTTPS 443                  │    │
│  │  ※ Cloud Run은 VPC Connector 없이도 기본 인터넷 Egress 가능           │    │
│  │     → all-traffic VPC 라우팅 비활성화 시 Cloud NAT 불필요             │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  Public 접근 (HTTPS 443, 사내 망 / VPN 전용)                          │    │
│  │  - Cloud Storage URL  (FO Vue.js 정적 파일, LB 없이 커스텀 도메인 불가)│    │
│  │  - Cloud Run  aimage-api.nsmall.com  (BO API)                        │    │
│  │                                                                      │    │
│  │  CORS: FO(storage URL) → BO(aimage-api.nsmall.com) 크로스 도메인    │    │
│  │        Spring Boot WebMvcConfigurer CORS 허용 설정 필요              │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                              │ Cloud VPN
┌─────────────────────────────┼────────────────────────────────────────────────┐
│  NS Private Cloud (IDC)     │  172.21.9.0/24                                 │
│                             │                                                │
│  Gitea (내부망 전용)                                                           │
│  ZCP CI/CD (Harbor, ArgoCD)                                                  │
│                                                                              │
│  방화벽 규칙 (인프라팀):                                                       │
│  172.31.56.0/23 → 172.21.9.0/24  TCP 443/3000  (Cloud Build Pool → Gitea)  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 환경 구성 방침

| 항목 | 결정 내용 |
|------|-----------|
| 프로젝트 수 | 단일 GCP 프로젝트 (ns-aimage-dev) |
| 개발/운영 분리 | 분리 없음 — 개발 환경을 현업에 바로 오픈 |
| DB | Cloud SQL db-standard-2 (PoC), 운영 확장 시 재산정 |
| 배포 | Cloud Build → Artifact Registry → Cloud Run 자동 반영 |
| 보안 | 사내 망 / VPN 전용 — LB/Cloud Armor 미사용 |
| 이미지 삭제 | GCS Lifecycle Policy (서버리스 미사용) |

---

## 도메인 구성

| 도메인 / URL | 연결 대상 | 설명 |
|------------|-----------|------|
| `storage.googleapis.com/버킷명/index.html` | Cloud Storage 직접 URL | FO (Vue.js) 서빙 — **PoC 기준 raw URL 사용** |
| `aimage-api.nsmall.com` | Cloud Run | BO (Spring Boot) API 서버 |

> **FO 커스텀 도메인 제약**: Cloud Storage는 HTTPS 커스텀 도메인(`aimage-dev.nsmall.com`)을 LB 없이 지원하지 않음
> - PoC: `storage.googleapis.com/…` raw URL 직접 사용
> - 운영 전환 시: Cloud Load Balancer + SSL 인증서 추가 → 커스텀 도메인 연결 가능

---

## Storage 구성

> 두 가지 Cloud Storage 버킷으로 분리 구성

| 버킷 | 용도 | 접근 방식 |
|------|------|----------|
| FO 정적 버킷 | ns-aimage-fo Vue.js Build Artifacts (dist/) | `storage.googleapis.com/버킷명/…` raw URL (PoC) |
| 이미지 버킷 | NanaBanana 이미지 생성 결과 저장 | Cloud Run → GCS SDK 직접 Write / 브라우저 → Public URL 또는 Signed URL |

> FO 버킷: HTTPS 커스텀 도메인은 LB 없이 불가 → PoC는 raw URL 사용
> 이미지 버킷: GCS Lifecycle Policy 적용 → N일 초과 오브젝트 자동 삭제 (Cloud Function 불필요)
> Cloud Run → 두 버킷 모두 VPC Connector 거치지 않고 **직접 인터넷 Egress로 접근**

| 옵션 | 비용 | 결론 |
|------|------|------|
| Cloud Storage + Lifecycle Policy | 저 ($0.023/GB·월, 자동 삭제 무료) | **★ 채택** |
| Cloud Storage + Cloud Function 배치 | 중 (서버리스 → 제외) | **미채택 (서버리스)** |
| GCP Filestore (관리형 NAS) | 고 (최소 1TB 단위, $200+/월) | 비권장 |

---

## DB 구성

| | PostgreSQL (Cloud SQL) |
|---|---|
| 확정 여부 | **확정** |
| 포트 | TCP 5432 |
| 접근 | Private IP (퍼블릭 노출 없음) |
| PoC 스펙 | db-standard-2 (2 vCPU, 7.5GB) |

### 주요 테이블

| 테이블 | 설명 |
|--------|------|
| `user` | 회원 정보, 권한(ADMIN/MANAGER/USER), 팀명, 일일 한도 |
| `category` | 상품 카테고리 계층 (LCS/MCS/SCS/DCS, self-reference) |
| `tag_group` | 태그 그룹 (인물, 분위기 등, 단일/복수 선택 여부) |
| `tag` | 태그 값 (남자, 화사한 등) |
| `prompt_history` | 생성된 프롬프트 저장 (상품/인물 공통, image_url 포함) |
| `prompt_history_tag` | 프롬프트-태그 매핑 |
| `human_prompt_step` | 인물 프롬프트 단계 정의 (5단계 구조) |
| `human_prompt_attr` | 단계별 세부 속성 (multiSelect, required 여부) |
| `human_prompt_option` | 속성별 선택지 값 |

---

## 비용 산정 (PoC 기준 월 예상, 50명 / 10회/일)

| 서비스 | 스펙 | 월 예상 비용 |
|--------|------|-------------|
| Cloud Run (ns-aimage-bo) | min=1, 2vCPU, 2GiB | ~$18 |
| Cloud SQL (PostgreSQL) | db-standard-2 | ~$75 |
| Cloud Storage (FO 정적) | 수 MB | ~$0 |
| Cloud Storage (이미지, Output 500MB/일) | ~15GB/월 (1달 보관) | ~$0.35 |
| Cloud Storage Lifecycle Policy | 자동 삭제 | **$0** |
| Cloud Build Private Pool | CI/CD | ~$5 |
| Artifact Registry | Docker 이미지 ~5GB | ~$0.50 |
| Secret Manager | gitea-access-token, API Key, JWT | ~$0 |
| 인터넷 이그레스 | Cloud Run → Gemini API (직접 Egress) | ~$27 |
| Gemini API (텍스트, Flash) | gemini-3-flash-preview | ~$5 |
| Gemini API (이미지, NanaBanana) | gemini-3-pro-image-preview | ~$230 |
| **합계** | | **~$361 / 월** |

> Cloud Function 미사용 → GCS Lifecycle Policy로 대체, 추가 비용 없음
> LB/Cloud Armor/CDN 미사용 (사내 망 전용)
> Gemini 3 Pro Image 모델 단가는 출시 후 GCP 콘솔에서 재확인 필요

---

## GCP 서비스 목록

| 분류 | 서비스 | 용도 |
|------|--------|------|
| **컴퓨팅** | Cloud Run | ns-aimage-bo (Spring Boot 3.x, OpenJDK 17) |
| **CI/CD** | Cloud Build (Private Pool) | Gitea → Artifact Registry → Cloud Run 자동 빌드/배포 |
| **이미지 저장소** | Artifact Registry | ns-aimage-bo Docker 이미지 (ns-aimage-docker) |
| **스토리지** | Cloud Storage (FO 버킷) | ns-aimage-fo Vue.js 정적 파일 직접 URL 서빙 |
| **스토리지** | Cloud Storage (이미지 버킷) | NanaBanana 생성 이미지, Lifecycle Policy 자동 삭제 |
| **DB** | Cloud SQL (PostgreSQL) | 운영 DB, Private IP, TCP 5432 |
| **보안** | Secret Manager | GEMINI_API_KEY, JWT_SECRET, gitea-access-token |
| **네트워크** | VPC + VPC Connector | Cloud Run ↔ Cloud SQL 내부 통신 |
| **네트워크** | Cloud Run 직접 Egress | Gemini API / Cloud Storage — Cloud NAT 미사용 |
| **DNS** | Cloud DNS | aimage-dev.nsmall.com, aimage-api.nsmall.com |
| **모니터링** | Cloud Monitoring + Logging | 로그, Gemini API 사용량 모니터링 |

---

## 핵심 설계 포인트

- **Cloud Build Private Pool** : Gitea Webhook은 인터넷 직접 Outbound(VPN 불필요), Clone은 Cloud VPN 경유(Private Pool → 내부망 Gitea). Worker IP(172.31.56.0/23) → Gitea(172.21.9.0/24) 방화벽 규칙 필요
- **VPC Connector** : Cloud Run ↔ Cloud SQL 사설 통신 전용. 인터넷 Egress(Gemini API, Cloud Storage)는 VPC Connector 경유 안 함 → Cloud NAT 불필요
- **Cloud Run (WAS 대체)** : min-instances=1 (Cold Start 방지), OpenJDK 17 + Spring Boot 3.x 유지
- **LB/Armor 미사용** : 사내 망 / VPN 전용 서비스 → PoC 기준 불필요. FO는 Cloud Storage raw URL 서빙 (커스텀 도메인 필요 시 LB 추가)
- **CORS** : FO(storage.googleapis.com)와 BO(aimage-api.nsmall.com) 도메인 상이 → Spring Boot WebMvcConfigurer CORS 허용 설정 필요
- **Cloud SQL PostgreSQL (DB 대체)** : Oracle → PostgreSQL 전환, Private IP로 외부 접근 차단
- **GCS Lifecycle Policy (NAS + 배치 대체)** : IDC NAS 대신 GCS 이미지 버킷 사용. 자동 삭제 정책으로 Cloud Function 없이 보관 기간 관리
- **Gemini API 이중 사용** : Flash(텍스트 프롬프트 생성) + Pro Image(NanaBanana 이미지 생성) — 동일 API Key, 모델명으로 구분
- **Gemini API Key 격리** : Secret Manager에만 존재, Cloud Run 런타임 시에만 주입 (Cloud Build/CI 노출 없음)

---

## API 엔드포인트 요약

| 도메인 | 엔드포인트 | 메서드 | 설명 |
|--------|-----------|--------|------|
| **Auth** | `/api/auth/login` | POST | 로그인 → JWT Access Token 발급 |
| **User** | `/api/users` | GET / POST | 사용자 목록 조회 / 생성 (ADMIN) |
| **User** | `/api/users/{id}` | PUT / DELETE | 사용자 수정 / 삭제 (ADMIN) |
| **Category** | `/api/categories` | GET | 카테고리 목록 (parentId로 단계별 조회) |
| **Category** | `/api/categories` | POST | 카테고리 생성 (MANAGER+) |
| **Category** | `/api/categories/{id}` | PUT / DELETE | 카테고리 수정 / 삭제 (MANAGER+) |
| **TagGroup** | `/api/tag-groups` | GET / POST / PUT / DELETE | 태그 그룹 CRUD (MANAGER+) |
| **Tag** | `/api/tags` | GET / POST / PUT / DELETE | 태그 CRUD (MANAGER+) |
| **Prompt (상품)** | `/api/prompts/goods` | POST | 상품 사진 프롬프트 생성 (Gemini 텍스트) |
| **Prompt** | `/api/prompts` | GET | 프롬프트 이력 조회 (USER: 같은 팀, MANAGER+: 전체) |
| **HumanPrompt** | `/api/human-prompts/steps` | GET | 인물 프롬프트 단계/속성/선택지 구조 조회 |
| **HumanPrompt** | `/api/human-prompts` | POST | 인물 사진 프롬프트 생성 (Gemini 텍스트) |
| **HumanPrompt** | `/api/human-prompts/generate-image` | POST | 이미지 직접 생성 (Gemini 이미지, NanaBanana) |

> 공통 응답: `{ "code": 200, "message": "OK", "data": { ... } }`
> 성공 시 HTTP 상태코드는 무조건 200

---

## 리스크 및 선결 과제

| 항목 | 현황 | 담당 |
|------|------|------|
| 방화벽 규칙 추가 | `172.31.56.0/23` → `172.21.9.0/24` TCP 443/3000 (Cloud Build Pool → Gitea) | 인프라팀 |
| Shared VPC 권한 | ns-infr-host-402505 프로젝트 권한 필요 (Private Pool Peering) | 인프라팀 |
| VPC 라우팅 확인 | Cloud Build Pool ↔ Gitea 라우팅 설정 검증 | 네트워크팀 |
| Cloud SQL 스펙 | PoC: db-standard-2 / 운영 확장 시 재산정 | 개발팀 |
| Gemini API 월 한도 | 미정 — 팀/전체 단위 총량 정책 검토 필요 | 개발팀 |
| NanaBanana 이미지 URL 접근 방식 | 미정 — Public URL vs Signed URL (보안 정책에 따라 결정) | 개발팀 |
| **[코드 수정 필요] NanaBanana 이미지 저장** | 현재 로컬 파일시스템 저장 중 → Cloud Run 재시작 시 소멸 → GCS SDK로 변경 필요 | 개발팀 |
| GCP vs Private Cloud 대안 | GCP 구성 시 네트워크 팀 협조 사항 多 → Private Cloud + NAS 100GB 구성도 검토 중 | 전체 |
