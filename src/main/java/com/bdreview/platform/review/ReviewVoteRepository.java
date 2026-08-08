package com.bdreview.platform.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, UUID> {

    Optional<ReviewVote> findByReviewIdAndUserIdAndVoteType(UUID reviewId, UUID userId, VoteType voteType);

    void deleteByReviewIdAndUserIdAndVoteType(UUID reviewId, UUID userId, VoteType voteType);

    boolean existsByReviewIdAndUserIdAndVoteType(UUID reviewId, UUID userId, VoteType voteType);
}
