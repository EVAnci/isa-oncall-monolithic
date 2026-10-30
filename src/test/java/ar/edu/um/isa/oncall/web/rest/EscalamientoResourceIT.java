package ar.edu.um.isa.oncall.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.isa.oncall.IntegrationTest;
import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.domain.Equipo;
import ar.edu.um.isa.oncall.domain.EventoDeIncidente;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.Notificacion;
import ar.edu.um.isa.oncall.domain.PasoEscalamiento;
import ar.edu.um.isa.oncall.domain.PoliticaEscalamiento;
import ar.edu.um.isa.oncall.domain.Rotacion;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.TurnoDeGuardia;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.Canal;
import ar.edu.um.isa.oncall.domain.enumeration.Criticidad;
import ar.edu.um.isa.oncall.domain.enumeration.Entorno;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.RolDeGuardia;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoEvento;
import ar.edu.um.isa.oncall.domain.enumeration.TipoRotacion;
import ar.edu.um.isa.oncall.repository.UserRepository;
import ar.edu.um.isa.oncall.security.AuthoritiesConstants;
import ar.edu.um.isa.oncall.service.EscalamientoService;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests de la cadena de escalamiento y del ciclo de vida del incidente, con la
 * base real: que queda escrito cuando la regla decide.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
class EscalamientoResourceIT {

    private static final String API_EJECUTAR = "/api/escalamiento/ejecutar";

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EscalamientoService escalamientoService;

