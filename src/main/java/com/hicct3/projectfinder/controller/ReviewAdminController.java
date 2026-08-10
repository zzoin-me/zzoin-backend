package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.project.review.UpdateReviewHiddenRequestDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class ReviewAdminController {
    private final ReviewService reviewService;

    @PatchMapping("/{reviewId}/hidden")
    public ApiResponse<Void> updateHidden(
            @PathVariable Long reviewId,
            @RequestBody @Valid UpdateReviewHiddenRequestDTO request
    ) {
        reviewService.updateHidden(reviewId, request.getHidden());
        return ApiResponse.onSuccess("리뷰 숨김 상태를 변경했습니다.", null);
    }
}
