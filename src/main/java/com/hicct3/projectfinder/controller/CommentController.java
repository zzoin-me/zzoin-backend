package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.community.CommentResponseDTO;
import com.hicct3.projectfinder.dto.community.CreateCommentRequestDTO;
import com.hicct3.projectfinder.dto.community.UpdateCommentRequestDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "게시글 댓글 목록 조회")
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponseDTO>> getComments(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        return ApiResponse.onSuccess(commentService.getComments(postId, currentUserId));
    }

    @Operation(summary = "댓글 생성")
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponseDTO> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequestDTO req
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return ApiResponse.onSuccess(commentService.createComment(userId, postId, req));
    }

    @Operation(summary = "댓글 수정")
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<Void> updateComment(
            Authentication authentication,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequestDTO req
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        commentService.updateComment(userId, commentId, req.getContent());
        return ApiResponse.onSuccess("댓글 수정에 성공했습니다.", null);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            Authentication authentication,
            @PathVariable Long commentId
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        commentService.deleteComment(userId, commentId);
        return ApiResponse.onSuccess("댓글 삭제에 성공했습니다.", null);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}
