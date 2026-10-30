package ar.edu.um.isa.oncall.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.isa.oncall.IntegrationTest;
import ar.edu.um.isa.oncall.domain.Equipo;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.ObjetivoDeServicio;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.enumeration.Criticidad;
import ar.edu.um.isa.oncall.domain.enumeration.Entorno;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoObjetivo;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests de la regla de MTTA / MTTR: lo que se lee y lo que queda guardado.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin")
class MetricasDeIncidenteResourceIT {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @Test
    @Transactional
    @DisplayName("Un incidente reconocido a tiempo y todavia abierto: reconocimiento cumplido, resolucion en curso")
    void evaluaUnIncidenteAbierto() throws Exception {
        Servicio servicio = servicioConObjetivos();
        Instant ahora = Instant.now();
        Incidente incidente = incidente(servicio, ahora.minus(Duration.ofMinutes(10)));
        incidente.setReconocidoEn(ahora.minus(Duration.ofMinutes(8)));
        em.flush();

        restMockMvc
            .perform(get("/api/incidentes/{id}/metricas", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reconocimiento.estado").value("CUMPLIDO"))
            .andExpect(jsonPath("$.reconocimiento.segundos").value(120))
            .andExpect(jsonPath("$.reconocimiento.minutosObjetivo").value(5))
            .andExpect(jsonPath("$.resolucion.estado").value("EN_CURSO"))
            .andExpect(jsonPath("$.cumplioObjetivo").doesNotExist());
    }

    @Test
    @Transactional
    @DisplayName("Resolver por el ciclo de vida guarda el veredicto en cumplioObjetivo")
    void resolverGuardaElVeredicto() throws Exception {
        Servicio servicio = servicioConObjetivos();
        Incidente incidente = incidente(servicio, Instant.now().minus(Duration.ofMinutes(10)));
        incidente.setReconocidoEn(Instant.now().minus(Duration.ofMinutes(9)));
        em.flush();

        restMockMvc
            .perform(post("/api/incidentes/{id}/resolver", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cumplioObjetivo").value(true));

        em.flush();
        em.clear();
        assertThat(em.find(Incidente.class, incidente.getId()).getCumplioObjetivo()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("Un incidente que no reconocio a tiempo queda con cumplioObjetivo = false")
    void unIncidenteQueNoReconocioATiempoIncumple() throws Exception {
        Servicio servicio = servicioConObjetivos();
        Incidente incidente = incidente(servicio, Instant.now().minus(Duration.ofMinutes(30)));
        incidente.setReconocidoEn(Instant.now().minus(Duration.ofMinutes(20)));
        incidente.setResueltoEn(Instant.now().minus(Duration.ofMinutes(5)));
        incidente.setEstado(EstadoIncidente.RESUELTO);
        em.flush();

        restMockMvc
            .perform(post("/api/incidentes/{id}/metricas/cumplimiento", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reconocimiento.estado").value("INCUMPLIDO"))
            .andExpect(jsonPath("$.cumplioObjetivo").value(false));
    }

    @Test
    @Transactional
    @DisplayName("El reporte del periodo promedia los incidentes del servicio")
    void elReporteDelPeriodo() throws Exception {
        Servicio servicio = servicioConObjetivos();
        Instant base = Instant.parse("2026-02-01T00:00:00Z");
        Incidente uno = incidente(servicio, base);
        uno.reconocidoEn(base.plus(Duration.ofMinutes(2)))
            .resueltoEn(base.plus(Duration.ofMinutes(20)))
            .estado(EstadoIncidente.RESUELTO);
        Incidente dos = incidente(servicio, base.plus(Duration.ofHours(1)));
        dos.reconocidoEn(base.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(10)))
            .resueltoEn(base.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(40)))
            .estado(EstadoIncidente.RESUELTO);
        em.flush();

        restMockMvc
            .perform(
                get("/api/metricas/incidentes")
                    .param("desde", "2026-02-01T00:00:00Z")
                    .param("hasta", "2026-02-02T00:00:00Z")
                    .param("servicioId", servicio.getId().toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.incidentes").value(2))
            .andExpect(jsonPath("$.total.mttaMinutos").value(6.0))
            .andExpect(jsonPath("$.total.mttrMinutos").value(30.0))
            .andExpect(jsonPath("$.total.cumplimientoReconocimiento").value(50.0))
            .andExpect(jsonPath("$.total.cumplimientoResolucion").value(100.0))
            .andExpect(jsonPath("$.porSeveridad[0].severidad").value("SEV1"));
    }

    @Test
    @Transactional
    @DisplayName("Un periodo invertido se rechaza con 400")
    void unPeriodoInvertidoSeRechaza() throws Exception {
        restMockMvc
            .perform(get("/api/metricas/incidentes").param("desde", "2026-02-02T00:00:00Z").param("hasta", "2026-02-01T00:00:00Z"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Un incidente que no existe da 404")
    void unIncidenteQueNoExisteDa404() throws Exception {
        restMockMvc.perform(get("/api/incidentes/{id}/metricas", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    /** Un servicio TIER1 con objetivos SEV1: 5 minutos para reconocer, 60 para resolver. */
    private Servicio servicioConObjetivos() {
        int n = SECUENCIA.incrementAndGet();
        Equipo equipo = new Equipo().nombre("equipo-metricas-" + n).emailContacto("metricas-" + n + "@um.edu.ar");
        em.persist(equipo);
        Servicio servicio = new Servicio()
            .nombre("servicio-metricas-" + n)
            .criticidad(Criticidad.TIER1)
            .entorno(Entorno.PRODUCCION)
            .activo(true)
            .equipo(equipo);
        em.persist(servicio);
        em.persist(
            new ObjetivoDeServicio()
                .tipo(TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO)
                .severidadAplicable(Severidad.SEV1)
                .minutosObjetivo(5)
                .servicio(servicio)
        );
        em.persist(
            new ObjetivoDeServicio()
                .tipo(TipoObjetivo.TIEMPO_DE_RESOLUCION)
                .severidadAplicable(Severidad.SEV1)
                .minutosObjetivo(60)
                .servicio(servicio)
        );
        em.flush();
        return servicio;
    }

    private Incidente incidente(Servicio servicio, Instant detectadoEn) {
        Incidente incidente = new Incidente()
            .titulo("incidente-metricas-" + SECUENCIA.incrementAndGet())
            .severidad(Severidad.SEV1)
            .estado(EstadoIncidente.ABIERTO)
            .detectadoEn(detectadoEn);
        incidente.addServicio(servicio);
        em.persist(incidente);
        return incidente;
    }
}
