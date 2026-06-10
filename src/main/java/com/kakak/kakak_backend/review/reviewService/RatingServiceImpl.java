package com.kakak.kakak_backend.review.reviewService;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import com.kakak.kakak_backend.jobs.jobentity.Job;
import com.kakak.kakak_backend.jobs.jobrepository.JobRepo;
import com.kakak.kakak_backend.review.reviewDTO.RatingResponse;
import com.kakak.kakak_backend.review.reviewDTO.SubmitRatingRequest;
import com.kakak.kakak_backend.review.reviewEntity.Review;
import com.kakak.kakak_backend.review.reviewRepository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final ReviewRepository reviewRepository;

    private final JobRepo jobRepo;

    private final UsersRepo usersRepo;

    @Override
    public RatingResponse submitRating(SubmitRatingRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        AuthUsers reviewer = usersRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "User not found"));

        Job job = jobRepo.findById(request.getJobId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Job not found"));

        AuthUsers targetUser = usersRepo.findById(request.getTargetUserId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Target user not found"));

        reviewRepository.findByJob_IdAndReviewer_Id(
                        request.getJobId(),
                        reviewer.getId())
                .ifPresent(review -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Duplicate rating detected");
                });

        Review review = Review.builder()
                .job(job)
                .reviewer(reviewer)
                .targetUser(targetUser)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .build();

        Review savedReview = reviewRepository.save(review);

        return mapToResponse(savedReview);
    }

    @Override
    public List<RatingResponse> getRatingsForUser(UUID userId) {

        return reviewRepository.findByTargetUser_Id(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private RatingResponse mapToResponse(Review review) {

        RatingResponse response = new RatingResponse();

        response.setId(review.getId());

        response.setJobId(review.getJob().getId());

        response.setReviewerId(review.getReviewer().getId());

        response.setTargetUserId(review.getTargetUser().getId());

        response.setRating(review.getRating());

        response.setReviewText(review.getReviewText());

        response.setCreatedAt(review.getCreatedAt());

        return response;
    }
}