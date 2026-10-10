package team.magic.flute.hercules.manager.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import team.magic.flute.hercules.common.util.ExecutorInfoUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline unit tests pinning client/server canonical-message consistency for
 * the header-based signing protocol (ADR-0016). The executor (feign client)
 * builds its canonical from {@code RequestTemplate.queries()} (a
 * {@code Map<String, Collection<String>>}) and {@code RequestTemplate.body()}
 * (a {@code byte[]}); the manager (servlet server) builds its canonical from
 * {@code HttpServletRequest.getParameterMap()} (a {@code Map<String, String[]>})
 * and {@code request.getAttribute("hercules.rawBody")} (a {@code byte[]}).
 *
 * <p>Both paths flow through {@link ExecutorInfoUtils#canonicalQuery} and
 * {@link ExecutorInfoUtils#bodySha}, so this test pins that those two methods
 * produce the same canonical fields from both input shapes — the core
 * invariant that makes the signature verify end-to-end. No Spring context.
 */
class TaskDispatchSignVerifyTest {

    private static final String IDENTITY = "EXECUTOR_IDENTITY_MGR_TEST";

    // ---- fetch (GET, query params, no body) ----

    @Test
    void fetchClientServerCanonicalIdenticalAndVerifies() {
        String executorId = "exec-1";
        String fetchLimit = "10";
        String ts = String.valueOf(System.currentTimeMillis());

        // Client side: feign template.queries() → Map<String, Collection<String>>
        Map<String, Collection<String>> clientQueries = new LinkedHashMap<>();
        clientQueries.put("executorId", Collections.singletonList(executorId));
        clientQueries.put("executorRegion", Collections.singletonList("TEST"));
        clientQueries.put("fetchLimit", Collections.singletonList(fetchLimit));
        byte[] clientBody = null; // GET, no body

        String clientCq = ExecutorInfoUtils.canonicalQuery(clientQueries);
        String clientBodySha = ExecutorInfoUtils.bodySha(clientBody);

        // Server side: servlet getParameterMap() → Map<String, String[]>
        Map<String, String[]> serverQueries = new TreeMap<>();
        serverQueries.put("executorId", new String[]{executorId});
        serverQueries.put("executorRegion", new String[]{"TEST"});
        serverQueries.put("fetchLimit", new String[]{fetchLimit});

        String serverCq = ExecutorInfoUtils.canonicalQuery(serverQueries);
        String serverBodySha = ExecutorInfoUtils.bodySha(null); // no rawBody attr

        // Canonical fields match despite different input shapes
        assertEquals(clientCq, serverCq);
        assertEquals(clientBodySha, serverBodySha);

        // Signature from client verifies on server
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", executorId, ts, clientCq, clientBodySha);
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", executorId, ts, serverCq, serverBodySha, sig));
    }

    @Test
    void fetchQueryTamperFailsVerification() {
        String executorId = "exec-1";
        String ts = String.valueOf(System.currentTimeMillis());

        // Client signs with fetchLimit=10
        Map<String, Collection<String>> clientQueries = new LinkedHashMap<>();
        clientQueries.put("executorId", Collections.singletonList(executorId));
        clientQueries.put("fetchLimit", Collections.singletonList("10"));
        String clientCq = ExecutorInfoUtils.canonicalQuery(clientQueries);
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", executorId, ts, clientCq, "");

        // Server sees fetchLimit=99 (tampered)
        Map<String, String[]> tamperedQueries = new TreeMap<>();
        tamperedQueries.put("executorId", new String[]{executorId});
        tamperedQueries.put("fetchLimit", new String[]{"99"});
        String tamperedCq = ExecutorInfoUtils.canonicalQuery(tamperedQueries);

        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", executorId, ts, tamperedCq, "", sig));
    }

    // ---- finish (PUT, body) ----

    @Test
    void finishClientServerCanonicalIdenticalAndVerifies() {
        String taskId = "task-42";
        String ts = String.valueOf(System.currentTimeMillis());

        // Client side: body is the raw wire bytes (JSON serialized VO)
        byte[] body = "{\"taskId\":\"task-42\",\"checkPointInfo\":\"cp-1\",\"executorId\":\"exec-1\"}"
                .getBytes(StandardCharsets.UTF_8);
        String clientCq = "";
        String clientBodySha = ExecutorInfoUtils.bodySha(body);

        // Server side: rawBody from CachedBodyFilter attribute
        byte[] rawBody = body.clone();
        String serverCq = ExecutorInfoUtils.canonicalQuery(new TreeMap<>());
        String serverBodySha = ExecutorInfoUtils.bodySha(rawBody);

        assertEquals(clientCq, serverCq);
        assertEquals(clientBodySha, serverBodySha);

        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FINISH", taskId, ts, clientCq, clientBodySha);
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", taskId, ts, serverCq, serverBodySha, sig));
    }

    @Test
    void finishBodyTamperFailsVerification() {
        String taskId = "task-42";
        String ts = String.valueOf(System.currentTimeMillis());

        byte[] originalBody = "{\"taskId\":\"task-42\",\"checkPointInfo\":\"cp-1\"}"
                .getBytes(StandardCharsets.UTF_8);
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FINISH", taskId, ts, "",
                ExecutorInfoUtils.bodySha(originalBody));

        // Tampered body — checkPointInfo changed
        byte[] tamperedBody = "{\"taskId\":\"task-42\",\"checkPointInfo\":\"cp-evil\"}"
                .getBytes(StandardCharsets.UTF_8);
        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", taskId, ts, "",
                ExecutorInfoUtils.bodySha(tamperedBody), sig));
    }

    @Test
    void finishBodyMissingOnServerStillVerifiesWhenClientHasNoBody() {
        // If client sent no body, server rawBody attr is null — both produce ""
        String taskId = "task-42";
        String ts = String.valueOf(System.currentTimeMillis());
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FINISH", taskId, ts, "", "");
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", taskId, ts, "",
                ExecutorInfoUtils.bodySha(null), sig));
    }

    // ---- timestamp freshness ----

    @Test
    void staleTimestampFailsVerification() {
        String executorId = "exec-1";
        // 10 minutes ago — well outside the 5-min window
        String staleTs = String.valueOf(System.currentTimeMillis() - 600_000L);

        Map<String, Collection<String>> queries = new LinkedHashMap<>();
        queries.put("executorId", Collections.singletonList(executorId));
        String cq = ExecutorInfoUtils.canonicalQuery(queries);

        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", executorId, staleTs, cq, "");

        // The signature itself verifies (it was correctly signed over the stale ts)
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", executorId, staleTs, cq, "", sig));

        // But the freshness check rejects it
        assertFalse(ExecutorInfoUtils.isFreshTimestamp(staleTs, System.currentTimeMillis()));
    }

    @Test
    void freshTimestampPassesAndSignatureVerifies() {
        String executorId = "exec-1";
        String freshTs = String.valueOf(System.currentTimeMillis());

        Map<String, Collection<String>> queries = new LinkedHashMap<>();
        queries.put("executorId", Collections.singletonList(executorId));
        String cq = ExecutorInfoUtils.canonicalQuery(queries);

        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", executorId, freshTs, cq, "");

        assertTrue(ExecutorInfoUtils.isFreshTimestamp(freshTs, System.currentTimeMillis()));
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FETCH", executorId, freshTs, cq, "", sig));
    }

    // ---- servlet HttpServletRequest integration shape ----

    @Test
    void mockedServletRequestProducesSameCanonicalAsClient() {
        // This mocks HttpServletRequest the same way CachedBodyFilter +
        // TaskDispatchController would read it, and confirms the canonical
        // built from the mocked request matches the client-side canonical.
        String executorId = "exec-1";
        String region = "TEST";
        String fetchLimit = "10";
        String ts = String.valueOf(System.currentTimeMillis());

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);

        // Server query params (String[] shape)
        Map<String, String[]> paramMap = new TreeMap<>();
        paramMap.put("executorId", new String[]{executorId});
        paramMap.put("executorRegion", new String[]{region});
        paramMap.put("fetchLimit", new String[]{fetchLimit});
        Mockito.when(req.getParameterMap()).thenReturn(paramMap);

        // Server raw body (GET → null attribute)
        Mockito.when(req.getAttribute("hercules.rawBody")).thenReturn(null);

        // Headers the client set (op/subject) and interceptor set (ts/sig)
        Mockito.when(req.getHeader(ExecutorInfoUtils.HEADER_OP)).thenReturn("FETCH");
        Mockito.when(req.getHeader(ExecutorInfoUtils.HEADER_SUBJECT)).thenReturn(executorId);

        // Client-side canonical
        Map<String, Collection<String>> clientQueries = new LinkedHashMap<>();
        clientQueries.put("executorId", Collections.singletonList(executorId));
        clientQueries.put("executorRegion", Collections.singletonList(region));
        clientQueries.put("fetchLimit", Collections.singletonList(fetchLimit));
        String clientCq = ExecutorInfoUtils.canonicalQuery(clientQueries);
        String clientBodySha = ExecutorInfoUtils.bodySha(null);

        // Server-side canonical (built exactly as isInvalidExecutorOp does)
        String serverCq = ExecutorInfoUtils.canonicalQuery(req.getParameterMap());
        byte[] rawBody = (byte[]) req.getAttribute("hercules.rawBody");
        String serverBodySha = ExecutorInfoUtils.bodySha(rawBody);

        assertEquals(clientCq, serverCq);
        assertEquals(clientBodySha, serverBodySha);

        // Sign on client, verify on server
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FETCH", executorId, ts, clientCq, clientBodySha);
        assertTrue(ExecutorInfoUtils.verifyRequest(
                IDENTITY, req.getHeader(ExecutorInfoUtils.HEADER_OP),
                req.getHeader(ExecutorInfoUtils.HEADER_SUBJECT),
                ts, serverCq, serverBodySha, sig));
    }

    @Test
    void mockedServletRequestBodyTamperDetected() {
        // Same shape as finishOneTask: body is the signed payload, tampering changes bodySha.
        String taskId = "task-42";
        String ts = String.valueOf(System.currentTimeMillis());

        byte[] originalBody = "{\"taskId\":\"task-42\",\"checkPointInfo\":\"cp-1\"}"
                .getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody = "{\"taskId\":\"task-42\",\"checkPointInfo\":\"cp-evil\"}"
                .getBytes(StandardCharsets.UTF_8);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameterMap()).thenReturn(new TreeMap<>());
        Mockito.when(req.getAttribute("hercules.rawBody")).thenReturn(tamperedBody);

        // Client signed the original body
        String sig = ExecutorInfoUtils.signRequest(
                IDENTITY, "FINISH", taskId, ts, "",
                ExecutorInfoUtils.bodySha(originalBody));

        // Server reads tampered body from attribute
        byte[] serverRawBody = (byte[]) req.getAttribute("hercules.rawBody");
        String serverBodySha = ExecutorInfoUtils.bodySha(serverRawBody);

        assertFalse(ExecutorInfoUtils.verifyRequest(
                IDENTITY, "FINISH", taskId, ts, "", serverBodySha, sig));
    }
}
