# Refactor: @RestController Validation 누락 보정

## 목적
`@PathVariable Long id`에 `@Positive` 적용, `@RequestBody`에 `@Valid` 누락 보정, 클래스 레벨 `@Validated` 추가.
`@Validated`가 없으면 `@Positive` 같은 메서드 파라미터 제약 어노테이션이 AOP로 동작하지 않음.

---

## 작업 체크리스트

### CategoryController
- [ ] import 추가: `jakarta.validation.constraints.Positive`, `org.springframework.validation.annotation.Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `getCategory(@PathVariable Long id)` → `@Positive` 추가
- [ ] `updateCategory(@PathVariable Long id, @RequestBody CategoryUpdateReqDto dto)` → `@Positive` + `@Valid` 추가
- [ ] `deleteCategory(@PathVariable Long id)` → `@Positive` 추가

### TagController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `getTag` → `@Positive`
- [ ] `updateTag` → `@Positive` + `@Valid`
- [ ] `deleteTag` → `@Positive`

### TagGroupController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `getTagGroup` → `@Positive`
- [ ] `updateTagGroup` → `@Positive` + `@Valid`
- [ ] `deleteTagGroup` → `@Positive`

### HumanPromptAttrController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `updateAttr` → `@Positive` (`@Valid`는 이미 있음)
- [ ] `deleteAttr` → `@Positive`

### HumanPromptOptionController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `updateOption` → `@Positive` (`@Valid`는 이미 있음)
- [ ] `deleteOption` → `@Positive`

### HumanPromptController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `updateStep` → `@Positive` + `@Valid` 추가
- [ ] `deleteStep` → `@Positive`

### UserController
- [ ] import 추가: `Positive`, `Validated`
- [ ] 클래스 레벨 `@Validated` 추가
- [ ] `getUser` → `@Positive`
- [ ] `updateUser` → `@Positive` (`@Valid`는 이미 있음)
- [ ] `deleteUser` → `@Positive`
- [ ] `getUserUsage(@PathVariable Long id)` → `@Positive`

---

## 완료 후 처리
- `docs/todo.md` 완료 항목 반영
- 이 파일 삭제
