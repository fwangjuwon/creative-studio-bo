# AI API 호출 실패 에러 처리 개선 계획

## 문제 요약

Gemini AI API 호출 자체가 실패할 때(HTTP 4xx/5xx, 네트워크 에러, 타임아웃)
전용 에러 처리가 없어 클라이언트에 항상 제네릭 500 메시지("서버 내부 오류가 발생했습니다")만 반환된다.

`IMAGE_GENERATION_FAILED (IG003)` 에러 코드가 정의되어 있으나 코드 전체에서 단 한 번도 throw되지 않는다.

---

## Gemini API 에러 코드 참고

> 출처: https://ai.google.dev/gemini-api/docs/troubleshooting?hl=ko#error-codes

| HTTP | Gemini 에러 코드 | 설명 | 대응 |
|------|-----------------|------|------|
| 400 | INVALID_ARGUMENT | 요청 본문 형식 오류 | 요청 파라미터 확인 |
| 400 | FAILED_PRECONDITION | 무료 등급 미지원 국가 | 유료 요금제 확인 |
| 403 | PERMISSION_DENIED | API 키 권한 없음 | API 키 확인 |
| 404 | NOT_FOUND | 리소스(모델명 등) 없음 | 모델명·파라미터 확인 |
| 429 | RESOURCE_EXHAUSTED | 비율 제한(Rate Limit) 초과 | 재시도 또는 할당량 상향 |
| 500 | INTERNAL | Google 내부 오류 | 재시도 |
| 503 | UNAVAILABLE | 서비스 과부하/다운 | 재시도 |
| 504 | DEADLINE_EXCEEDED | 처리 시간 초과 | 타임아웃 값 증가 후 재시도 |

---

## 영향 범위

| 파일 | 메서드 | 문제 |
|------|--------|------|
| `NanoBananaServiceImpl.java` | `callAndSave()` | RestClient 호출에 try-catch 없음, IG003 미사용 |
| `GeminiServiceImpl.java` | `generateHumanPrompt()` | RestClient 호출에 try-catch 없음 |
| `GeminiServiceImpl.java` | `generateGoodsPrompt()` | RestClient 호출에 try-catch 없음 |
| `PromptErrorCode.java` | — | AI 호출 실패 전용 에러 코드 없음 |

---

## 수정 계획

### Step 1. `PromptErrorCode.java` — AI 호출 실패 코드 추가

```java
GEMINI_API_ERROR("P008", "Gemini API 호출 중 오류가 발생했습니다.", 500),
```

> P007이 마지막이므로 P008 사용.

---

### Step 2. `NanoBananaServiceImpl.java` — `callAndSave()` 수정

`callAndSave()` 내 RestClient 호출 부분을 try-catch로 감싼다.
Gemini가 반환하는 HTTP 에러(`RestClientResponseException`)와 네트워크·타임아웃 에러(`RestClientException`) 두 경우를 분리하여 로깅하고, 둘 다 `IMAGE_GENERATION_FAILED`로 변환한다.

## 수정 후 에러 흐름 비교

### 수정 전

```
Gemini API 호출 실패 (예: 429 RESOURCE_EXHAUSTED)
  → RestClientResponseException (미처리)
  → GlobalExceptionHandler.handleException(Exception.class)
  → { "code": 500, "message": "서버 내부 오류가 발생했습니다" }
```

### 수정 후

```
Gemini 프롬프트 생성 실패
  → BizException(PromptErrorCode.GEMINI_API_ERROR)
  → GlobalExceptionHandler.handleBizException()
  → { "code": 500, "message": "Gemini API 호출 중 오류가 발생했습니다." }
  + 서버 로그: "Gemini API 오류 응답 — status: 429, body: {errorDetails...}"

NanoBanana 이미지 생성 실패
  → BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED)
  → GlobalExceptionHandler.handleBizException()
  → { "code": 500, "message": "이미지 생성에 실패했습니다." }
  + 서버 로그: "NanoBanana API 오류 응답 — status: 503, body: {errorDetails...}"
```

---

## 에러 코드 최종 정리

### `PromptErrorCode.java`

| 코드 | 이름 | 메시지 | HTTP |
|------|------|--------|------|
| P006 | GEMINI_EMPTY_RESPONSE | Gemini API로부터 유효한 응답을 받지 못했습니다. | 500 |
| P008 | GEMINI_API_ERROR | Gemini API 호출 중 오류가 발생했습니다. | 500 |

### `ImageGenerationErrorCode.java`

| 코드 | 이름 | 메시지 | HTTP | 사용처 |
|------|------|--------|------|--------|
| IG001 | EMPTY_IMAGE_RESPONSE | Gemini 이미지 응답이 비어 있습니다. | 500 | callAndSave() — candidates 없음 |
| IG002 | IMAGE_SAVE_FAILED | 이미지 저장에 실패했습니다. | 500 | saveImage() — IOException |
| IG003 | IMAGE_GENERATION_FAILED | 이미지 생성에 실패했습니다. | 500 | callAndSave() — API 호출 실패 **(수정 후 사용)** |
| IG004 | INVALID_IMAGE_MIME_TYPE | 참조 이미지의 Content-Type을 확인할 수 없습니다. | 400 | generateImage() — mimeType null |
| IG005 | UNSUPPORTED_IMAGE_TYPE | 지원하지 않는 이미지 형식입니다. | 400 | generateImage() — 허용 외 MIME |
| IG006 | IMAGE_TOO_LARGE | 참조 이미지 크기가 허용 범위를 초과합니다. | 400 | generateImage() — 15MB 초과 |
