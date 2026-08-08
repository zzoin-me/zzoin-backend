# 백엔드 수정 사항 전체 문서 (tag.md)

이 문서는 프론트엔드(HICC T3) 작업 세션에서 백엔드에 가해진 모든 수정을 상세히 기록한다.
각 항목은 **파일 → 변경 내용 → 코드 → 왜 변경했는지 → 역할** 구조로 작성되었다.

---

## 작업 개요

총 3개의 큰 작업이 백엔드에 반영되었다:

1. **직군 카테고리 시스템 도입** — 모집 직군을 5개 카테고리 + 27개 세부 직군으로 표준화
2. **상세조건 검색 고도화** — 세부 직군 필터, 카테고리별 카운트 API, 마감임박 정렬, goal/recruitingOnly 필터 추가
3. **사소한 버그 수정** — `@NotBlank` on Integer, 죽은 import 제거

---

## 1. 신규 파일

### 1-1. `entity/enums/RecruitmentCategory.java` (신규)

**역할**: 모집 직군의 큰 분류 5가지를 정의하는 enum.

```java
public enum RecruitmentCategory {
    PLANNING,    // 기획
    DESIGN,      // 디자인
    DEVELOPMENT, // 개발
    MARKETING,   // 마케팅
    ETC          // 기타 (사용자 자유 입력)
}
```

**왜 추가했는가**: 기존에는 모집 직군(`ProjectRecruitment.name`)이 자유 텍스트 문자열이었다. "프론트엔드", "Frontend", "FE" 등 다양한 표기가 섞여 필터링이 정확히 동작하지 않았다 (백엔드 `recruitment.name.eq(field)` 정확 일치). 카테고리 enum을 도입해 정규화된 대분류 기준을 만들었다.

**설계 결정**:
- `CollaborationType`, `GoalType` 등 기존 enum 패턴과 동일하게 단순한 상수 enum (Lombok 없음)
- JPA 엔티티에서 `@Enumerated(EnumType.STRING)`으로 저장 (문자열 "PLANNING" 등이 DB에 저장)
- `ETC`는 예외적으로 name 필드를 자유 텍스트로 허용 (나머지 카테고리는 `RecruitmentRole`에 정의된 세부 직군명만 허용)

---

### 1-2. `entity/enums/RecruitmentRole.java` (신규)

**역할**: 5개 카테고리에 속하는 27개 세부 직군을 정의하는 enum. 카테고리-name 매핑의 **진실 공급원(single source of truth)**.

```java
public enum RecruitmentRole {
    // PLANNING (4개)
    SERVICE_PLANNING("서비스 기획", RecruitmentCategory.PLANNING),
    PM("PM", RecruitmentCategory.PLANNING),
    PROJECT_MANAGER("프로젝트 매니저", RecruitmentCategory.PLANNING),
    BUSINESS_PLANNING("사업 기획", RecruitmentCategory.PLANNING),

    // DESIGN (6개)
    UX("UX", RecruitmentCategory.DESIGN),
    UI("UI", RecruitmentCategory.DESIGN),
    UX_UI("UX/UI", RecruitmentCategory.DESIGN),
    GRAPHIC("그래픽", RecruitmentCategory.DESIGN),
    BRAND_DESIGN("브랜드", RecruitmentCategory.DESIGN),
    ILLUSTRATION("일러스트", RecruitmentCategory.DESIGN),

    // DEVELOPMENT (11개)
    FRONTEND("프론트엔드", RecruitmentCategory.DEVELOPMENT),
    BACKEND("백엔드", RecruitmentCategory.DEVELOPMENT),
    // ... iOS, 안드로이드, 크로스플랫폼, 데스크탑, 게임 클라이언트, 게임 서버, DevOps, 데이터 엔지니어링, 보안

    // MARKETING (6개)
    CONTENT("콘텐츠", RecruitmentCategory.MARKETING),
    // ... 성장, SNS, 브랜드, 광고, PR

    ;
    private final String displayName;   // 사용자에게 보이는 한국어 이름
    private final RecruitmentCategory category;
}
```

