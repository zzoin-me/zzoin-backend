package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.community.*;
import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.PostQueryService;
import com.hicct3.projectfinder.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostQueryService postQueryService;

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ApiResponse<PageResponseDTO<PostPreviewResponseDTO>> getPosts(
            @RequestParam(defaultValue = "ALL") String board,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) String keyword,
            Authentication authentication,
            Pageable pageable
    ) {
        PostBoardType boardType = PostBoardType.from(board);
        PostSortType sortType = PostSortType.from(sort);
        Long currentUserId = extractUserId(authentication);
        return ApiResponse.onSuccess(PageResponseDTO.from(
                postQueryService.getPostList(boardType, sortType, keyword, currentUserId, pageable)));
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponseDTO> getPostDetail(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        return ApiResponse.onSuccess("게시글 상세 조회 성공했습니다.", postQueryService.getPostDetail(postId, currentUserId));
    }

    @Operation(summary = "게시글 조회수 기록")
    @PostMapping("/{postId}/view")
    public ApiResponse<ViewCountResultDTO> recordPostView(
            @PathVariable Long postId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        return ApiResponse.onSuccess(
                postQueryService.recordView(postId, currentUserId, viewerId));
    }

    @Operation(summary = "게시글 생성")
    @PostMapping
    public ApiResponse<Long> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequestDTO req
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        Long postId = postService.createPost(userId, req);
        return ApiResponse.onSuccess("게시글 등록에 성공했습니다.", postId);
    }

    @Operation(summary = "게시글 수정")
    @PatchMapping("/{postId}")
    public ApiResponse<Void> updatePost(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequestDTO req
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        postService.updatePost(userId, postId, req);
        return ApiResponse.onSuccess("게시글 수정에 성공했습니다.", null);
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        postService.deletePost(userId, postId);
        return ApiResponse.onSuccess("게시글 삭제에 성공했습니다.", null);
    }

    @Operation(summary = "게시글 좋아요 토글")
    @PostMapping("/{postId}/like")
    public ApiResponse<ToggleResultDTO> toggleLike(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ApiResponse.onSuccess(postService.toggleLike(userId, postId));
    }

    @Operation(summary = "게시글 저장 토글")
    @PostMapping("/{postId}/save")
    public ApiResponse<ToggleResultDTO> toggleSave(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ApiResponse.onSuccess(postService.toggleSave(userId, postId));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}
