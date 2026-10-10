package team.magic.flute.hercules.executor.feign;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import team.magic.flute.hercules.common.util.ExecutorInfoUtils;
import team.magic.flute.hercules.executor.config.RunnerEnv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline unit tests for {@link SigningRequestInterceptor}. Builds a real
 * {@link RequestTemplate}, injects a fixed identity via {@link RunnerEnv},
 * applies the interceptor, and asserts that the four signing headers are
 * present and that the signature verifies against the canonical message
 * rebuilt by hand. No Spring context.
 */
class SigningRequestInterceptorTest {

    private static final String IDENTITY = "EXECUTOR_IDENTITY_OFFLINE_TEST";

    private SigningRequestInterceptor interceptor;
    private RunnerEnv runnerEnv;

    @BeforeEach
    void setUp() {
        runnerEnv = new RunnerEnv();
        ReflectionTestUtils.setField(runnerEnv, "runnerIdentityId", IDENTITY);
        interceptor = new SigningRequestInterceptor(runnerEnv);
    }

    @Test
    void applyInjectsAllFourHeaders() {
        RequestTemplate template = newTemplate("GET", "/taskDispatch/tryFetchTasksWithByteArray");
        template.header(ExecutorInfoUtils.HEADER_OP, "FETCH");
        template.header(ExecutorInfoUtils.HEADER_SUBJECT, "exec-1");
        // a query param, decoded
        template.query("executorId", "exec-1");
        template.query("fetchLimit", "10");

        interceptor.apply(template);

        assertEquals("FETCH", firstHeader(template, ExecutorInfoUtils.HEADER_OP));
        assertEquals("exec-1", firstHeader(template, ExecutorInfoUtils.HEADER_SUBJECT));
        String ts = firstHeader(template, ExecutorInfoUtils.HEADER_TIMESTAMP);
        String sig = firstHeader(template, ExecutorInfoUtils.HEADER_SIGNATURE);
        assertNotNull(ts);
        assertNotNull(sig);
        // timestamp is a parseable long
        Long.parseLong(ts);
    }