**왜 추가했는가**: 카테고리만으로는 "개발"이라는 큰 분류만 필터링 가능하다. 사용자가 "프론트엔드", "백엔드" 등 세부 직군으로 필터링하려면, 각 카테고리에 속하는 유효한 세부 직군 목록이 백엔드에 있어야 한다. 이 enum이 그 목록을 보유한다.

**주요 메서드**:
- `getDisplayName()`: 한국어 표시명 반환 (예: `FRONTEND` → `"프론트엔드"`)
- `getCategory()`: 해당 역할이 속한 카테고리 반환
- `fromDisplayName(String name)`: 표시명으로 enum 찾기 (null 반환 가능)
- `displayNamesByCategory(RecruitmentCategory category)`: 특정 카테고리의 모든 표시명 List 반환 — **검증 로직에서 핵심**

**설계 결정**:
- enum 상수명은 영어(`FRONTEND`), 표시명은 한국어(`"프론트엔드"`)로 분리 — DB에는 enum명이 아닌 표시명이 `ProjectRecruitment.name`에 저장됨
- "브랜드"가 DESIGN과 MARKETING 양쪽에 존재 (`BRAND_DESIGN`, `BRAND_MARKETING`) — 동일 표시명이지만 카테고리가 다름. `(category, name)` 쌍으로 검증하므로 문제 없음
- 프론트엔드 `src/constants/recruitment.ts`가 이 enum의 사본 (직군 추가 시 양쪽 업데이트 필요)

---

## 2. 엔티티 수정

### 2-1. `entity/ProjectRecruitment.java`

**변경**: `category` 컬럼 추가 + 죽은 import 제거

```java
// 제거됨: import org.antlr.v4.runtime.misc.Interval;  (사용되지 않는 import)

// 추가됨:
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private RecruitmentCategory category;
```

**왜**: 카테고리 필터링의 기반이 되는 컬럼. `name`은 세부 직군명(예: "프론트엔드"), `category`는 대분류(예: `DEVELOPMENT`)를 저장한다.

**역할**:
- `@Enumerated(EnumType.STRING)`: enum을 문자열("DEVELOPMENT")로 DB에 저장. 서수(0,1,2) 저장 시 enum 순서 변경에 취약하므로 STRING 사용
- `@Column(nullable = false)`: 카테고리는 필수값. NOT NULL 제약
- Hibernate `ddl-auto=update`가 자동으로 컬럼 추가 (기존 row는 null이 되지만, 개발 초기 단계라 무시)

**name vs category 분담**:
| 필드 | 예시 | 용도 |
|------|------|------|
| `name` | "프론트엔드", "데이터 분석" | 표시용 (카드 태그, 지원 모달). ETC는 자유 입력 |
| `category` | `DEVELOPMENT`, `ETC` | 필터링용 (큰 카테고리별 검색) |

---

## 3. DTO 수정

### 3-1. `dto/project/CreateRecruitmentRequestDTO.java`

**변경**: `category` 필드 추가 + `count` 어노테이션 버그 수정

```java
// 추가됨:
@NotNull
private RecruitmentCategory category;

// 수정됨 (버그 수정):
@NotBlank  →  @NotNull
@Min(0)
@Max(100)
private Integer count;
```

**왜 category를 추가했는가**: 생성 요청 시 클라이언트가 카테고리를 보내야 한다. `@NotNull`로 필수값 지정.

**왜 count의 `@NotBlank`를 `@NotNull`로 바꿨는가**: `@NotBlank`는 `CharSequence`(String) 전용 어노테이션이다. `Integer` 타입에 적용하면 Hibernate Validator가 검증을 수행하지 않거나 예외를 발생시킨다. `@NotNull` + `@Min(0)` + `@Max(100)` 조합이 Integer 검증에 올바르다. 이것은 기존부터 존재하던 백엔드 버그였다.

---

### 3-2. `dto/project/UpdateRecruitmentRequestDTO.java`

**변경**: `category` 필드 추가

```java
private RecruitmentCategory category;  // optional (수정 시에만)
```

**왜**: 프로젝트 수정 시 모집 역할의 카테고리도 변경 가능해야 한다. Update DTO는 모든 필드가 optional이므로 `@NotNull` 없이 추가.

---

### 3-3. `dto/project/RecruitmentDetailResponseDTO.java`

**변경**: `category` 필드 추가 + 죽은 import 제거

