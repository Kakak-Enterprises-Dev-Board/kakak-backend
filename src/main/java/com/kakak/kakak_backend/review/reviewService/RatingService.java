package com.kakak.kakak_backend.review.reviewService;

import com.kakak.kakak_backend.review.reviewDTO.RatingResponse;
import com.kakak.kakak_backend.review.reviewDTO.SubmitRatingRequest;

import java.util.List;
import java.util.UUID;

public interface RatingService {

    RatingResponse submitRating(SubmitRatingRequest request);

    List<RatingResponse> getRatingsForUser(UUID userId);
}