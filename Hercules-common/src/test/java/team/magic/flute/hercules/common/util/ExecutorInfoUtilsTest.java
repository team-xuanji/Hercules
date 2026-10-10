package team.magic.flute.hercules.common.util;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.common.global.ExecutorTaskOps;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline unit tests for the header-based signing protocol utilities
 * (ADR-0016). No Spring, no DB — pins the 5-field canonical message format,
 * query canonicalization, sign/verify roundtrip, single-char tamper detection
 * and the timestamp freshness window boundaries.
 */
class ExecutorInfoUtilsTest {

    private static final String IDENTITY = "EXECUTOR_IDENTITY_TEST_123";

    @Test
    void buildCanonicalMessageExactString() {
        String msg = ExecutorInfoUtils.buildCanonicalMessage(
                "FETCH", "exec-1", "1700000000000",
                "executorId=exec-1&fetchLimit=10", "");
        String expected = "FETCH\nexec-1\n1700000000000\nexecutorId=exec-1&fetchLimit=10\n";
        assertEquals(expected, msg);
    }

    @Test
    void buildCanonicalMessageWithBodySha() {
        String msg = ExecutorInfoUtils.buildCanonicalMessage(
                "FINISH", "task-9", "1700000000001", "", "abc123");
        assertEquals("FINISH\ntask-9\n1700000000001\n\nabc123", msg);
    }

    @Test
    void canonicalQuerySortedByNameAsc() {
        Map<String, List<String>> q = new LinkedHashMap<>();
        q.put("zeta", Arrays.asList("1"));
        q.put("alpha", Arrays.asList("2"));
        q.put("mid", Arrays.asList("3"));
        assertEquals("alpha=2&mid=3&zeta=1", ExecutorInfoUtils.canonicalQuery(q));
    }

    @Test
    void canonicalQuerySameNameValuesSortedByString() {
        Map<String, List<String>> q = new LinkedHashMap<>();
        List<String> vals = new ArrayList<>();
        vals.add("b");
        vals.add("a");
        vals.add("c");
        q.put("k", vals);
        assertEquals("k=a&k=b&k=c", ExecutorInfoUtils.canonicalQuery(q));
    }

    @Test
    void canonicalQueryEmpty() {
        assertEquals("", ExecutorInfoUtils.canonicalQuery(new HashMap<>()));
        assertEquals("", ExecutorInfoUtils.canonicalQuery(null));
    }

    @Test
    void canonicalQueryNoPercentEncoding() {
        Map<String, List<String>> q = new HashMap<>();
        q.put("region", Arrays.asList("us/east=1"));
        assertEquals("region=us/east=1", ExecutorInfoUtils.canonicalQuery(q));
    }