    @Test
    @Transactional
    @DisplayName("Correr el proceso dos veces seguidas notifica una sola vez, a quien esta de guardia")
    void correrDosVecesNotificaUnaSolaVez() throws Exception {
        Incidente incidente = incidenteConPolitica();

        restMockMvc.perform(post(API_EJECUTAR)).andExpect(status().isOk());
        restMockMvc.perform(post(API_EJECUTAR)).andExpect(status().isOk());

        List<Notificacion> notificaciones = notificacionesDe(incidente);
        assertThat(notificaciones).hasSize(1);
        assertThat(notificaciones.get(0).getDestinatario().getLogin()).isEqualTo("admin");
        assertThat(notificaciones.get(0).getCanal()).isEqualTo(Canal.PUSH);
        assertThat(ejecucionesDe(incidente)).hasSize(1);
        assertThat(eventosDe(incidente)).extracting(EventoDeIncidente::getTipo).containsExactly(TipoEvento.ESCALAMIENTO);

        restMockMvc
            .perform(get("/api/incidentes/{id}/escalamiento", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_CURSO"))
            .andExpect(jsonPath("$.nivel").value(1))
            .andExpect(jsonPath("$.historial.length()").value(1))
            .andExpect(jsonPath("$.historial[0].destinatario").value("admin"));
    }

    @Test
    @Transactional
    @DisplayName("Vencida la espera, el siguiente paso va al destinatario directo")
    void vencidaLaEsperaSubeDeNivel() {
        Incidente incidente = incidenteConPolitica();
        Instant ahora = Instant.now();

        escalamientoService.procesar(incidente.getId(), ahora);
        escalamientoService.procesar(incidente.getId(), ahora.plus(Duration.ofMinutes(5)));
        em.flush();

        assertThat(notificacionesDe(incidente))
            .extracting(n -> n.getDestinatario().getLogin(), Notificacion::getCanal)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("admin", Canal.PUSH),
                org.assertj.core.groups.Tuple.tuple("user", Canal.EMAIL)
            );
    }

    @Test
    @Transactional
    @DisplayName("Reconocer corta el escalamiento y nombra comandante a quien reconoce")
    void reconocerCortaElEscalamiento() throws Exception {
        Incidente incidente = incidenteConPolitica();

        restMockMvc
            .perform(post("/api/incidentes/{id}/reconocer", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RECONOCIDO"))
            .andExpect(jsonPath("$.comandante").value("admin"))
            .andExpect(jsonPath("$.cambio").value(true));

        // reconocer de nuevo no cambia nada
        restMockMvc
            .perform(post("/api/incidentes/{id}/reconocer", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cambio").value(false));

        restMockMvc.perform(post(API_EJECUTAR)).andExpect(status().isOk());

        assertThat(notificacionesDe(incidente)).isEmpty();
        assertThat(eventosDe(incidente)).extracting(EventoDeIncidente::getTipo).containsExactly(TipoEvento.CAMBIO_DE_ESTADO);
        restMockMvc
            .perform(get("/api/incidentes/{id}/escalamiento", incidente.getId()))
            .andExpect(jsonPath("$.estado").value("RECONOCIDO"));
    }

    @Test
    @Transactional
    @DisplayName("Silenciar pausa el escalamiento")
    void silenciarPausaElEscalamiento() throws Exception {
        Incidente incidente = incidenteConPolitica();

        restMockMvc
            .perform(
                post("/api/incidentes/{id}/silenciar", incidente.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"minutos\":30}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.silenciadoHasta").isNotEmpty());

        restMockMvc.perform(post(API_EJECUTAR)).andExpect(status().isOk());

        assertThat(notificacionesDe(incidente)).isEmpty();
        restMockMvc
            .perform(get("/api/incidentes/{id}/escalamiento", incidente.getId()))
            .andExpect(jsonPath("$.estado").value("SILENCIADO"));
    }

    @Test
    @Transactional
    @DisplayName("Un silencio fuera de rango se rechaza")
    void unSilencioFueraDeRangoSeRechaza() throws Exception {
        Incidente incidente = incidenteConPolitica();

        restMockMvc
            .perform(
                post("/api/incidentes/{id}/silenciar", incidente.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"minutos\":5000}")
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Resolver deja la marca, corta el escalamiento y no se puede reconocer despues")
    void resolverCortaYNoSePuedeReconocerDespues() throws Exception {
        Incidente incidente = incidenteConPolitica();

        restMockMvc
            .perform(post("/api/incidentes/{id}/resolver", incidente.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("RESUELTO"))
            .andExpect(jsonPath("$.resueltoEn").isNotEmpty());

        restMockMvc.perform(post("/api/incidentes/{id}/reconocer", incidente.getId())).andExpect(status().isBadRequest());

        restMockMvc.perform(get("/api/incidentes/{id}/escalamiento", incidente.getId())).andExpect(jsonPath("$.estado").value("RESUELTO"));
    }

    @Test
    @Transactional
    @DisplayName("Un incidente que no existe da 404")
    void unIncidenteQueNoExisteDa404() throws Exception {
        restMockMvc.perform(get("/api/incidentes/{id}/escalamiento", Long.MAX_VALUE)).andExpect(status().isNotFound());
        restMockMvc.perform(post("/api/incidentes/{id}/reconocer", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = AuthoritiesConstants.USER)
    @DisplayName("Disparar el proceso a mano es solo para administradores")
    void dispararElProcesoEsSoloParaAdministradores() throws Exception {
        restMockMvc.perform(post(API_EJECUTAR)).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    /**
     * Un incidente SEV1 abierto hace un minuto, sobre un servicio con politica: paso 1 push a la
     * guardia (admin, de turno ahora), 5 minutos; paso 2 mail directo a user, 10 minutos.
     */
    private Incidente incidenteConPolitica() {
        int n = SECUENCIA.incrementAndGet();
        User admin = userRepository.findOneByLogin("admin").orElseThrow();
        User user = userRepository.findOneByLogin("user").orElseThrow();
        Instant ahora = Instant.now();

        Equipo equipo = new Equipo().nombre("equipo-escalamiento-" + n).emailContacto("equipo-" + n + "@um.edu.ar");
        em.persist(equipo);
        Servicio servicio = new Servicio()
            .nombre("servicio-escalamiento-" + n)
            .criticidad(Criticidad.TIER1)
            .entorno(Entorno.PRODUCCION)
            .activo(true)
            .equipo(equipo);
        em.persist(servicio);

        Rotacion rotacion = new Rotacion()
            .nombre("rotacion-" + n)
            .tipo(TipoRotacion.SEMANAL)
            .zonaHoraria("UTC")
            .activa(true)
            .equipo(equipo);
        em.persist(rotacion);
        em.persist(
            new TurnoDeGuardia()
                .desde(ahora.minus(Duration.ofHours(4)))
                .hasta(ahora.plus(Duration.ofHours(4)))
                .esReemplazo(false)
                .rol(RolDeGuardia.PRIMARIO)
                .rotacion(rotacion)
                .responsable(admin)
        );

        PoliticaEscalamiento politica = new PoliticaEscalamiento()
            .nombre("politica-" + n)
            .repetirVeces(0)
            .servicio(servicio);
        em.persist(politica);
        em.persist(new PasoEscalamiento().orden(1).esperaMinutos(5).canal(Canal.PUSH).politica(politica).rotacion(rotacion));
        em.persist(new PasoEscalamiento().orden(2).esperaMinutos(10).canal(Canal.EMAIL).politica(politica).destinatarioDirecto(user));

        Incidente incidente = new Incidente()
            .titulo("incidente-escalamiento-" + n)
            .severidad(Severidad.SEV1)
            .estado(EstadoIncidente.ABIERTO)
            .detectadoEn(ahora.minus(Duration.ofMinutes(1)));
        incidente.addServicio(servicio);
        em.persist(incidente);
        em.flush();
        em.clear();
        return incidente;
    }

    private List<Notificacion> notificacionesDe(Incidente incidente) {
        return em
            .createQuery("select n from Notificacion n join fetch n.destinatario where n.incidente.id = :id", Notificacion.class)
            .setParameter("id", incidente.getId())
            .getResultList();
    }

    private List<EjecucionDeEscalamiento> ejecucionesDe(Incidente incidente) {
        return em
            .createQuery("select e from EjecucionDeEscalamiento e where e.incidente.id = :id", EjecucionDeEscalamiento.class)
            .setParameter("id", incidente.getId())
            .getResultList();
    }

    private List<EventoDeIncidente> eventosDe(Incidente incidente) {
        return em
            .createQuery("select e from EventoDeIncidente e where e.incidente.id = :id", EventoDeIncidente.class)
            .setParameter("id", incidente.getId())
            .getResultList();
    }
}
