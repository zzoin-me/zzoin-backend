package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.QProject;
import com.hicct3.projectfinder.entity.QProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
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
            List<RecruitmentCategory> categories,
            List<String> names,
            Integer maxDays,
            Integer minCount,
            Integer maxCount,
            List<GoalType> goals,
            Boolean recruitingOnly,
            Pageable pageable
    ) {
        QProject project = QProject.project;

        BooleanExpression where = project.deletedAt.isNull();

        where = where.and(keywordContains(keyword));
        where = where.and(maxDaysContains(maxDays));
        where = where.and(recruitmentContains(categories, names));
        where = where.and(countBetween(minCount, maxCount, categories, names));
        where = where.and(goalContains(goals));
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

    private BooleanExpression recruitmentContains(List<RecruitmentCategory> categories, List<String> names) {
        boolean hasCategory = categories != null && !categories.isEmpty();
        boolean hasName = names != null && !names.isEmpty();
        if (!hasCategory && !hasName) {
            return null;
        }
        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression cond = recruitment.deletedAt.isNull();
        if (hasCategory) {
            cond = cond.and(recruitment.category.in(categories));
        }
        if (hasName) {
            cond = cond.and(recruitment.name.in(names));
        }

        return project.id.in(
                JPAExpressions
                        .select(recruitment.project.id)
                        .from(recruitment)
                        .where(cond)
        );
    }

    private BooleanExpression countBetween(Integer minCount, Integer maxCount, List<RecruitmentCategory> categories, List<String> names) {
        if (minCount == null && maxCount == null) {
            return null;
        }

        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression conditions = recruitment.project.id.eq(project.id)
                .and(recruitment.deletedAt.isNull());

        if (categories != null && !categories.isEmpty()) {
            conditions = conditions.and(recruitment.category.in(categories));
        }
        if (names != null && !names.isEmpty()) {
            conditions = conditions.and(recruitment.name.in(names));
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

    private BooleanExpression goalContains(List<GoalType> goals) {
        if (goals == null || goals.isEmpty()) {
            return null;
        }
        return QProject.project.goal.in(goals);
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
        return project.createdAt.desc();
    }
}
