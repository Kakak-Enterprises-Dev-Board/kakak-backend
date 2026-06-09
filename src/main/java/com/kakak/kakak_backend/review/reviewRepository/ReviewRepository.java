package com.kakak.kakak_backend.review.reviewRepository;

import com.kakak.kakak_backend.review.reviewEntity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByTargetUser_Id(UUID targetUserId);

    Optional<Review> findByJob_IdAndReviewer_Id(
            UUID jobId,
            UUID reviewerId
    );
}