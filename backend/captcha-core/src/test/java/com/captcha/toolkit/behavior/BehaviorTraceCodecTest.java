package com.captcha.toolkit.behavior;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为报文编解码测试。
 */
class BehaviorTraceCodecTest {

    @Test
    void roundTripPreservesTrace() {
        BehaviorTrace trace = new BehaviorTrace(1, 300.6, 262.0, 1_000_000L, 1_001_000L,
                List.of(
                        new BehaviorPoint(0, 0.0035, 0.8476, BehaviorEventType.START),
                        new BehaviorPoint(21, 0.0525, 0.9119, BehaviorEventType.MOVE),
                        new BehaviorPoint(980, 0.7784, 0.9961, BehaviorEventType.UP)));

        BehaviorTrace decoded = BehaviorTraceCodec.decode(BehaviorTraceCodec.encode(trace));

        assertEquals(trace, decoded);
    }

    @Test
    void compressedRoundTripPreservesTrace() {
        BehaviorTrace trace = new BehaviorTrace(1, 300.6, 262.0, 1_000_000L, 1_001_000L,
                List.of(
                        new BehaviorPoint(0, 0.0, 0.5, BehaviorEventType.START),
                        new BehaviorPoint(500, 0.5, 0.5, BehaviorEventType.MOVE),
                        new BehaviorPoint(1000, 0.8, 0.5, BehaviorEventType.UP)));

        String compressed = BehaviorTraceCodec.encodeCompressed(trace);

        assertTrue(compressed.startsWith("H4sI"), "gzip+base64url 应以 H4sI 开头");
        assertEquals(trace, BehaviorTraceCodec.decode(compressed));
    }

    @Test
    void malformedTextIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BehaviorTraceCodec.decode("1|300|190|1000"));
        assertThrows(IllegalArgumentException.class,
                () -> BehaviorTraceCodec.decode("1|300|190|1000|2000|0,0.1,0.2"));
        assertThrows(IllegalArgumentException.class,
                () -> BehaviorTraceCodec.decode("1|300|190|1000|2000|0,0.1,0.2,9"));
    }
}