    @Test
    void signAndVerifyRoundtrip() {
        String ts = String.valueOf(System.currentTimeMillis());
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", "exec-1", ts,
                "executorId=exec-1&fetchLimit=10", "");
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-1", ts,
                "executorId=exec-1&fetchLimit=10", "", sig));
    }

    @Test
    void oneCharChangeFailsVerification() {
        String ts = String.valueOf(System.currentTimeMillis());
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", "exec-1", ts, "", "");
        // tamper subject
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-2", ts, "", "", sig));
        // tamper op
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "LOCK", "exec-1", ts, "", "", sig));
        // tamper query
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-1", ts, "x=1", "", sig));
        // tamper bodySha
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-1", ts, "", "deadbeef", sig));
        // tamper identity (key)
        assertFalse(ExecutorInfoUtils.verifyRequest(
                "WRONG_KEY", "FETCH", "exec-1", ts, "", "", sig));
    }

    @Test
    void verifyRejectsNullBlankSignature() {
        String ts = String.valueOf(System.currentTimeMillis());
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-1", ts, "", "", null));
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", "exec-1", ts, "", "", ""));
    }

    @Test
    void signRequestDifferentInputsProduceDifferentSignatures() {
        String ts = String.valueOf(System.currentTimeMillis());
        String s1 = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", "exec-1", ts, "", "");
        String s2 = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", "exec-1", ts, "x=1", "");
        assertNotEquals(s1, s2);
    }

    @Test
    void timestampWindowBoundaryPass() {
        long now = 1_000_000_000_000L;
        // exactly at window edge — |delta| == window → accepted (<=)
        String tsAtEdge = String.valueOf(now + ExecutorInfoUtils.FRESHNESS_WINDOW_MS);
        assertTrue(ExecutorInfoUtils.isFreshTimestamp(tsAtEdge, now));
        String tsAtEdgePast = String.valueOf(now - ExecutorInfoUtils.FRESHNESS_WINDOW_MS);
        assertTrue(ExecutorInfoUtils.isFreshTimestamp(tsAtEdgePast, now));
    }

    @Test
    void timestampWindowJustOutsideRejected() {
        long now = 1_000_000_000_000L;
        String tooOld = String.valueOf(now - ExecutorInfoUtils.FRESHNESS_WINDOW_MS - 1);
        assertFalse(ExecutorInfoUtils.isFreshTimestamp(tooOld, now));
        String tooNew = String.valueOf(now + ExecutorInfoUtils.FRESHNESS_WINDOW_MS + 1);
        assertFalse(ExecutorInfoUtils.isFreshTimestamp(tooNew, now));
    }

    @Test
    void timestampUnparseableRejected() {
        long now = System.currentTimeMillis();
        assertFalse(ExecutorInfoUtils.isFreshTimestamp("not-a-number", now));
        assertFalse(ExecutorInfoUtils.isFreshTimestamp("", now));
        assertFalse(ExecutorInfoUtils.isFreshTimestamp(null, now));
    }

    @Test
    void signingHeadersContainOpAndSubject() {
        Map<String, String> h = ExecutorInfoUtils.signingHeaders(ExecutorTaskOps.LOCK, "task-7");
        assertEquals("LOCK", h.get(ExecutorInfoUtils.HEADER_OP));
        assertEquals("task-7", h.get(ExecutorInfoUtils.HEADER_SUBJECT));
        assertEquals(2, h.size());
    }

    @Test
    void bodyShaEmptyForNullOrEmptyBody() {
        assertEquals("", ExecutorInfoUtils.bodySha(null));
        assertEquals("", ExecutorInfoUtils.bodySha(new byte[0]));
    }

    @Test
    void bodyShaLowercaseHexForNonEmpty() {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        String sha = ExecutorInfoUtils.bodySha(body);
        assertEquals(64, sha.length());
        assertTrue(sha.equals(sha.toLowerCase()));
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha);
    }

    @Test
    void buildSignSubjectSortedCommaJoined() {
        List<String> ids = Arrays.asList("t3", "t1", "t2");
        assertEquals("t1,t2,t3", ExecutorInfoUtils.buildSignSubject(ids));
    }

    @Test
    void buildSignSubjectDropsNulls() {
        List<String> ids = Arrays.asList("t2", null, "t1", null);
        assertEquals("t1,t2", ExecutorInfoUtils.buildSignSubject(ids));
    }

    @Test
    void sha256HexKnownVector() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ExecutorInfoUtils.sha256Hex(new byte[0]));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ExecutorInfoUtils.sha256Hex(null));
    }

    @Test
    void canonicalQueryHandlesStringArrayInput() {
        // Server side: HttpServletRequest.getParameterMap() returns Map<String,String[]>
        Map<String, String[]> arrMap = new TreeMap<>();
        arrMap.put("b", new String[]{"2"});
        arrMap.put("a", new String[]{"1", "0"});
        assertEquals("a=0&a=1&b=2", ExecutorInfoUtils.canonicalQuery(arrMap));
    }

    @Test
    void canonicalQueryHandlesTreeMapInput() {
        Map<String, List<String>> collMap = new TreeMap<>();
        collMap.put("b", Arrays.asList("2"));
        collMap.put("a", Arrays.asList("1"));
        assertEquals("a=1&b=2", ExecutorInfoUtils.canonicalQuery(collMap));
    }
}
