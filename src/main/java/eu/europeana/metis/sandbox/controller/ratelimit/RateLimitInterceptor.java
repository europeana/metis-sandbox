package eu.europeana.metis.sandbox.controller.ratelimit;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.remote.commands.TryConsumeAndReturnRemainingTokensCommand;
import io.github.bucket4j.postgresql.PostgreSQLadvisoryLockBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Implementation of Rate Limit interceptor to intercept the requests
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int CONVERSION_FACTOR_FROM_NANOS_TO_SECONDS = 1_000_000_000;
    public static final String X_RATE_LIMIT_LIMIT = "X-Rate-Limit-Limit";
    public static final String X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    public static final String X_RATE_LIMIT_RESET = "X-Rate-Limit-Reset";

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
        final Long key = generateUniqueIdFromIpAddress(request.getRemoteAddr());
        final ConsumptionProbe probe = resolveBucket(key);
        response.addHeader(X_RATE_LIMIT_LIMIT, String.valueOf(capacity));
        if (probe.isConsumed()) {
            final long waitForRefill = probe.getNanosToWaitForReset() / CONVERSION_FACTOR_FROM_NANOS_TO_SECONDS;
            response.addHeader(X_RATE_LIMIT_RESET, String.valueOf(waitForRefill));
            response.addHeader(X_RATE_LIMIT_REMAINING, String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            final long waitForRefill = probe.getNanosToWaitForRefill() / CONVERSION_FACTOR_FROM_NANOS_TO_SECONDS;
            response.addHeader(X_RATE_LIMIT_RESET, String.valueOf(waitForRefill));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "You have exhausted your API Request Quota");
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

    // Although collision can still happen, this approach provides higher chances of uniqueness than simply using hashCode()
    private Long generateUniqueIdFromIpAddress(String ipAddress){
        // Generate a UUID based on the ip address's bytes
        UUID uuid = UUID.nameUUIDFromBytes(ipAddress.getBytes());
        // Convert the UUID to a Long value.
        // UUID is a 128 bit value, while a Long is a 64 bit value.
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();

        // We take the last 32 bits of the most significant bits and shift them to the left side.
        // Then, we take the last 32 bits of the least significant bits and keep them on the right side.
        // The result is a combination of both, with all 64 bits of the long populated.
        // Math.abs() is to guarantee for the value to always be positive
        final int numberOfBitsToShift = 32;
        return Math.abs((mostSignificantBits << numberOfBitsToShift) | (leastSignificantBits & 0xFFFFFFFFL));
    }

}
