# MyBatis Mapper 리팩터링 계획

토의 결과 2가지 항목만 진행한다.

- **1번 (parameterType 명시)** → 불필요. Java Mapper Interface 방식에서 MyBatis가 인터페이스 시그니처에서 자동 추론. 현재 상태 유지.
- **3번 (updateImageUrl DTO 전환)** → 3개 파라미터는 수용 가능 범위. 현재 상태 유지.
- **1번 (void → int 전환)** → 서비스 전 도메인에서 selectById 선행 검증 패턴이 확립되어 있어 활용할 일 없음. YAGNI 원칙. 현재 상태 유지.

---

## Task 1 — `@Param` 누락 일관성 적용

### 배경

단일 primitive/wrapper 파라미터에 `@Param`이 없으면 현재는 동작하지만,
파라미터를 하나 추가하는 순간 XML의 `#{변수명}` 매핑이 조용히 깨진다 (런타임 오류, 컴파일 타임에 미탐지).

### 제외 기준

- Entity/DTO 단일 객체 파라미터: `insert(User user)`, `update(Category category)`, `selectList(UserSearchReqDto dto)` 등
  → `@Param` 추가 시 XML에서 `#{user.id}` 형태로 바꿔야 해서 오히려 복잡해짐. 현재 방식이 정상.
- 이미 `@Param` 적용된 메서드: 변경 불필요.
- `HumanPromptStepMapper.selectAllWithAttrsAndOptions()`: 파라미터 없음. 변경 불필요.

### 변경 대상 (Java 파일 5개, XML 변경 없음)

| 파일 | 변경 메서드 |
|---|---|
| `UserMapper.java` | `selectById`, `selectByLoginId`, `deleteById` |
| `CategoryMapper.java` | `selectById`, `selectList`, `existsByParentId`, `selectCategoryPath`, `deleteById` |
| `TagMapper.java` | `selectById`, `selectList`, `existsByTagGroupId`, `deleteById` |
| `TagGroupMapper.java` | `selectById`, `deleteById` |
| `PromptHistoryMapper.java` | `selectById` |

- [ ] `domain/user/mapper/UserMapper.java`
- [ ] `domain/category/mapper/CategoryMapper.java`
- [ ] `domain/tag/mapper/TagMapper.java`
- [ ] `domain/tag/mapper/TagGroupMapper.java`
- [ ] `domain/prompt/mapper/PromptHistoryMapper.java`

---

## Task 2 — `selectAll` → `selectList` 네이밍 통일

### 배경

`TagGroupMapper`만 `selectAll()`을 사용하고 나머지 전 도메인은 `selectList()`를 사용 중.
`selectAll`은 나중에 조건 필터링이 추가되면 이름이 misleading해지므로 중립적인 `selectList`로 통일.

- `HumanPromptStepMapper.selectAllWithAttrsAndOptions()`: 3테이블 조인을 표현하는 복합 이름이므로 변경 대상 아님.

### 변경 대상 (3개 파일)

| 파일 | 변경 내용 |
|---|---|
| `domain/tag/mapper/TagGroupMapper.java` | `selectAll()` → `selectList()` |
| `mybatis/mappers/tag/TagGroupMapper.xml` | `id="selectAll"` → `id="selectList"` |
| `domain/tag/service/TagGroupServiceImpl.java` | `tagGroupMapper.selectAll()` → `tagGroupMapper.selectList()` |

- [ ] `domain/tag/mapper/TagGroupMapper.java`
- [ ] `mybatis/mappers/tag/TagGroupMapper.xml`
- [ ] `domain/tag/service/TagGroupServiceImpl.java`
