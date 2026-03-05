# 시스템 구성도

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              사용자 (Browser)                                   │
└──────────────────────────────┬──────────────────────────────────────────────────┘
                               │ HTTPS
               ┌───────────────┴───────────────┐
               │                               │
               ▼                               ▼
   ┌───────────────────────┐       ┌───────────────────────┐
   │   FO (Vue.js)         │       │   BO API (Spring Boot) │
   │  Cloud Storage +      │       │   Cloud Run            │
   │  Cloud CDN            │       │   (컨테이너)            │
   │  (정적 호스팅)          │  API  │                       │
   │                       │──────►│  /api/**               │
   └───────────────────────┘       └───────────┬───────────┘
                                               │
                          ┌────────────────────┼───────────────────┐
                          │                    │                   │
                          ▼                    ▼                   ▼
               ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
               │  Cloud SQL       │  │  Secret Manager  │  │  외부 API        │
               │  (PostgreSQL)    │  │  - GEMINI_API_KEY│  │  Gemini API      │
               │  Private IP      │  │  - JWT_SECRET    │  │  (Google AI)     │
               └──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## CI/CD 파이프라인

```
┌────────────────────────┐     push/webhook     ┌────────────────────────────┐
│  Private Gitea         │─────────────────────►│  Cloud Build               │
│  (Self-hosted)         │                      │  - bo : Docker build       │
│  - ns-creative-bo      │                      │  - fo : npm build → GCS    │
│  - ns-creative-fo      │                      └────────────┬───────────────┘
└────────────────────────┘                                   │
                                                             │ push image
                                                             ▼
                                                ┌────────────────────────────┐
                                                │  Artifact Registry         │
                                                │  (Docker Image)            │
                                                └────────────┬───────────────┘
                                                             │ deploy
                                                             ▼
                                                ┌────────────────────────────┐
                                                │  Cloud Run (BO API)        │
                                                │  자동 배포                  │
                                                └────────────────────────────┘
```

### 배포 흐름 요약

```
코드 push (Gitea)
  → Cloud Build webhook 트리거
    ├── [BO] Docker build → Artifact Registry push → Cloud Run 자동 배포
    └── [FO] npm run build → dist/ → Cloud Storage sync → Cache 무효화
```

---

## 네트워크 구성

```
┌───────────────────────────────────────────────────────────────────┐
│  VPC (커스텀)                                                      │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  Private Subnet                                             │  │
│  │  - Cloud Run (VPC Connector)                                │  │
│  │  - Cloud SQL (Private IP)                                   │  │
│  │  - Gitea VM (Compute Engine)                                │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Cloud NAT  →  외부 인터넷 (Gemini API 호출)                        │
└───────────────────────────────────────────────────────────────────┘
```

---

## GCP 서비스 목록

| 분류 | 서비스 | 용도 |
|------|--------|------|
| **컴퓨팅** | Cloud Run | BO API (Spring Boot) 컨테이너 실행 |
| **스토리지** | Cloud Storage | FO (Vue.js) 정적 파일 호스팅 |
| **CDN** | Cloud CDN | FO 전역 캐싱, 응답속도 개선 |
| **DB** | Cloud SQL (PostgreSQL) | 운영 DB (HA 구성 권장) |
| **보안** | Secret Manager | GEMINI_API_KEY, JWT_SECRET 등 환경변수 관리 |
| **이미지** | Artifact Registry | Docker 이미지 저장소 |
| **CI/CD** | Cloud Build | Gitea webhook → 빌드/배포 자동화 |
| **네트워크** | VPC + VPC Connector | Cloud Run ↔ Cloud SQL 내부 통신 |
| **네트워크** | Cloud NAT | Private subnet에서 외부 API 호출 |
| **DNS** | Cloud DNS | 도메인 연결 (`api.yourdomain.com`, `app.yourdomain.com`) |
| **모니터링** | Cloud Monitoring + Logging | 애플리케이션 로그, 알림 설정 |
| **Git** | Compute Engine (VM) | Gitea self-hosted |

---

## 핵심 설계 포인트

- **Cloud Run** : 컨테이너 기반, 트래픽 없을 때 비용 0 (PoC에 최적)
- **Cloud SQL Private IP** : 퍼블릭 노출 없이 VPC 내부에서만 접근
- **Secret Manager** : `.env` 파일 대신 GCP 관리형 시크릿으로 API Key 주입
- **Gitea VM** : Private Subnet에 두고 SSH/HTTPS만 허용, 외부 노출 최소화
- **Cloud Build + webhook** : Gitea push → 자동 빌드/배포 (GitHub Actions 대신)
