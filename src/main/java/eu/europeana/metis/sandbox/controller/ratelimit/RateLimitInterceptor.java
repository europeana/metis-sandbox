package eu.europeana.metis.sandbox.controller.ratelimit;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final Integer capacity;
    private final Long time;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public RateLimitInterceptor(Integer capacity, Long time){
        this.capacity = capacity;
        this.time = time;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        request.getRemoteAddr();

//        Bucket tokenBucket = Bucket.builder()
//                // 20 requests per hour per API client
//                .addLimit(Bandwidth.classic(capacity, Refill.intervally(tokens, Duration.ofSeconds(time))))
//                .build();
        Bucket tokenBucket = resolveBucket(request.getRemoteAddr());
        response.addHeader("X-Rate-Limit-Limit", String.valueOf(capacity));
        ConsumptionProbe probe = tokenBucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(waitForRefill));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "You have exhausted your API Request Quota");
            return false;

        }
    }

    private Bucket resolveBucket(String apiKey) {
        return cache.computeIfAbsent(apiKey, this::newBucket);
    }

    private Bucket newBucket(String apiKey) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(time))))
                .build();
    }

}
