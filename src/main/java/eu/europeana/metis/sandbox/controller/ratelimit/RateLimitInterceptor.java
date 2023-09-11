package eu.europeana.metis.sandbox.controller.ratelimit;


import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.TryConsumeAndReturnRemainingTokensCommand;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;

/**
 * Implementation of Rate Limit interceptor to intercept the requests
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Integer capacity;
    private final PostgreSQLadvisoryLockBasedProxyManager postgreSQLManager;
    private final BucketConfiguration bucketConfiguration;

    /**
     * Constructor
     * @param capacity The max number of tokens per user
     * @param time The time it takes to refresh the tokens per uset
     * @param postgreSQLManager The database manager of the tokens per user
     */
    public RateLimitInterceptor(Integer capacity, Long time, PostgreSQLadvisoryLockBasedProxyManager postgreSQLManager){
        this.capacity = capacity;
        this.postgreSQLManager = postgreSQLManager;
        bucketConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(time))))
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        final Long key = (long) request.getRemoteAddr().hashCode();
        final ConsumptionProbe probe = resolveBucket(key);
        response.addHeader("X-Rate-Limit-Limit", String.valueOf(capacity));
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            final long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(waitForRefill));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "You have exhausted your API Request Quota");
            return false;

        }
    }

    private ConsumptionProbe resolveBucket(Long apiKey) {
        final Request<ConsumptionProbe> request = new Request<>(new TryConsumeAndReturnRemainingTokensCommand(1), null, null);
        final CommandResult<ConsumptionProbe> commandResult = postgreSQLManager.execute(apiKey, request);
        if(commandResult.isBucketNotFound()){
            Bucket bucket = postgreSQLManager.builder().build(apiKey, bucketConfiguration);
            return bucket.tryConsumeAndReturnRemaining(1);
        } else {
            return commandResult.getData();
        }
    }

}