```java
// 제거됨: Max, Min, NotBlank, Size import (응답 DTO라 검증 어노테이션 불필요)
// 추가됨:
private RecruitmentCategory category;
// from() 메서드에:
.category(projectRecruitment.getCategory())
```

**왜**: 상세 조회 응답에 카테고리를 포함해 프론트엔드가 표시할 수 있게 한다 (역할 카드 제목 옆에 카테고리 라벨 표시용).

---

### 3-4. `dto/project/ProjectPreviewResponseDTO.java`

**변경**: `categories` 리스트 추가 + 죽은 import 제거

```java
// 제거됨: CollaborationType, GoalType import (사용 안 함)
// 추가됨:
private List<RecruitmentCategory> categories;
// from() 메서드에:
.categories(recruitments.stream().map(ProjectRecruitment::getCategory).distinct().toList())
```

**왜**: 프로젝트 목록(카드)에서 이 프로젝트가 어떤 카테고리의 직군을 모집 중인지 알 수 있게 한다. `distinct()`로 중복 제거 (한 프로젝트가 "프론트엔드"와 "백엔드" 둘 다 모집해도 카테고리는 `DEVELOPMENT` 하나).

**역할**: 프론트엔드에서 카드 카테고리별 색상 분류, 필터링 힌트 등에 사용 가능. `recruitments: List<String>` (세부 직군명)은 그대로 유지 — 카드 태그에는 세부 직군명을 표시.

---

### 3-5. `dto/project/myproject/MyApplicationPreviewResponseDTO.java`

**변경**: `appliedRecruitmentCategory` 추가 + 죽은 import 제거

```java
// 제거됨: Project, ProjectRecruitment, ProjectStatus, List import
// 추가됨:
private RecruitmentCategory appliedRecruitmentCategory;
// from() 메서드에:
.appliedRecruitmentCategory(application.getRecruitment().getCategory())
```

**왜**: "내 지원 현황" 페이지에서 카테고리별 필터링이 가능해야 한다. 기존에는 `appliedRecruitmentName`(문자열)으로만 필터링했는데, 이는 정확 매칭이라 "프론트엔드"로 지원했어도 필터 "프론트엔드"와 매칭이 안 되는 버그가 있었다. 카테고리 enum으로 필터링하면 정확히 매칭된다.

---

### 3-6. `dto/application/ProjectApplicantResponseDTO.java`

**변경**: `recruitmentCategory` 추가 + 죽은 import 제거

```java
// 제거됨: RecruitmentDetailResponseDTO, CollaborationType, GoalType, ProjectStatus import
// 추가됨:
private RecruitmentCategory recruitmentCategory;
// from() 메서드에:
.recruitmentCategory(application.getRecruitment().getCategory())
```

**왜**: 지원자 목록/상세에서 지원자가 어떤 카테고리에 지원했는지 표시하기 위해. 지원자 관리 페이지에서 카테고리별 정렬/필터링에 사용 가능.

---

## 4. 서비스 수정

### 4-1. `service/ProjectService.java`

**변경**: 검증 로직 추가 + category 매핑 + import 추가

**추가된 import**:
```java
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import com.hicct3.projectfinder.entity.enums.RecruitmentRole;
```

#### createProject (프로젝트 생성)

```java
// 기존:
req.getRecruitments().forEach(x ->
    projectRecruitmentRepository.save(ProjectRecruitment.builder()
        .name(x.getName())
        ...));

// 변경:
req.getRecruitments().forEach(x -> {
    validateRecruitmentRole(x.getCategory(), x.getName());  // 검증 추가
    projectRecruitmentRepository.save(ProjectRecruitment.builder()
        .name(x.getName())
        .category(x.getCategory())  // category 추가
        ...));
});
```

**왜**: 생성 시 카테고리-name 일치를 검증하고, 카테고리를 DB에 저장한다.

#### updateProject (프로젝트 수정)

두 분기 모두 동일하게 수정:
1. **신규 역할 생성 분기** (`recruitmentId == null`): `validateRecruitmentRole` 호출 + `.category(x.getCategory())` 추가
2. **기존 역할 업데이트 분기**: `validateRecruitmentRole` 호출 + `recruitment.setCategory(x.getCategory())` 추가

