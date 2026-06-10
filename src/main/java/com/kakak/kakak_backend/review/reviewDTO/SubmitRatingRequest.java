package com.kakak.kakak_backend.review.reviewDTO;

import lombok.Data;

import java.util.UUID;

@Data
public class SubmitRatingRequest {

    private UUID jobId;

    private UUID targetUserId;

    private Integer rating;

    private String reviewText;
}