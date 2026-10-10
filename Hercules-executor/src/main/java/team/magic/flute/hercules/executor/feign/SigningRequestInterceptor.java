package team.magic.flute.hercules.executor.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import team.magic.flute.hercules.common.util.ExecutorInfoUtils;
import team.magic.flute.hercules.executor.config.RunnerEnv;

import java.util.Collection;
import java.util.Map;

/**
 * Adds the {@code X-Hercules-Timestamp} and {@code X-Hercules-Signature}
 * headers to every signed executor→manager request, using the op and subject
 * the caller already placed on the template via {@code @HeaderMap}
 * ({@link ExecutorInfoUtils#signingHeaders}).
 *
 * <p>The canonical message is built from the template's own queries and the
 * SHA-256 of its body, so signer and server derive the same fields from the
 * same single source of truth per side (ADR-0016). Endpoints that do not
 * carry the op/subject headers (heartbeat, plugin lookup, status check)
 * are left untouched — signing is opt-in per call.
 */
@Slf4j
public class SigningRequestInterceptor implements RequestInterceptor {

    @Setter
    private RunnerEnv runnerEnv;

    public SigningRequestInterceptor() {}

    public SigningRequestInterceptor(RunnerEnv runnerEnv) {
        this.runnerEnv = runnerEnv;
    }

    @Override
    public void apply(RequestTemplate template) {
        RunnerEnv env = this.runnerEnv;
        if (env == null) {
            // Not wired — nothing to sign. Defensive: never break the request.
            return;
        }
        String op = firstHeader(template, ExecutorInfoUtils.HEADER_OP);
        String subject = firstHeader(template, ExecutorInfoUtils.HEADER_SUBJECT);
        if (op == null || op.isEmpty() || subject == null || subject.isEmpty()) {
            // Not a signed endpoint; leave the request alone.
            return;
        }
        String identityId = env.getRunnerIdentityId();
        if (identityId == null || identityId.isEmpty()) {
            log.warn("[SIGN] Runner identity id not set; skipping signature for op [{}] subject [{}].", op, subject);
            return;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String canonicalQuery = ExecutorInfoUtils.canonicalQuery(template.queries());
        String bodySha = ExecutorInfoUtils.bodySha(template.body());

        String signature = ExecutorInfoUtils.signRequest(
                identityId, op, subject, timestamp, canonicalQuery, bodySha);

        template.header(ExecutorInfoUtils.HEADER_TIMESTAMP, timestamp);
        template.header(ExecutorInfoUtils.HEADER_SIGNATURE, signature);
    }

    /**
     * Case-insensitive single-value lookup, so the interceptor survives any
     * header-name normalization feign applies between the caller's
     * {@code @HeaderMap} and {@link RequestTemplate#headers()}.
     */
    private static String firstHeader(RequestTemplate template, String name) {
        Map<String, Collection<String>> headers = template.headers();
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Collection<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                Collection<String> values = e.getValue();
                if (values == null) {
                    return null;
                }
                return values.stream().findFirst().orElse(null);
            }
        }
        return null;
    }

}
