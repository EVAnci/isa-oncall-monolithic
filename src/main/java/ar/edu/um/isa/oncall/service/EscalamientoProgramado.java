package ar.edu.um.isa.oncall.service;

import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quien despierta al escalamiento: un reloj que corre cada {@code oncall.escalamiento.intervalo}.
 *
 * <p>El intervalo es la resolucion de la regla: con 30 segundos, un paso de 5 minutos sale
 * entre los 5:00 y los 5:30. Se puede apagar con {@code oncall.escalamiento.automatico=false}
 * (los tests lo apagan para controlar el tiempo a mano) y disparar una corrida por
 * {@code POST /api/escalamiento/ejecutar}.</p>
 *
 * <p>{@code fixedDelay} y no {@code fixedRate}: la proxima corrida se agenda cuando termina la
 * anterior, asi dos corridas del mismo reloj nunca se pisan.</p>
 */
@Component
@ConditionalOnProperty(prefix = "oncall.escalamiento", name = "automatico", havingValue = "true", matchIfMissing = true)
public class EscalamientoProgramado {

    private final EjecutorDeEscalamientos ejecutorDeEscalamientos;

    public EscalamientoProgramado(EjecutorDeEscalamientos ejecutorDeEscalamientos) {
        this.ejecutorDeEscalamientos = ejecutorDeEscalamientos;
    }

    @Scheduled(fixedDelayString = "${oncall.escalamiento.intervalo:PT30S}", initialDelayString = "${oncall.escalamiento.intervalo:PT30S}")
    public void despertar() {
        ejecutorDeEscalamientos.ejecutarCiclo(Instant.now());
    }
}
