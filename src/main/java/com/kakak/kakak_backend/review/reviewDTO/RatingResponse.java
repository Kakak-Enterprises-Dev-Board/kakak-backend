package com.kakak.kakak_backend.review.reviewDTO;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@JsonPropertyOrder({
        "id",
        "jobId",
        "reviewerId",
        "targetUserId",
        "rating",
        "reviewText",
        "createdAt"
})
public class RatingResponse {

    private UUID id;

    private UUID jobId;

    private UUID reviewerId;

    private UUID targetUserId;

    private Integer rating;

    private String reviewText;

    private Timestamp createdAt;
}