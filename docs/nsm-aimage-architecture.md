# 시스템 구성도

## 전체 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  사용자 (Browser)                                                                        │
└──────────────────────────────┬───────────────────────────┬──────────────────────────────┘
                               │ HTTPS 443                 │ HTTPS 443
                               ▼                           ▼
                  nsm-aimage-fo.nsmall.com    nsm-aimage-bo.nsmall.com
                               │                           │
┌──────────────────────────────┼───────────────────────────┼──────────────────────────────┐
│  GCP                         │                           │                              │
│                              ▼                           ▼                              │
│         ┌────────────────────────────┐   ┌──────────────────────────────────┐          │
│         │  Cloud Load Balancer       │   │  Cloud Run                       │          │
│         │   └─ Cloud CDN             │   │  [WAS] ns-aimage-bo              │          │
│         │        └─ Cloud Storage    │   │  OpenJDK 17 / Spring Boot 3.x    │          │
│         │  [WEB] ns-aimage-fo        │   │  /api/**                         │          │
│         └────────────────────────────┘   └───────────┬──────────────────────┘          │
│                                                      │                                 │
│                              ┌───────────────────────┤                                 │
│                              │                       │ HTTPS 443 (Outbound Only)       │
│                              ▼                       ▼                                 │
│         ┌────────────────────────┐    ┌──────────────────────────┐                    │
│         │  Cloud SQL             │    │  Google Gemini API        │                    │
│         │  [DB] PostgreSQL       │    │  *.googleapis.com         │                    │
│         │  Private IP / TCP 5432 │    │  (Cloud NAT 경유)         │                    │
│         └────────────────────────┘    └──────────────────────────┘                    │
│                                                                                        │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │  Artifact Registry   │  │  Cloud Storage       │  │  Secret Manager              │ │
│  │  (BO Docker Image)   │  │  [Storage] 파일 버킷  │  │  - GEMINI_API_KEY            │ │
│  │           ▲          │  │  일 ~8GB      ▲      │  │  - JWT_SECRET                │ │
│  └───────────┼──────────┘  └───────────────┼──────┘  └──────────────────────────────┘ │
│              │ docker push   gsutil rsync   │                                          │
│              │ (HTTPS Outbound)             │                                          │
└──────────────┼──────────────────────────────┼──────────────────────────────────────────┘
               │                              │
┌──────────────┼──────────────────────────────┼──────────────────────────────────────────┐
│  NS Private Cloud (IDC)      │              │                                          │
│                              │              │                                          │
│  ┌───────────────┐  push  ┌──┴─────────────────────────────────────────────────────┐  │
│  │  Gitea        │───────►│  Tekton (CI Pipeline)                                  │  │
│  │  (내부망 전용) │        │                                                         │  │
│  │  ns-aimage-bo │        │  [BO] Docker build ──► Harbor (Private)                │  │
│  │  ns-aimage-fo │        │                   └──► GCP Artifact Registry ──────────┼──┘
│  └───────────────┘        │                                                         │
│                           │  [FO] npm run build ──► GCP Cloud Storage ─────────────┼──┘
│                           └─────────────────────────────────────────────────────────┘  │
│                                                                                        │
│  ┌────────────────────────┐    ┌───────────────────────────────────────────────────┐   │
│  │  Harbor                │    │  ArgoCD                                           │   │
│  │  (Private Registry)    │───►│  Harbor 이미지 기준으로 ZCP Kubernetes 배포 관리   │   │
│  └────────────────────────┘    └───────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 온프레미스 → GCP 대응 구조

| 온프레미스 구성 | 스펙 | GCP 대응 서비스 |
|----------------|------|----------------|
| WEB (DMZ) — Nginx | Ubuntu, 2 vCPU / 40GB | Cloud Load Balancer + Cloud CDN + Cloud Storage |
| WAS (DMZ) — Spring Boot 3.x | Ubuntu, 4 vCPU / 100GB, OpenJDK 17 | Cloud Run (ns-aimage-bo) |
| DB Server (대외계) — Oracle | 4 vCPU / 200GB | Cloud SQL (PostgreSQL) |
| Storage — NAS/File Server | prd | Cloud Storage 파일 버킷 (일 ~8GB) |
| 외부 API — Google Gemini API | HTTPS 443 Outbound | Cloud NAT → *.googleapis.com |

> DB는 온프레미스 Oracle에서 **PostgreSQL (TCP 5432)** 로 전환하여 Cloud SQL에 구성

---

## CI/CD 파이프라인

> Gitea는 내부망 전용, 외부 노출 없음
> Tekton(CI) + Harbor(Registry) + ArgoCD(CD) 모두 Private Cloud 내에서 운영
> GCP 배포는 Tekton 파이프라인에서 HTTPS 아웃바운드로 직접 Push — Interconnect/VPN 불필요

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  NS Private Cloud (IDC)                                                      │
│                                                                              │
│  ┌─────────────────┐  push   ┌─────────────────────────────────────────┐    │
│  │  Gitea          │────────►│  Tekton (CI Pipeline)                   │    │
│  │  (내부망 전용)   │         │                                         │    │
│  │  - ns-aimage-bo │         │  [BO] Docker build                      │    │
│  │  - ns-aimage-fo │         │   ├─ push → Harbor (Private Registry)   │    │
│  └─────────────────┘         │   └─ push → GCP Artifact Registry ─────┼───►│
│                              │                                         │    │
│                              │  [FO] npm run build                     │    │
│                              │   └─ gsutil → GCP Cloud Storage ────────┼───►│
│                              └─────────────────────────────────────────┘    │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  ArgoCD (CD)                                                         │    │
│  │  Harbor 이미지 기준으로 ZCP 내부 Kubernetes 워크로드 관리              │    │
│  │  (GCP Cloud Run 배포는 Tekton에서 직접 처리)                          │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────────┘
                                      │ HTTPS Outbound
                    ┌─────────────────┴──────────────────┐
                    ▼                                    ▼
        ┌───────────────────────┐          ┌─────────────────────────────┐
        │  GCP Artifact Registry│          │  Cloud Storage              │
        │  (BO Docker Image)    │          │  (FO dist/ 업로드)           │
        └───────────┬───────────┘          │  + CDN Cache 무효화          │
                    │ gcloud run deploy    └─────────────────────────────┘
                    ▼
        ┌───────────────────────────────────────────┐
        │  Cloud Run (ns-aimage-bo)                 │
        │  └─ Secret Manager → GEMINI_API_KEY 주입   │
        │  └─ Gemini API 호출 (런타임 시점)           │
        └───────────────────────────────────────────┘
```

### 각 도구의 역할

| 도구 | 위치 | 역할 |
|------|------|------|
| **Gitea** | Private Cloud | 소스코드 저장소 (내부망 전용) |
| **Tekton** | Private Cloud | CI — 빌드, 이미지 생성, Push |
| **Harbor** | Private Cloud | Private Container Registry — ZCP 내부 배포용 |
| **ArgoCD** | Private Cloud | CD — Harbor 이미지 기준으로 ZCP Kubernetes 배포 관리 |
| **Artifact Registry** | GCP | GCP 전용 Container Registry — Cloud Run 배포용 |

> ArgoCD는 ZCP 내부 Kubernetes 워크로드를 담당하고, GCP Cloud Run 배포는 Tekton 파이프라인에서 직접 처리

### 배포 흐름 요약

```
코드 push (Private Gitea)
  → Tekton CI 트리거
    ├── [BO] Docker build
    │     ├─ Harbor push          (ZCP 내부 배포 — ArgoCD가 관리)
    │     └─ Artifact Registry push → gcloud run deploy → Cloud Run
    └── [FO] npm run build
              └─ gsutil rsync → Cloud Storage → CDN Cache 무효화
```

### Tekton → GCP 연결 요건

| 항목 | 내용 |
|------|------|
| 네트워크 | HTTPS 아웃바운드만 필요 (Interconnect / VPN 불필요) |
| 인증 | GCP Service Account Key (Artifact Registry push + Cloud Storage + Cloud Run deploy 권한) |
| Gemini API Key | Tekton/CI 에는 노출 없음 — Cloud Run 런타임 시 Secret Manager에서만 주입 |

---

## Gemini API Key 보안 흐름

```
Secret Manager (GCP)
  └─ Cloud Run 컨테이너 기동 시 환경변수로 주입
       └─ ZCP / CI 파이프라인에는 Key 전혀 노출되지 않음
```

---

## 네트워크 구성

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  GCP VPC (커스텀)                                                             │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  Private Subnet                                                        │  │
│  │  - Cloud Run     (VPC Connector로 VPC 내부 통신)                        │  │
│  │  - Cloud SQL     (Private IP, TCP 5432, 퍼블릭 노출 없음)                │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐  │
│  │  Public 접근 (HTTPS 443)                                                │  │
│  │  - Cloud Load Balancer → Cloud CDN → Cloud Storage (nsm-aimage-fo)     │  │
│  │  - Cloud Run (nsm-aimage-bo)                                           │  │
│  └────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  Cloud NAT  →  외부 인터넷 (Cloud Run → Gemini API, HTTPS 443 Outbound)      │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│  NS Private Cloud (IDC)                                                      │
│  - Gitea (내부망 전용)                                                        │
│  - ZCP CI/CD → GCP HTTPS 아웃바운드 (Artifact Registry, Cloud Storage)       │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 환경 구성 방침

| 항목 | 결정 내용 |
|------|-----------|
| 프로젝트 수 | 단일 GCP 프로젝트 |
| 개발/운영 분리 | 분리 없음 — 개발 환경을 현업에 바로 오픈 |
| DB | DB만 별도 구성 고려 |
| 배포 | ZCP push → GCP 자동 반영 |

---

## 도메인 구성

| 도메인 | 연결 대상 | 설명 |
|--------|-----------|------|
| `nsm-aimage-fo.nsmall.com` | Cloud Load Balancer → Cloud CDN → Cloud Storage | FO (Vue.js) 정적 파일 서빙 |
| `nsm-aimage-bo.nsmall.com` | Cloud Run | BO (Spring Boot) API 서버 |

---

## Storage 구성

> 일 약 **8GB** 규모 파일 저장, IDC NAS 연결 없이 GCP 내 자체 구성

| 옵션 | 비용 | 결론 |
|------|------|------|
| Cloud Storage (오브젝트) | 저 ($0.02/GB·월) | **★ 권장** |
| GCP Filestore (관리형 NAS) | 고 (최소 1TB 단위, $200+/월) | 비권장 |
| VM + NAS 연결 | 중 (VM 운영 부담) | 비권장 |

---

## DB 구성

| | PostgreSQL (Cloud SQL) | MySQL (Cloud SQL) |
|---|---|---|
| 현재 코드 기준 | **현재 설정과 동일** | 변경 필요 |
| 포트 | TCP 5432 | TCP 3306 |
| 권장 | **★** | |

> PoC 스펙: 1~2 vCPU / 10~20GB / 운영 확장 시 재산정

---

## 비용 산정 (PoC 기준 월 예상)

| 서비스 | 스펙 | 월 예상 비용 |
|--------|------|-------------|
| Cloud Run | 요청 기반 (트래픽 없을 때 $0) | $0 ~ $10 |
| Cloud SQL (PostgreSQL) | 1~2 vCPU / 10~20GB SSD | $25 ~ $50 |
| Cloud Storage (FO 정적) | 수 MB | ~$0 |
| Cloud Storage (파일, 8GB/일) | ~240GB/월 | ~$5 |
| Cloud Load Balancer | 소규모 트래픽 | ~$5 |
| Cloud CDN | 소규모 트래픽 | ~$1 |
| Artifact Registry | 이미지 저장 | ~$1 |
| Secret Manager | 소규모 | ~$0 |
| **합계 (PoC)** | | **약 $37 ~ $72 / 월** |

> Interconnect 미사용으로 네트워크 인프라 비용 없음

---

## GCP 서비스 목록

| 분류 | 서비스 | 용도 |
|------|--------|------|
| **컴퓨팅** | Cloud Run | ns-aimage-bo (Spring Boot 3.x, OpenJDK 17) |
| **스토리지** | Cloud Storage (정적) | ns-aimage-fo Vue.js 정적 파일 호스팅 |
| **스토리지** | Cloud Storage (파일) | 이미지/파일 저장, 일 ~8GB |
| **CDN** | Cloud CDN | FO 전역 캐싱 (`nsm-aimage-fo.nsmall.com`) |
| **LB** | Cloud Load Balancer | FO HTTPS 진입점, SSL 인증서 처리 |
| **DB** | Cloud SQL (PostgreSQL) | 운영 DB, Private IP, TCP 5432 |
| **보안** | Secret Manager | GEMINI_API_KEY, JWT_SECRET 관리 |
| **이미지** | Artifact Registry | ns-aimage-bo Docker 이미지 저장소 |
| **네트워크** | VPC + VPC Connector | Cloud Run ↔ Cloud SQL 내부 통신 |
| **네트워크** | Cloud NAT | Cloud Run → Gemini API Outbound |
| **DNS** | Cloud DNS | `nsm-aimage-fo.nsmall.com`, `nsm-aimage-bo.nsmall.com` |
| **모니터링** | Cloud Monitoring + Logging | 로그, Gemini API 사용량 모니터링 |

---

## 핵심 설계 포인트

- **ZCP 빌드 후 GCP Push** : Gitea 외부 노출 없이 ZCP가 빌드 후 GCP에 HTTPS로 Push, Interconnect/VPN 불필요
- **Cloud Run (WAS 대체)** : 트래픽 없을 때 비용 $0, OpenJDK 17 + Spring Boot 3.x 유지
- **Cloud Load Balancer** : FO의 HTTPS 진입점, SSL 처리 및 Cloud CDN 연동 (CDN은 LB 없이 동작 불가)
- **Cloud SQL PostgreSQL (DB 대체)** : Oracle → PostgreSQL 전환, Private IP로 외부 접근 차단
- **Cloud Storage (NAS 대체)** : IDC NAS 연결 없이 GCP 오브젝트 스토리지로 자체 구성
- **Gemini API Key 격리** : Secret Manager에만 존재, Cloud Run 런타임 시에만 주입 (ZCP/CI 노출 없음)
- **단일 환경 운영** : 개발/운영 미분리, 단일 GCP 프로젝트로 관리

---

## 미결 사항

| 항목 | 현황 | 비고 |
|------|------|------|
| DB 종류 | PostgreSQL 유력 | MySQL도 검토 중 |
| Cloud SQL 스펙 | PoC: 1~2 vCPU / 10~20GB | 운영 확장 시 재산정 |
| Gemini API 월 한도 | 미정 | 팀/전체 단위 총량 정책 검토 |
| 파일 보관 기간 | 미정 | 8GB/일 기준 기간에 따라 비용 변동 |
| ZCP CI/CD 도구 | Tekton + Harbor + ArgoCD 확정 | GCP 배포는 Tekton에서 직접 처리 |