    @Test
    void signatureVerifiesAgainstHandBuiltCanonical() {
        RequestTemplate template = newTemplate("GET", "/taskDispatch/tryFetchTasksWithByteArray");
        template.header(ExecutorInfoUtils.HEADER_OP, "FETCH");
        template.header(ExecutorInfoUtils.HEADER_SUBJECT, "exec-1");
        template.query("executorId", "exec-1");
        template.query("fetchLimit", "10");

        interceptor.apply(template);

        String op = firstHeader(template, ExecutorInfoUtils.HEADER_OP);
        String subject = firstHeader(template, ExecutorInfoUtils.HEADER_SUBJECT);
        String ts = firstHeader(template, ExecutorInfoUtils.HEADER_TIMESTAMP);
        String sig = firstHeader(template, ExecutorInfoUtils.HEADER_SIGNATURE);

        // Rebuild canonical exactly as the server would, from the same fields.
        String cq = ExecutorInfoUtils.canonicalQuery(template.queries());
        String bodySha = ExecutorInfoUtils.bodySha(template.body());
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, op, subject, ts, cq, bodySha, sig));
    }

    @Test
    void differentBodyProducesDifferentSignature() {
        // Two templates identical except for the body — signatures must differ.
        RequestTemplate t1 = newTemplate("PUT", "/taskDispatch/finishOneTask");
        t1.header(ExecutorInfoUtils.HEADER_OP, "FINISH");
        t1.header(ExecutorInfoUtils.HEADER_SUBJECT, "task-1");
        t1.body("body-A");

        RequestTemplate t2 = newTemplate("PUT", "/taskDispatch/finishOneTask");
        t2.header(ExecutorInfoUtils.HEADER_OP, "FINISH");
        t2.header(ExecutorInfoUtils.HEADER_SUBJECT, "task-1");
        t2.body("body-B");

        interceptor.apply(t1);
        interceptor.apply(t2);

        String sig1 = firstHeader(t1, ExecutorInfoUtils.HEADER_SIGNATURE);
        String sig2 = firstHeader(t2, ExecutorInfoUtils.HEADER_SIGNATURE);
        assertNotNull(sig1);
        assertNotNull(sig2);
        assertNotEquals(sig1, sig2);

        // Each verifies against its own canonical, and fails against the other's.
        String cq1 = ExecutorInfoUtils.canonicalQuery(t1.queries());
        String bs1 = ExecutorInfoUtils.bodySha(t1.body());
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", "task-1",
                firstHeader(t1, ExecutorInfoUtils.HEADER_TIMESTAMP),
                cq1, bs1, sig1));

        String cq2 = ExecutorInfoUtils.canonicalQuery(t2.queries());
        String bs2 = ExecutorInfoUtils.bodySha(t2.body());
        // Cross-check: sig1 does NOT verify with t2's bodySha
        assertFalseQuietly(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", "task-1",
                firstHeader(t1, ExecutorInfoUtils.HEADER_TIMESTAMP),
                cq2, bs2, sig1));
    }

    @Test
    void applySkipsWhenNoOpHeader() {
        // Unsigned endpoint (no op header) — interceptor must not add ts/sig.
        RequestTemplate template = newTemplate("GET", "/taskDispatch/tryFetchTasksWithByteArray");
        interceptor.apply(template);
        assertNull(firstHeader(template, ExecutorInfoUtils.HEADER_TIMESTAMP));
        assertNull(firstHeader(template, ExecutorInfoUtils.HEADER_SIGNATURE));
    }

    @Test
    void applySkipsWhenNoIdentity() {
        // No identity wired — must not sign, must not throw.
        SigningRequestInterceptor bare = new SigningRequestInterceptor();
        RequestTemplate template = newTemplate("GET", "/taskDispatch/tryFetchTasksWithByteArray");
        template.header(ExecutorInfoUtils.HEADER_OP, "FETCH");
        template.header(ExecutorInfoUtils.HEADER_SUBJECT, "exec-1");
        bare.apply(template);
        assertNull(firstHeader(template, ExecutorInfoUtils.HEADER_TIMESTAMP));
        assertNull(firstHeader(template, ExecutorInfoUtils.HEADER_SIGNATURE));
    }

    @Test
    void signatureIsDeterministicForSameCanonicalInputs() {
        // Same inputs at the same instant produce the same signature. We fix
        // the timestamp by stubbing System.currentTimeMillis via a subclass? No —
        // simpler: build canonical by hand with the injected ts and confirm
        // the interceptor's sig equals the hand-built one.
        RequestTemplate template = newTemplate("GET", "/taskDispatch/tryFetchTasksWithByteArray");
        template.header(ExecutorInfoUtils.HEADER_OP, "FETCH");
        template.header(ExecutorInfoUtils.HEADER_SUBJECT, "exec-1");
        interceptor.apply(template);

        String ts = firstHeader(template, ExecutorInfoUtils.HEADER_TIMESTAMP);
        String sig = firstHeader(template, ExecutorInfoUtils.HEADER_SIGNATURE);
        String cq = ExecutorInfoUtils.canonicalQuery(template.queries());
        String bs = ExecutorInfoUtils.bodySha(template.body());
        String expected = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", "exec-1", ts, cq, bs);
        assertEquals(expected, sig);
    }

    // ---- helpers ----

    private static RequestTemplate newTemplate(String method, String path) {
        RequestTemplate t = new RequestTemplate();
        t.method(method);
        // Feign's path is set via the request line; use resolve to set it.
        t.uri(path);
        return t;
    }

    private static String firstHeader(RequestTemplate template, String name) {
        Map<String, Collection<String>> headers = template.headers();
        if (headers == null) return null;
        for (Map.Entry<String, Collection<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue() == null ? null : e.getValue().stream().findFirst().orElse(null);
            }
        }
        return null;
    }

    private static void assertFalseQuietly(boolean v) {
        org.junit.jupiter.api.Assertions.assertFalse(v);
    }
}
