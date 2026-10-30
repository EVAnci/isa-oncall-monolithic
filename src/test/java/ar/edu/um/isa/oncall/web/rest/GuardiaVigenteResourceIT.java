package ar.edu.um.isa.oncall.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.isa.oncall.IntegrationTest;
import ar.edu.um.isa.oncall.domain.Equipo;
import ar.edu.um.isa.oncall.domain.Rotacion;
import ar.edu.um.isa.oncall.domain.TurnoDeGuardia;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.RolDeGuardia;
import ar.edu.um.isa.oncall.domain.enumeration.TipoRotacion;
import ar.edu.um.isa.oncall.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
 * Integration tests de la regla de guardia vigente: la consulta contra la base real, que es
 * donde vive el criterio del intervalo semiabierto.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GuardiaVigenteResourceIT {

    private static final String API_GUARDIA = "/api/rotaciones/{id}/guardia";

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("En el relevo manda el turno que empieza; un reemplazo pisa al regular")
    void relevoYReemplazo() throws Exception {
        Rotacion rotacion = rotacion("UTC");
        turno(rotacion, "admin", "2026-03-10T00:00:00Z", "2026-03-10T09:00:00Z", false);
        turno(rotacion, "user", "2026-03-10T09:00:00Z", "2026-03-10T18:00:00Z", false);
        turno(rotacion, "admin", "2026-03-10T12:00:00Z", "2026-03-10T14:00:00Z", true);

        restMockMvc
            .perform(get(API_GUARDIA, rotacion.getId()).param("en", "2026-03-10T09:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cobertura").value("CUBIERTA"))
            .andExpect(jsonPath("$.primario.login").value("user"));

        restMockMvc
            .perform(get(API_GUARDIA, rotacion.getId()).param("en", "2026-03-10T13:00:00Z"))
            .andExpect(jsonPath("$.primario.login").value("admin"))
            .andExpect(jsonPath("$.primario.esReemplazo").value(true));
    }

    @Test
    @Transactional
    @DisplayName("Un hueco responde 200 SIN_COBERTURA, no un error")
    void unHuecoRespondeSinCobertura() throws Exception {
        Rotacion rotacion = rotacion("UTC");
        turno(rotacion, "admin", "2026-03-10T00:00:00Z", "2026-03-10T08:00:00Z", false);

        restMockMvc
            .perform(get(API_GUARDIA, rotacion.getId()).param("en", "2026-03-10T08:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cobertura").value("SIN_COBERTURA"))
            .andExpect(jsonPath("$.primario").doesNotExist());
    }

    @Test
    @Transactional
    @DisplayName("La hora local se interpreta en la zona de la rotacion")
    void laHoraLocalSeInterpretaEnLaZonaDeLaRotacion() throws Exception {
        Rotacion rotacion = rotacion("America/Argentina/Mendoza");
        // 09:00 a 17:00 hora de Mendoza = 12:00 a 20:00 UTC
        turno(rotacion, "admin", "2026-03-10T12:00:00Z", "2026-03-10T20:00:00Z", false);

        restMockMvc
            .perform(get(API_GUARDIA, rotacion.getId()).param("horaLocal", "2026-03-10T09:30:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instante").value("2026-03-10T12:30:00Z"))
            .andExpect(jsonPath("$.primario.login").value("admin"));
    }

    @Test
    @Transactional
    @DisplayName("Pedir 'en' y 'horaLocal' a la vez es ambiguo: 400")
    void pedirLosDosInstantesEsAmbiguo() throws Exception {
        Rotacion rotacion = rotacion("UTC");

        restMockMvc
            .perform(get(API_GUARDIA, rotacion.getId()).param("en", "2026-03-10T09:00:00Z").param("horaLocal", "2026-03-10T09:00:00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Una rotacion que no existe da 404")
    void unaRotacionQueNoExisteDa404() throws Exception {
        restMockMvc.perform(get(API_GUARDIA, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private Rotacion rotacion(String zonaHoraria) {
        int n = SECUENCIA.incrementAndGet();
        Equipo equipo = new Equipo().nombre("equipo-guardia-" + n).emailContacto("guardia-" + n + "@um.edu.ar");
        em.persist(equipo);
        Rotacion rotacion = new Rotacion()
            .nombre("rotacion-guardia-" + n)
            .tipo(TipoRotacion.SEMANAL)
            .zonaHoraria(zonaHoraria)
            .activa(true)
            .equipo(equipo);
        em.persist(rotacion);
        em.flush();
        return rotacion;
    }

    private void turno(Rotacion rotacion, String login, String desde, String hasta, boolean esReemplazo) {
        User responsable = userRepository.findOneByLogin(login).orElseThrow();
        em.persist(
            new TurnoDeGuardia()
                .desde(Instant.parse(desde))
                .hasta(Instant.parse(hasta))
                .esReemplazo(esReemplazo)
                .rol(RolDeGuardia.PRIMARIO)
                .rotacion(rotacion)
                .responsable(responsable)
        );
        em.flush();
    }
}