**왜**: 수정 시에도 동일한 검증이 필요. 신규 역할 추가든 기존 역할 수정이든 카테고리-name 일치를 보장.

#### validateRecruitmentRole 메서드 (신규)

```java
private void validateRecruitmentRole(RecruitmentCategory category, String name) {
    if (category == null || category == RecruitmentCategory.ETC) {
        return;
    }
    if (name == null || name.isBlank()) {
        throw new GeneralException(ErrorCode.INVALID_RECRUITMENT_ROLE);
    }
    var allowed = RecruitmentRole.displayNamesByCategory(category);
    if (!allowed.contains(name)) {
        throw new GeneralException(ErrorCode.INVALID_RECRUITMENT_ROLE);
    }
}
```

**역할**: 클라이언트가 보낸 (category, name) 쌍이 유효한지 검증.

**동작**:
- `ETC` 카테고리: 검증 생략 (자유 텍스트 허용)
- 그 외 카테고리: `RecruitmentRole.displayNamesByCategory(category)`로 해당 카테고리의 유효한 세부 직군명 목록을 가져와 `name`이 포함되는지 확인
- 불일치 시 `INVALID_RECRUITMENT_ROLE` 에러 (400 Bad Request)

**왜 필요한가**: 프론트엔드 UI로 막아도 API를 직접 호출하면 우회 가능하다. 백엔드에서 `category=DEVELOPMENT, name="디자인"` 같은 모순된 데이터가 저장되는 것을 방지한다. 데이터 정합성의 최후 방어선.

---

## 5. 검색 쿼리 수정

### 5-1. `repository/ProjectRepositoryImpl.java` (대규모 수정)

이 파일은 상세조건 검색의 핵심이다. 총 5개 변경이 있다.

#### (1) `recruitmentContains` — 카테고리 + 세부 직군 통합 필터

기존 `categoryContains(category)`를 대체:

```java
private BooleanExpression recruitmentContains(RecruitmentCategory category, String name) {
    if (category == null && (name == null || name.isBlank())) {
        return null;
    }
    QProject project = QProject.project;
    QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

    BooleanExpression cond = recruitment.deletedAt.isNull();
    if (category != null) {
        cond = cond.and(recruitment.category.eq(category));
    }
    if (name != null && !name.isBlank()) {
        cond = cond.and(recruitment.name.eq(name));
    }

    return project.id.in(
            JPAExpressions
                    .select(recruitment.project.id)
                    .from(recruitment)
                    .where(cond)
    );
}
```

**역할**: 카테고리와 세부 직군명으로 프로젝트를 필터링하는 서브쿼리.

**동작**:
- category만 있으면: 해당 카테고리의 모집글을 가진 프로젝트
- name만 있으면: 해당 이름의 모집글을 가진 프로젝트
- 둘 다 있으면: 해당 카테고리 + 해당 이름인 모집글을 가진 프로젝트 (AND 조건)
- 서브쿼리 패턴: `project.id IN (SELECT recruitment.project.id FROM recruitment WHERE ...)`

**왜 기존을 대체했는가**: 기존 `categoryContains`는 카테고리만 필터링했다. 세부 직군(name) 필터링 요구사항이 추가되어 두 조건을 하나의 서브쿼리로 통합했다. 별도 메서드로 분리하면 AND 결합 시 서브쿼리가 2번 실행되어 비효율적이다.

#### (2) `countBetween` — name 스코프 추가

```java
// 기존: countBetween(minCount, maxCount, category)
// 변경: countBetween(minCount, maxCount, category, name)
// 내부 조건에 name 추가:
if (name != null && !name.isBlank()) {
    conditions = conditions.and(recruitment.name.eq(name));
}
```

**역할**: 모집 인원수 필터. category와 name이 모두 주어지면 해당 스코프에서만 인원수 합산.

**왜**: "개발 카테고리의 프론트엔드 직군 모집인원 1~3명" 같은 세부 필터링이 정확히 동작하려면, 인원수 합산도 name 스코프로 제한해야 한다.

#### (3) `goalContains` + `recruitingOnlyContains` — 신규 필터

