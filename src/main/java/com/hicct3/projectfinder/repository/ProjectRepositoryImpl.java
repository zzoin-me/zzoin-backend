package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.QProject;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Project> searchProjects(
            SortType sortType,
            String keyword,
            Pageable pageable
    ) {

        QProject project = QProject.project;
        List<Project> content = queryFactory
                .selectFrom(project)
                .where(
                        project.deletedAt.isNull(),
                        keywordContains(keyword)
                )
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
                .where(
                        project.deletedAt.isNull(),
                        keywordContains(keyword)
                )
                .fetchOne();


        return new PageImpl<>(
                content,
                pageable,
                total
        );
    }

    private BooleanExpression keywordContains(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        QProject project = QProject.project;

        return project.title.contains(keyword)
                .or(project.description.contains(keyword));
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

    private OrderSpecifier<?> sortOrder(
            QProject project,
            SortType sortType
    ) {

        /*if (sortType == SortType.POPULAR) {
            return project.viewCount.desc();
        }*/

        return project.createdAt.desc();
    }
}