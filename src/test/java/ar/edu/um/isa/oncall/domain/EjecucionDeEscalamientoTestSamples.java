package ar.edu.um.isa.oncall.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EjecucionDeEscalamientoTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static EjecucionDeEscalamiento getEjecucionDeEscalamientoSample1() {
        return new EjecucionDeEscalamiento().id(1L).vuelta(1).nivel(1).detalle("detalle1");
    }

    public static EjecucionDeEscalamiento getEjecucionDeEscalamientoSample2() {
        return new EjecucionDeEscalamiento().id(2L).vuelta(2).nivel(2).detalle("detalle2");
    }

    public static EjecucionDeEscalamiento getEjecucionDeEscalamientoRandomSampleGenerator() {
        return new EjecucionDeEscalamiento()
            .id(longCount.incrementAndGet())
            .vuelta(intCount.incrementAndGet())
            .nivel(intCount.incrementAndGet())
            .detalle(UUID.randomUUID().toString());
    }
}
