package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.QProject;
import com.hicct3.projectfinder.entity.QProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.SortType;
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
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Project> searchProjects(
            SortType sortType,
            String keyword,
            String field,
            Integer maxDays,
            Integer minCount,
            Integer maxCount,
            Pageable pageable
    ) {
        QProject project = QProject.project;

        BooleanExpression where = project.deletedAt.isNull();

        where = where.and(keywordContains(keyword));
        where = where.and(maxDaysContains(maxDays));
        where = where.and(fieldContains(field));
        where = where.and(countBetween(minCount, maxCount, field));

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

    private BooleanExpression fieldContains(String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        return project.id.in(
                JPAExpressions
                        .select(recruitment.project.id)
                        .from(recruitment)
                        .where(recruitment.name.eq(field)
                                .and(recruitment.deletedAt.isNull()))
        );
    }

    private BooleanExpression countBetween(Integer minCount, Integer maxCount, String field) {
        if (minCount == null && maxCount == null) {
            return null;
        }

        QProject project = QProject.project;
        QProjectRecruitment recruitment = QProjectRecruitment.projectRecruitment;

        BooleanExpression conditions = recruitment.project.id.eq(project.id)
                .and(recruitment.deletedAt.isNull());

        if (field != null && !field.isBlank()) {
            conditions = conditions.and(recruitment.name.eq(field));
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
        return project.createdAt.desc();
    }
}
