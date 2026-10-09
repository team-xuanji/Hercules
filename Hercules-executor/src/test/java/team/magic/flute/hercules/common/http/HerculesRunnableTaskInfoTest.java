package team.magic.flute.hercules.common.http;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.common.status.TaskType;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link HerculesRunnableTaskInfo#buildDefaultUniId}. No
 * Spring context, no DB. The forward chain
 * ({@code ExecutorProcessHandleImpl.tryForwardTask}) relies on this method to
 * mint a collision-free id when a forwarded task arrives without one; these
 * tests pin the determinism and component-sensitivity invariants that contract
 * depends on.
 */
class HerculesRunnableTaskInfoTest {

    private static final Pattern MD5_HEX = Pattern.compile("^[0-9a-f]{32}$");

    private static HerculesRunnableTaskInfo base() {
        return new HerculesRunnableTaskInfo()
                .setPluginGroup("group-1")
                .setPluginHandle("handle-1")
                .setContext("ctx")
                .setExecutorRegion("region-1")
                .setFromType(TaskType.FORWARD)
                .setFromSourceId("parent-1");
    }

    @Test
    void yieldsValidMd5Hex() {
        String id = base().buildDefaultUniId();
        assertNotNull(id);
        assertTrue(MD5_HEX.matcher(id).matches(), "default uni id must be a 32-char md5 hex: " + id);
    }

    @Test
    void deterministicForSameInputs() {
        // Same forward descriptor must always produce the same default id — this is
        // what makes the blank-id fallback idempotent across retries.
        assertEquals(base().buildDefaultUniId(), base().buildDefaultUniId());
    }

    @Test
    void sensitiveToPluginHandle() {
        assertNotEquals(base().buildDefaultUniId(),
                base().setPluginHandle("handle-2").buildDefaultUniId());
    }

    @Test
    void sensitiveToPluginGroup() {
        assertNotEquals(base().buildDefaultUniId(),
                base().setPluginGroup("group-2").buildDefaultUniId());
    }

    @Test
    void sensitiveToContext() {
        assertNotEquals(base().buildDefaultUniId(),
                base().setContext("different-ctx").buildDefaultUniId());
    }

    @Test
    void sensitiveToExecutorRegion() {
        assertNotEquals(base().buildDefaultUniId(),
                base().setExecutorRegion("region-2").buildDefaultUniId());
    }

    @Test
    void sensitiveToFromType() {
        // ONCE vs FORWARD must yield different ids — the fromType encodes the
        // dispatch lane and cannot collapse with other task types.
        assertNotEquals(base().setFromType(TaskType.ONCE).buildDefaultUniId(),
                base().setFromType(TaskType.FORWARD).buildDefaultUniId());
    }

    @Test
    void sensitiveToFromSourceId() {
        assertNotEquals(base().buildDefaultUniId(),
                base().setFromSourceId("parent-2").buildDefaultUniId());
    }

    @Test
    void distinctForwardsDoNotCollide() {
        // The forward flow mints ids for many distinct child tasks; a healthy hash
        // over distinct (context, fromSourceId) pairs must not collide in practice.
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ids.add(base().setFromSourceId("parent-" + i).setContext("ctx-" + i).buildDefaultUniId());
        }
        assertEquals(500, ids.size(), "default uni ids must be collision-free across distinct forwards");
    }

    @Test
    void stableWhenAllRoutingFieldsBeNull() {
        // Degenerate but legal: the method must still return a stable md5 rather
        // than throwing, so the forward fallback never NPEs on a sparse request.
        HerculesRunnableTaskInfo empty = new HerculesRunnableTaskInfo();
        String id = empty.buildDefaultUniId();
        assertNotNull(id);
        assertTrue(MD5_HEX.matcher(id).matches());
        assertEquals(id, empty.buildDefaultUniId());
    }
}
