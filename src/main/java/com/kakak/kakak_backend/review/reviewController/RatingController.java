package com.kakak.kakak_backend.review.reviewController;

import com.kakak.kakak_backend.review.reviewDTO.RatingResponse;
import com.kakak.kakak_backend.review.reviewDTO.SubmitRatingRequest;
import com.kakak.kakak_backend.review.reviewService.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse submitRating(
            @RequestBody SubmitRatingRequest request) {

        return ratingService.submitRating(request);
    }

    @GetMapping("/user/{id}")
    public List<RatingResponse> getRatingsForUser(
            @PathVariable UUID id) {

        return ratingService.getRatingsForUser(id);
    }
}