```java
private BooleanExpression goalContains(GoalType goal) {
    if (goal == null) {
        return null;
    }
    return QProject.project.goal.eq(goal);
}

private BooleanExpression recruitingOnlyContains(Boolean recruitingOnly) {
    if (recruitingOnly == null || !recruitingOnly) {
        return null;
    }
    return QProject.project.status.eq(ProjectStatus.RECRUITING);
}
```

**역할**:
- `goalContains`: 프로젝트 목표(포트폴리오/출시/공모전)로 필터링. `Project.goal`은 기존 enum 필드.
- `recruitingOnlyContains`: "모집 중만 보기" 토글. `status = RECRUITING`인 프로젝트만.

**왜**: 상세조건에 프로젝트 목표 필터와 모집중 필터가 추가되었다. 엔티티에 이미 필드가 존재하므로 단순 `eq` 조건으로 구현.

#### (4) `sortOrder` — DEADLINE 정렬 실제 반영

```java
// 기존 (버그):
private OrderSpecifier<?> sortOrder(QProject project, SortType sortType) {
    return project.createdAt.desc();  // sortType 무시, 항상 최신순
}

// 수정:
private OrderSpecifier<?> sortOrder(QProject project, SortType sortType) {
    if (sortType == SortType.DEADLINE) {
        return project.recruitmentDeadline.asc();  // 마감임박순
    }
    return project.createdAt.desc();  // 신규순
}
```

**역할**: `sortType` 파라미터를 실제로 반영해 정렬을 결정.

**왜**: 기존 코드는 `sortType`을 받고도 무시하고 항상 `createdAt.desc()`만 반환했다. 마감임박 탭이 제대로 동작하려면 `recruitmentDeadline.asc()`가 필요하다. 이것은 기존부터 존재하던 버그였다.

#### (5) `countProjectsPerCategory` — 카테고리별 카운트 (신규)

```java
@Override
public Map<RecruitmentCategory, Long> countProjectsPerCategory() {
    QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;
    QProject project = QProject.project;

    List<Tuple> rows = queryFactory
            .select(recruitment.category, recruitment.project.id.countDistinct())
            .from(recruitment)
            .join(recruitment.project, project)
            .where(recruitment.deletedAt.isNull().and(project.deletedAt.isNull()))
            .groupBy(recruitment.category)
            .fetch();

    Map<RecruitmentCategory, Long> map = new EnumMap<>(RecruitmentCategory.class);
    for (Tuple t : rows) {
        RecruitmentCategory cat = t.get(recruitment.category);
        Long cnt = t.get(recruitment.project.id.countDistinct());
        if (cat != null && cnt != null) {
            map.put(cat, cnt);
        }
    }
    return map;
}
```

**역할**: 각 카테고리별로 모집 중인 프로젝트 수를 카운트해 `Map`으로 반환.

**동작**:
- `projectRecruitments` 테이블을 `projects`와 JOIN
- 삭제되지 않은 모집글(`deletedAt IS NULL`) + 삭제되지 않은 프로젝트만
- 카테고리별로 GROUP BY
- `countDistinct(project.id)`: 한 프로젝트가 같은 카테고리의 모집글을 여러 개 가져도 1번만 카운트 (예: "프론트엔드 2명, 백엔드 3명" 둘 다 DEVELOPMENT지만 프로젝트는 1개)
- 결과를 `EnumMap<RecruitmentCategory, Long>`으로 반환

**왜**: 프론트엔드 상세조건 패널에서 카테고리 버튼 옆에 "기획 (12)", "개발 (45)" 식으로 프로젝트 수를 표시하기 위해. 마운트 시 1회 호출 (필터와 무관한 전체 기준 고정값).

**설계 결정**:
- `EnumMap` 사용: enum 키에 특화된 Map 구현체, `HashMap`보다 효율적
- 카테고리가 없는(0개) 경우 Map에 키 자체가 없음 → 프론트엔드에서 `?? 0` 처리

---

### 5-2. `repository/ProjectRepositoryCustom.java`

**변경**: `searchProjects` 시그니처 확장 + `countProjectsPerCategory` 추가

