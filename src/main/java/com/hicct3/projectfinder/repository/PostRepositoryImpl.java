package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Comment;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.QComment;
import com.hicct3.projectfinder.entity.QPost;
import com.hicct3.projectfinder.entity.QPostLike;
import com.hicct3.projectfinder.entity.QPostSave;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> searchPosts(
            PostBoardType board,
            PostSortType sortType,
            String keyword,
            Long currentUserId,
            Pageable pageable
    ) {
        QPost post = QPost.post;
        QComment comment = QComment.comment;
        QPostLike postLike = QPostLike.postLike;
        QPostSave postSave = QPostSave.postSave;

        BooleanExpression where = post.deletedAt.isNull();

        where = where.and(keywordContains(keyword, post));

        if (board == PostBoardType.MINE && currentUserId != null) {
            where = where.and(post.author.userId.eq(currentUserId));
        } else if (board == PostBoardType.COMMENTS && currentUserId != null) {
            where = where.and(
                    post.id.in(
                            JPAExpressions
                                    .select(comment.post.id)
                                    .from(comment)
                                    .where(comment.author.userId.eq(currentUserId)
                                            .and(comment.deletedAt.isNull()))
                    )
            );
        } else if (board == PostBoardType.LIKES && currentUserId != null) {
            where = where.and(
                    post.id.in(
                            JPAExpressions
                                    .select(postLike.post.id)
                                    .from(postLike)
                                    .where(postLike.user.userId.eq(currentUserId))
                    )
            );
        } else if (board == PostBoardType.SAVED && currentUserId != null) {
            where = where.and(
                    post.id.in(
                            JPAExpressions
                                    .select(postSave.post.id)
                                    .from(postSave)
                                    .where(postSave.user.userId.eq(currentUserId))
                    )
            );
        }

        List<Post> content = queryFactory
                .selectFrom(post)
                .where(where)
                .orderBy(sortOrder(post, sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(post.count())
                .from(post)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public long countLikesByPostId(Long postId) {
        QPostLike postLike = QPostLike.postLike;
        Long count = queryFactory
                .select(postLike.count())
                .from(postLike)
                .where(postLike.post.id.eq(postId))
                .fetchOne();
        return count != null ? count : 0;
    }

    @Override
    public long countCommentsByPostId(Long postId) {
        QComment comment = QComment.comment;
        Long count = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId)
                        .and(comment.deletedAt.isNull()))
                .fetchOne();
        return count != null ? count : 0;
    }

    @Override
    public Map<Long, Long> countLikesByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QPostLike postLike = QPostLike.postLike;
        List<Tuple> rows = queryFactory
                .select(postLike.post.id, postLike.count())
                .from(postLike)
                .where(postLike.post.id.in(postIds))
                .groupBy(postLike.post.id)
                .fetch();
        Map<Long, Long> result = new HashMap<>();
        for (Tuple row : rows) {
            Long postId = row.get(0, Long.class);
            Long count = row.get(1, Long.class);
            result.put(postId, count != null ? count : 0L);
        }
        return result;
    }

    @Override
    public Map<Long, Long> countCommentsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QComment comment = QComment.comment;
        List<Tuple> rows = queryFactory
                .select(comment.post.id, comment.count())
                .from(comment)
                .where(comment.post.id.in(postIds)
                        .and(comment.deletedAt.isNull()))
                .groupBy(comment.post.id)
                .fetch();
        Map<Long, Long> result = new HashMap<>();
        for (Tuple row : rows) {
            Long postId = row.get(0, Long.class);
            Long count = row.get(1, Long.class);
            result.put(postId, count != null ? count : 0L);
        }
        return result;
    }

    @Override
    public List<Long> findLikedPostIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        QPostLike postLike = QPostLike.postLike;
        return queryFactory
                .select(postLike.post.id)
                .from(postLike)
                .where(postLike.user.userId.eq(userId)
                        .and(postLike.post.id.in(postIds)))
                .fetch();
    }

    @Override
    public List<Long> findSavedPostIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }
        QPostSave postSave = QPostSave.postSave;
        return queryFactory
                .select(postSave.post.id)
                .from(postSave)
                .where(postSave.user.userId.eq(userId)
                        .and(postSave.post.id.in(postIds)))
                .fetch();
    }

    private BooleanExpression keywordContains(String keyword, QPost post) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return post.title.contains(keyword)
                .or(post.content.contains(keyword));
    }

    private OrderSpecifier<?> sortOrder(QPost post, PostSortType sortType) {
        if (sortType == PostSortType.POPULAR) {
            return post.viewCount.desc();
        }
        return post.createdAt.desc();
    }
}
