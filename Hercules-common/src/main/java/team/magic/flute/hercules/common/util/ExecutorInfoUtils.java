package team.magic.flute.hercules.common.util;

import team.magic.flute.hercules.common.global.ExecutorTaskOps;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Executor operation signing utilities.
 *
 * <p>Every mutating executor→manager operation (and the task fetch) is signed
 * with HMAC-SHA256 over a canonical message that covers the operation, subject,
 * timestamp, decoded query string and the SHA-256 of the raw wire body. The
 * signature, timestamp, op and subject travel in four {@code X-Hercules-*}
 * headers; the signing key is the executor's {@code identityId} (ADR-0016).
 *
 * <p>The canonical message is the five fields below joined with {@code "\n"}:
 * <pre>
 *   OP + "\n" + SUBJECT + "\n" + TIMESTAMP + "\n"
 *       + CANONICAL_QUERY + "\n" + SHA256_HEX(BODY)
 * </pre>
 * Client and server MUST build this byte-for-byte identically.
 */
public class ExecutorInfoUtils {

    public static final String HEADER_OP = "X-Hercules-Op";
    public static final String HEADER_SUBJECT = "X-Hercules-Subject";
    public static final String HEADER_TIMESTAMP = "X-Hercules-Timestamp";
    public static final String HEADER_SIGNATURE = "X-Hercules-Signature";

    /** Replay window: |now - ts| &lt;= this is fresh. 5 minutes. */
    public static final long FRESHNESS_WINDOW_MS = 300_000L;

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private ExecutorInfoUtils() {}

    /**
     * SHA-256 of the raw wire body, lowercase hexadecimal. Used both as the
     * body digest inside the canonical message and nowhere else — the HMAC
     * itself is over the canonical message string, not the body directly.
     *
     * @param body raw bytes; null/empty yields the empty digest of nothing
     *             (which the protocol represents as the literal "" — see
     *             {@link #bodySha(byte[])})
     */
    public static String sha256Hex(byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest(body == null ? new byte[0] : body));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 computation failed", e);
        }
    }

    /**
     * Body digest for the canonical message: empty string when there is no
     * body (GET / no-content PUT), otherwise {@link #sha256Hex(byte[])}.
     */
    public static String bodySha(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        return sha256Hex(body);
    }

    /**
     * Canonical query string over <em>decoded</em> parameter values. Entries
     * are sorted by parameter name ascending; within the same name, values are
     * sorted by string ascending. Each entry is emitted as {@code name=value}
     * and joined with {@code &amp;}. No percent-encoding is applied — the
     * inputs are already decoded, so re-encoding would diverge between signer
     * and verifier. An empty parameter set yields "".
     *
     * @param queries decoded parameters; on the client from
     *                {@code RequestTemplate.queries()} (a
     *                {@code Map<String, Collection<String>>}), on the server
     *                from {@code HttpServletRequest.getParameterMap()} (a
     *                {@code Map<String, String[]>}). Both shapes are accepted.
     */
    public static String canonicalQuery(Map<String, ?> queries) {
        if (queries == null || queries.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>(queries.keySet());
        Collections.sort(names);
        List<String> pairs = new ArrayList<>();
        for (String name : names) {
            List<String> sortedVals = toStringList(queries.get(name));
            if (sortedVals.isEmpty()) {
                continue;
            }
            Collections.sort(sortedVals);
            for (String v : sortedVals) {
                pairs.add(name + "=" + v);
            }
        }
        return String.join("&", pairs);
    }

    /**
     * Adapt the value of a parameter map entry — which may be a
     * {@code Collection<String>} (feign), a {@code String[]} (servlet), a
     * single {@code String}, or null — into a flat list of strings. The server
     * side never sees the same collection type as the feign client, so this is
     * the single point where both shapes converge on the canonical
     * representation.
     */
    private static List<String> toStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object o : (Collection<?>) value) {
                out.add(o == null ? "" : o.toString());
            }
        } else if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                Object o = java.lang.reflect.Array.get(value, i);
                out.add(o == null ? "" : o.toString());
            }
        } else {
            out.add(value.toString());
        }
        return out;
    }

    /**
     * Assemble the canonical message string. Single definition shared by
     * {@link #signRequest} and {@link #verifyRequest} so signer and verifier
     * cannot drift.
     */
    public static String buildCanonicalMessage(String op,
                                                String subject,
                                                String timestamp,
                                                String canonicalQuery,
                                                String bodySha) {
        return op + "\n"
                + subject + "\n"
                + timestamp + "\n"
                + canonicalQuery + "\n"
                + bodySha;
    }

    /**
     * The two headers the caller sets on every signed request (op + subject).
     * The {@link ExecutorInfoUtils#HEADER_TIMESTAMP timestamp} and
     * {@link ExecutorInfoUtils#HEADER_SIGNATURE signature} are added by the
     * signing interceptor, not the caller, because the timestamp must be fresh
     * at send time and the signature depends on it.
     */
    public static Map<String, String> signingHeaders(ExecutorTaskOps op, String subject) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_OP, op.name());
        headers.put(HEADER_SUBJECT, subject);
        return headers;
    }

    /**
     * Compute the request signature (lowercase hex HMAC-SHA256) over the
     * canonical message. The signing key is the executor's
     * {@code identityId}.
     */
    public static String signRequest(String identityId,
                                     String op,
                                     String subject,
                                     String timestamp,
                                     String canonicalQuery,
                                     String bodySha) {
        String msg = buildCanonicalMessage(op, subject, timestamp, canonicalQuery, bodySha);
        return HmacUtils.hmacSha256Hex(identityId, msg);
    }

    /**
     * Verify a presented signature against the canonical message, in constant
     * time. A null/blank presented signature is rejected without computing.
     */
    public static boolean verifyRequest(String identityId,
                                        String op,
                                        String subject,
                                        String timestamp,
                                        String canonicalQuery,
                                        String bodySha,
                                        String presentedSign) {
        if (presentedSign == null || presentedSign.isEmpty()) {
            return false;
        }
        String msg = buildCanonicalMessage(op, subject, timestamp, canonicalQuery, bodySha);
        return HmacUtils.verify(identityId, msg, presentedSign);
    }

    /**
     * @param ts     epoch-millis string from the {@code X-Hercules-Timestamp} header
     * @param nowMs  the server's current epoch millis
     * @return true iff ts parses as a long and |now - ts| &lt;=
     *         {@link #FRESHNESS_WINDOW_MS}
     */
    public static boolean isFreshTimestamp(String ts, long nowMs) {
        if (ts == null || ts.isEmpty()) {
            return false;
        }
        long parsed;
        try {
            parsed = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return false;
        }
        long delta = nowMs - parsed;
        long abs = delta < 0 ? -delta : delta;
        return abs <= FRESHNESS_WINDOW_MS;
    }

    /**
     * Build the canonical signed subject for a batch operation.
     *
     * <p>The ids are sorted before joining, so the signature depends on the id
     * <em>set</em>, not the order — signer and verifier may collect the ids in
     * any order and still produce the same subject.
     *
     * @param ids task ids to sign; nulls are dropped
     * @return the comma-joined, sorted id list
     * @throws IllegalArgumentException if ids is null or contains no non-null element
     */
    public static String buildSignSubject(Collection<String> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("[CAN NOT BUILD SIGN]:ids is null");
        }
        String joined = ids.stream().filter(Objects::nonNull).sorted().collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            throw new IllegalArgumentException("[CAN NOT BUILD SIGN]:ids is empty");
        }
        return joined;
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }

}