```java
// searchProjects 파라미터 확장:
Page<Project> searchProjects(
    SortType sortType, String keyword, RecruitmentCategory category,
    String name,              // 추가 (세부 직군)
    Integer maxDays, Integer minCount, Integer maxCount,
    GoalType goal,            // 추가
    Boolean recruitingOnly,   // 추가
    Pageable pageable
);

// 신규 메서드:
Map<RecruitmentCategory, Long> countProjectsPerCategory();
```

**왜**: Impl에서 새 기능을 구현했으므로 인터페이스에도 시그니처를 추가해야 한다.

---

## 6. 서비스/컨트롤러 전파

### 6-1. `service/ProjectQueryService.java`

**변경**: `getProjectList` 시그니처 확장 + `countProjectsPerCategory` 위임

```java
// 시그니처 확장:
public Page<ProjectPreviewResponseDTO> getProjectList(
    SortType sort, String keyword, RecruitmentCategory category,
    String name, Integer maxDays, Integer minCount, Integer maxCount,
    GoalType goal, Boolean recruitingOnly, Pageable pageable
) {
    return projectRepository.searchProjects(
        sort, keyword, category, name, maxDays, minCount, maxCount,
        goal, recruitingOnly, pageable
    ).map(...);
}

// 신규 위임:
public Map<RecruitmentCategory, Long> countProjectsPerCategory() {
    return projectRepository.countProjectsPerCategory();
}
```

**왜**: Controller → Service → Repository 흐름에서 중간 다리 역할. 비즈니스 로직은 없고 단순 위임.

---

### 6-2. `controller/ProjectController.java`

**변경**: param 추가 + `/category-counts` 엔드포인트 신규

#### getProjects param 확장

```java
@GetMapping
public ApiResponse<Page<ProjectPreviewResponseDTO>> getProjects(
    @RequestParam(required = false) String keyword,
    @RequestParam(defaultValue = "LATEST") String sort,
    @RequestParam(required = false) RecruitmentCategory category,
    @RequestParam(required = false) String name,              // 추가
    @RequestParam(required = false) Integer maxDays,
    @RequestParam(required = false) Integer minCount,
    @RequestParam(required = false) Integer maxCount,
    @RequestParam(required = false) GoalType goal,            // 추가
    @RequestParam(required = false, defaultValue = "false") Boolean recruitingOnly,  // 추가
    Pageable pageable
)
```

**왜**: 프론트엔드 상세조건에서 새 필터(name, goal, recruitingOnly)를 보낼 수 있게 param을 열었다.

**동작**:
- `name`: 세부 직군명 (예: "프론트엔드"). `?name=프론트엔드`
- `goal`: GoalType enum (예: "PORTFOLIO"). Spring이 자동 파싱
- `recruitingOnly`: boolean. 기본 false. `?recruitingOnly=true`

#### `/category-counts` 엔드포인트 신규

```java
@GetMapping("/category-counts")
public ApiResponse<Map<RecruitmentCategory, Long>> getCategoryCounts() {
    return ApiResponse.onSuccess(projectQueryService.countProjectsPerCategory());
}
```

**왜**: 카테고리별 프로젝트 수를 반환하는 전용 엔드포인트.

**라우팅 주의**: `@GetMapping("/category-counts")`를 `@GetMapping("/{projectId}")`보다 **앞에** 배치했다. Spring MVC는 더 구체적인 경로(`/category-counts`)를 먼저 매칭하므로, `category-counts`가 projectId로 해석되지 않는다. (만약 순서가 뒤바뀌면 `/category-counts`를 `projectId="category-counts"`로 파싱하려다 NumberFormatException 발생)

---

## 7. enum 수정

### 7-1. `entity/enums/SortType.java`

**변경**: `DEADLINE` 추가

```java
public enum SortType {
    LATEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    DEADLINE(Sort.by(Sort.Direction.ASC, "recruitmentDeadline"));  // 추가
    ...
}
```

**왜**: "마감임박순" 정렬을 지원하기 위해. `recruitmentDeadline` 오름차순 = 마감이 가까운 순.

**참고**: `Sort.by(...)`의 `sort` 필드는 현재 코드에서 사용되지 않는다 (`ProjectRepositoryImpl.sortOrder`가 직접 `OrderSpecifier`를 반환). 향후 Spring Data Sort 기반 정렬을 쓸 때를 대비해 유지. `from()` 메서드는 알 수 없는 값이 들어오면 LATEST로 폴백하므로 하위 호환성 유지.

