package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.QProject;
import com.hicct3.projectfinder.entity.QProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Project> searchProjects(
            SortType sortType,
            String keyword,
            JobCategoryCode category,
            String name,
            Integer maxDays,
            Integer minCount,
            Integer maxCount,
            GoalType goal,
            Boolean recruitingOnly,
            Pageable pageable
    ) {
        QProject project = QProject.project;

        BooleanExpression where = project.deletedAt.isNull();

        where = where.and(keywordContains(keyword));
        where = where.and(maxDaysContains(maxDays));
        where = where.and(recruitmentContains(category, name));
        where = where.and(countBetween(minCount, maxCount, category, name));
        where = where.and(goalContains(goal));
        where = where.and(recruitingOnlyContains(recruitingOnly));

        List<Project> content = queryFactory
                .selectFrom(project)
                .where(where)
                .orderBy(
                    statusOrder(project),
                    sortOrder(project, sortType)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(project.count())
                .from(project)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Map<JobCategoryCode, Long> countProjectsPerCategory() {
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;
        QProject project = QProject.project;

        List<Tuple> rows = queryFactory
                .select(recruitment.jobRole.jobCategory.categoryCode, recruitment.project.id.countDistinct())
                .from(recruitment)
                .join(recruitment.project, project)
                .where(recruitment.deletedAt.isNull().and(project.deletedAt.isNull()))
                .groupBy(recruitment.jobRole.jobCategory.categoryCode)
                .fetch();

        Map<JobCategoryCode, Long> map = new EnumMap<>(JobCategoryCode.class);
        for (Tuple t : rows) {
            JobCategoryCode cat = t.get(recruitment.jobRole.jobCategory.categoryCode);
            Long cnt = t.get(recruitment.project.id.countDistinct());
            if (cat != null && cnt != null) {
                map.put(cat, cnt);
            }
        }
        return map;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        QProject project = QProject.project;
        return project.title.contains(keyword)
                .or(project.description.contains(keyword));
    }

    private BooleanExpression maxDaysContains(Integer maxDays) {
        if (maxDays == null) {
            return null;
        }
        QProject project = QProject.project;
        return project.recruitmentDeadline.loe(
                LocalDateTime.now().plusDays(maxDays)
        );
    }

    private BooleanExpression recruitmentContains(JobCategoryCode category, String name) {
        if (category == null && (name == null || name.isBlank())) {
            return null;
        }
        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression cond = recruitment.deletedAt.isNull();
        if (category != null) {
            cond = cond.and(recruitment.jobRole.jobCategory.categoryCode.eq(category));
        }
        if (name != null && !name.isBlank()) {
            cond = cond.and(recruitment.jobRole.name.eq(name));
        }

        return project.id.in(
                JPAExpressions
                        .select(recruitment.project.id)
                        .from(recruitment)
                        .where(cond)
        );
    }

    private BooleanExpression countBetween(Integer minCount, Integer maxCount, JobCategoryCode category, String name) {
        if (minCount == null && maxCount == null) {
            return null;
        }

        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression conditions = recruitment.project.id.eq(project.id)
                .and(recruitment.deletedAt.isNull());

        if (category != null) {
            conditions = conditions.and(recruitment.jobRole.jobCategory.categoryCode.eq(category));
        }
        if (name != null && !name.isBlank()) {
            conditions = conditions.and(recruitment.jobRole.name.eq(name));
        }

        var sumExpression = Expressions.asNumber(
                JPAExpressions
                        .select(recruitment.recruitmentCount.sum())
                        .from(recruitment)
                        .where(conditions)
        );

        if (minCount != null && maxCount != null) {
            return sumExpression.between(minCount, maxCount);
        } else if (minCount != null) {
            return sumExpression.goe(minCount);
        } else {
            return sumExpression.loe(maxCount);
        }
    }

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

    private OrderSpecifier<Integer> statusOrder(QProject project) {
        return new CaseBuilder()
                .when(project.status.eq(ProjectStatus.RECRUITING))
                .then(1)
                .when(project.status.eq(ProjectStatus.IN_PROGRESS))
                .then(2)
                .when(project.status.eq(ProjectStatus.COMPLETED))
                .then(3)
                .otherwise(4)
                .asc();
    }

    private OrderSpecifier<?> sortOrder(QProject project, SortType sortType) {
        if (sortType == SortType.DEADLINE) {
            return project.recruitmentDeadline.asc();
        }
        if (sortType == SortType.POPULAR) {
            return project.viewCount.desc();
        }
        return project.createdAt.desc();
    }

    @Override
    public Page<Project> findPopularProjects(Pageable pageable) {
        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression where = project.deletedAt.isNull();

        List<Project> allCandidates = queryFactory
                .selectFrom(project)
                .where(where)
                .fetch();

        List<Long> projectIds = allCandidates.stream().map(Project::getId).toList();

        Map<Long, Long> applicantCounts = projectIds.isEmpty() ? Map.of() : queryFactory
                .select(recruitment.project.id, com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.count())
                .from(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication)
                .join(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.recruitment, recruitment)
                .where(recruitment.project.id.in(projectIds))
                .groupBy(recruitment.project.id)
                .fetch()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        t -> t.get(recruitment.project.id),
                        t -> t.get(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.count())
                ));

        List<Project> sorted = allCandidates.stream().sorted((a, b) -> {
            double scoreA = calculatePopularScore(a, applicantCounts.getOrDefault(a.getId(), 0L));
            double scoreB = calculatePopularScore(b, applicantCounts.getOrDefault(b.getId(), 0L));
            return Double.compare(scoreB, scoreA);
        }).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<Project> content = start < sorted.size()
                ? sorted.subList(start, end)
                : List.of();

        return new PageImpl<>(content, pageable, sorted.size());
    }

    @Override
    public Page<Project> findRecommendProjects(Long userId, List<String> userFields, Pageable pageable) {
        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression where = project.deletedAt.isNull();

        List<Long> appliedProjectIds = queryFactory
                .select(recruitment.project.id)
                .from(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication)
                .join(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.recruitment, recruitment)
                .where(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.user.userId.eq(userId))
                .distinct()
                .fetch();

        if (!appliedProjectIds.isEmpty()) {
            where = where.and(project.id.notIn(appliedProjectIds));
        }

        List<Project> allCandidates = queryFactory
                .selectFrom(project)
                .where(where)
                .fetch();

        List<Long> projectIds = allCandidates.stream().map(Project::getId).toList();

        Map<Long, List<String>> roleNamesByProject = projectIds.isEmpty() ? Map.of() : queryFactory
                .select(recruitment.project.id, recruitment.jobRole.name)
                .from(recruitment)
                .where(recruitment.project.id.in(projectIds)
                        .and(recruitment.deletedAt.isNull()))
                .fetch()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.get(recruitment.project.id),
                        java.util.stream.Collectors.mapping(t -> t.get(recruitment.jobRole.name), java.util.stream.Collectors.toList())
                ));

        Map<Long, Long> applicantCounts = projectIds.isEmpty() ? Map.of() : queryFactory
                .select(recruitment.project.id, com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.count())
                .from(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication)
                .join(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.recruitment, recruitment)
                .where(recruitment.project.id.in(projectIds))
                .groupBy(recruitment.project.id)
                .fetch()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        t -> t.get(recruitment.project.id),
                        t -> t.get(com.hicct3.projectfinder.entity.QProjectApplication.projectApplication.count())
                ));

        List<Project> sorted = allCandidates.stream().sorted((a, b) -> {
            double scoreA = calculateRecommendScore(a, userFields,
                    roleNamesByProject.getOrDefault(a.getId(), List.of()),
                    applicantCounts.getOrDefault(a.getId(), 0L));
            double scoreB = calculateRecommendScore(b, userFields,
                    roleNamesByProject.getOrDefault(b.getId(), List.of()),
                    applicantCounts.getOrDefault(b.getId(), 0L));
            return Double.compare(scoreB, scoreA);
        }).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<Project> content = start < sorted.size()
                ? sorted.subList(start, end)
                : List.of();

        return new PageImpl<>(content, pageable, sorted.size());
    }

    private double calculateRecommendScore(Project p, List<String> userFields,
                                           List<String> roleNames, long applicantCount) {
        int fieldMatch = 0;
        if (userFields != null && !userFields.isEmpty()) {
            java.util.Set<String> userFieldSet = new java.util.HashSet<>(userFields);
            for (String name : roleNames) {
                if (userFieldSet.contains(name)) fieldMatch++;
            }
        }

        double base = fieldMatch * 40.0
                + p.getAuthor().getRatingAvg() * 20.0
                + applicantCount * 10.0
                + p.getViewCount() * 1.0;

        long daysToDeadline = java.time.Duration.between(
                java.time.LocalDateTime.now(), p.getRecruitmentDeadline()).toDays();
        if (daysToDeadline <= 3) base *= 1.5;
        else if (daysToDeadline <= 7) base *= 1.2;

        long daysSinceCreated = java.time.Duration.between(
                p.getCreatedAt(), java.time.LocalDateTime.now()).toDays();
        base *= Math.pow(0.95, daysSinceCreated);

        return base;
    }

    private double calculatePopularScore(Project p, long applicantCount) {
        double base = applicantCount * 10.0
                + p.getAuthor().getRatingAvg() * 20.0
                + p.getViewCount() * 1.0;

        long daysToDeadline = java.time.Duration.between(
                java.time.LocalDateTime.now(), p.getRecruitmentDeadline()).toDays();
        if (daysToDeadline <= 3) base *= 1.5;
        else if (daysToDeadline <= 7) base *= 1.2;

        long daysSinceCreated = java.time.Duration.between(
                p.getCreatedAt(), java.time.LocalDateTime.now()).toDays();
        base *= Math.pow(0.95, daysSinceCreated);

        return base;
    }
}
