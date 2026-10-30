package ar.edu.um.isa.oncall.web.rest;

import ar.edu.um.isa.oncall.security.AuthoritiesConstants;
import ar.edu.um.isa.oncall.service.EjecutorDeEscalamientos;
import ar.edu.um.isa.oncall.service.EscalamientoService;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.ResumenDeCicloDTO;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Endpoints de la cadena de escalamiento.
 */
@RestController
@RequestMapping("/api")
public class EscalamientoResource {

    private static final Logger LOG = LoggerFactory.getLogger(EscalamientoResource.class);

    private final EscalamientoService escalamientoService;
    private final EjecutorDeEscalamientos ejecutorDeEscalamientos;

    public EscalamientoResource(EscalamientoService escalamientoService, EjecutorDeEscalamientos ejecutorDeEscalamientos) {
        this.escalamientoService = escalamientoService;
        this.ejecutorDeEscalamientos = ejecutorDeEscalamientos;
    }

    /**
     * {@code GET /api/incidentes/:id/escalamiento} : por donde va la cadena. No ejecuta nada.
     *
     * @param id el incidente.
     * @return {@code 200} con el estado, o {@code 404} si el incidente no existe.
     */
    @GetMapping("/incidentes/{id}/escalamiento")
    public ResponseEntity<EstadoDeEscalamientoDTO> consultarEscalamiento(@PathVariable("id") Long id) {
        LOG.debug("REST request para el escalamiento del Incidente {}", id);
        return ResponseUtil.wrapOrNotFound(escalamientoService.consultar(id, Instant.now()));
    }

    /**
     * {@code POST /api/escalamiento/ejecutar} : corre el proceso de escalamiento ahora, sin
     * esperar al reloj. Es idempotente: correrlo dos veces seguidas no notifica dos veces.
     *
     * @return {@code 200} con el resumen de la corrida.
     */
    @PostMapping("/escalamiento/ejecutar")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResumenDeCicloDTO ejecutar() {
        LOG.debug("REST request para correr el escalamiento");
        return ejecutorDeEscalamientos.ejecutarCiclo(Instant.now());
    }
}
