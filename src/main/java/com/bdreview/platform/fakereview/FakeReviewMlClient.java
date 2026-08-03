package com.bdreview.platform.fakereview;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * REST client to the Python ML microservice's `/fake-review/analyze` endpoint
 * (see /ml-service/app/fake_review). This class only talks HTTP — persisting
 * the result (Review.suspicionScore/visibilityStatus, FakeReviewSignal rows)
 * is the caller's job, via ReviewRepository/FakeReviewSignalRepository.
 */
@Component
public class FakeReviewMlClient {

    private final WebClient webClient;

    public FakeReviewMlClient(WebClient mlServiceWebClient) {
        this.webClient = mlServiceWebClient;
    }

    public Mono<FakeReviewAnalysisResponseDto> analyze(FakeReviewAnalysisRequestDto request) {
        return webClient.post()
                .uri("/fake-review/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FakeReviewAnalysisResponseDto.class);
    }

    /** Blocking convenience wrapper for call sites that aren't already reactive. */
    public FakeReviewAnalysisResponseDto analyzeBlocking(FakeReviewAnalysisRequestDto request) {
        return analyze(request).block();
    }
}