---

## 8. 에러 코드 추가

### 8-1. `global/ErrorCode.java`

**변경**: `INVALID_RECRUITMENT_ROLE` 추가

```java
INVALID_RECRUITMENT_ROLE(HttpStatus.BAD_REQUEST, "모집 직군이 카테고리와 일치하지 않습니다."),
```

**왜**: `ProjectService.validateRecruitmentRole`이 카테고리-name 불일치 시 throw할 에러 코드. HTTP 400 Bad Request.

---

## 전체 수정 파일 요약

| 파일 | 유형 | 변경 요약 |
|------|------|-----------|
| `entity/enums/RecruitmentCategory.java` | 신규 | 5개 카테고리 enum |
| `entity/enums/RecruitmentRole.java` | 신규 | 27개 세부 직군 enum + 검증 헬퍼 |
| `entity/enums/SortType.java` | 수정 | DEADLINE 정렬 추가 |
| `entity/ProjectRecruitment.java` | 수정 | category 컬럼 + 죽은 import 제거 |
| `dto/project/CreateRecruitmentRequestDTO.java` | 수정 | category 필드 + @NotBlank→@NotNull 버그 수정 |
| `dto/project/UpdateRecruitmentRequestDTO.java` | 수정 | category 필드 |
| `dto/project/RecruitmentDetailResponseDTO.java` | 수정 | category 반환 + 죽은 import 제거 |
| `dto/project/ProjectPreviewResponseDTO.java` | 수정 | categories 리스트 + 죽은 import 제거 |
| `dto/project/myproject/MyApplicationPreviewResponseDTO.java` | 수정 | appliedRecruitmentCategory + 죽은 import 제거 |
| `dto/application/ProjectApplicantResponseDTO.java` | 수정 | recruitmentCategory + 죽은 import 제거 |
| `service/ProjectService.java` | 수정 | validateRecruitmentRole + create/update 매핑 |
| `service/ProjectQueryService.java` | 수정 | 시그니처 확장 + countProjectsPerCategory 위임 |
| `repository/ProjectRepositoryCustom.java` | 수정 | 시그니처 확장 + countProjectsPerCategory |
| `repository/ProjectRepositoryImpl.java` | 수정 | recruitmentContains + countBetween name + goalContains + recruitingOnlyContains + sortOrder 수정 + countProjectsPerCategory |
| `controller/ProjectController.java` | 수정 | name/goal/recruitingOnly param + /category-counts 엔드포인트 |
| `global/ErrorCode.java` | 수정 | INVALID_RECRUITMENT_ROLE 추가 |

**총 16개 파일** (신규 2, 수정 14)

---

## 데이터 흐름 요약

### 프로젝트 생성 시
```
프론트 RecruitmentSelect (category + name 선택)
  → POST /api/projects { recruitments: [{ category, name, count, ... }] }
  → ProjectController.createProject
  → ProjectService.createProject
    → validateRecruitmentRole(category, name)  // category-name 일치 검증
    → ProjectRecruitment 저장 (name + category 컬럼)
```

### 상세조건 검색 시
```
프론트 상세조건 패널
  → GET /api/projects?category=DEVELOPMENT&name=프론트엔드&goal=PORTFOLIO&recruitingOnly=true&sort=DEADLINE
  → ProjectController.getProjects
  → ProjectQueryService.getProjectList
  → ProjectRepositoryImpl.searchProjects
    → recruitmentContains(DEVELOPMENT, "프론트엔드")  // 카테고리+세부직군 필터
    → goalContains(PORTFOLIO)                         // 목표 필터
    → recruitingOnlyContains(true)                    // 모집중만
    → sortOrder(project, DEADLINE)                    // 마감임박 정렬
```

### 카테고리 카운트 표시 시
```
프론트 마운트 시 1회
  → GET /api/projects/category-counts
  → ProjectController.getCategoryCounts
  → ProjectQueryService.countProjectsPerCategory
  → ProjectRepositoryImpl.countProjectsPerCategory  // GROUP BY category
  → { PLANNING: 12, DEVELOPMENT: 45, ... }
  → 프론트 카테고리 버튼에 "기획 (12)" 표시
```

---
