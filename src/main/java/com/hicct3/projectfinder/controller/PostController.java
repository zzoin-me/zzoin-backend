package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.dto.community.CreatePostRequestDTO;
import com.hicct3.projectfinder.dto.community.PostDetailResponseDTO;
import com.hicct3.projectfinder.dto.community.PostImageUploadResponseDTO;
import com.hicct3.projectfinder.dto.community.PostPreviewResponseDTO;
import com.hicct3.projectfinder.dto.community.ToggleResultDTO;
import com.hicct3.projectfinder.dto.community.UpdatePostRequestDTO;
import com.hicct3.projectfinder.dto.community.ViewCountResultDTO;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.PostQueryService;
import com.hicct3.projectfinder.service.PostService;
import com.hicct3.projectfinder.service.R2ImageStorageService;
import com.hicct3.projectfinder.service.SecurityRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostQueryService postQueryService;
    private final R2ImageStorageService r2ImageStorageService;
    private final SecurityRateLimitService rateLimitService;

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
            HttpServletRequest request,
            @PathVariable Long postId,
            @RequestHeader(value = "X-Viewer-Id", required = false) String viewerId,
            Authentication authentication
    ) {
        rateLimitService.consume(
                "post-view", clientIp(request), 120, Duration.ofMinutes(1));
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

    @Operation(summary = "게시글 이미지 업로드")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostImageUploadResponseDTO> uploadPostImages(
            Authentication authentication,
            @RequestPart("images") List<MultipartFile> images
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        List<String> imageUrls = r2ImageStorageService.uploadPostImages(userId, images);
        return ApiResponse.onSuccess("게시글 이미지 업로드에 성공했습니다.", PostImageUploadResponseDTO.of(imageUrls));
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

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
