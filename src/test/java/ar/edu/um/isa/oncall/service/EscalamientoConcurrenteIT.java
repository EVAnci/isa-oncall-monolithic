package ar.edu.um.isa.oncall.service;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.isa.oncall.IntegrationTest;
import ar.edu.um.isa.oncall.domain.Equipo;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.PasoEscalamiento;
import ar.edu.um.isa.oncall.domain.PoliticaEscalamiento;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.Canal;
import ar.edu.um.isa.oncall.domain.enumeration.Criticidad;
import ar.edu.um.isa.oncall.domain.enumeration.Entorno;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La idempotencia del escalamiento frente a dos corridas realmente simultaneas: dos hilos,
 * dos transacciones, la misma fila. Este test no es transaccional a proposito (cada corrida
 * tiene que commitear), asi que limpia lo que crea.
 */
@IntegrationTest
class EscalamientoConcurrenteIT {

    @Autowired
    private EscalamientoService escalamientoService;

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    private Long incidenteId;
    private Long politicaId;
    private Long servicioId;
    private Long equipoId;

    @BeforeEach
    void crearDatos() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(estado -> {
            User user = userRepository.findOneByLogin("user").orElseThrow();
            Equipo equipo = new Equipo().nombre("equipo-concurrente").emailContacto("concurrente@um.edu.ar");
            em.persist(equipo);
            Servicio servicio = new Servicio()
                .nombre("servicio-concurrente")
                .criticidad(Criticidad.TIER1)
                .entorno(Entorno.PRODUCCION)
                .activo(true)
                .equipo(equipo);
            em.persist(servicio);
            PoliticaEscalamiento politica = new PoliticaEscalamiento().nombre("politica-concurrente").repetirVeces(0).servicio(servicio);
            em.persist(politica);
            em.persist(new PasoEscalamiento().orden(1).esperaMinutos(5).canal(Canal.PUSH).politica(politica).destinatarioDirecto(user));
            Incidente incidente = new Incidente()
                .titulo("incidente-concurrente")
                .severidad(Severidad.SEV1)
                .estado(EstadoIncidente.ABIERTO)
                .detectadoEn(Instant.now().minus(Duration.ofMinutes(1)));
            incidente.addServicio(servicio);
            em.persist(incidente);

            incidenteId = incidente.getId();
            politicaId = politica.getId();
            servicioId = servicio.getId();
            equipoId = equipo.getId();
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(estado -> {
            for (String entidad : new String[] { "Notificacion", "EventoDeIncidente", "EjecucionDeEscalamiento" }) {
                em.createQuery("delete from " + entidad + " e where e.incidente.id = :id")
                    .setParameter("id", incidenteId)
                    .executeUpdate();
            }
            em.remove(em.find(Incidente.class, incidenteId));
            em.createQuery("delete from PasoEscalamiento p where p.politica.id = :id").setParameter("id", politicaId).executeUpdate();
            em.remove(em.find(PoliticaEscalamiento.class, politicaId));
            em.remove(em.find(Servicio.class, servicioId));
            em.remove(em.find(Equipo.class, equipoId));
        });
    }

    @Test
    @DisplayName("Dos corridas simultaneas sobre el mismo incidente notifican una sola vez")
    void dosCorridasSimultaneasNotificanUnaSolaVez() throws Exception {
        Instant ahora = Instant.now();
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Integer> corrida = () -> {
            largada.await();
            return escalamientoService.procesar(incidenteId, ahora).orElseThrow().ejecutadosAhora().size();
        };

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> una = hilos.submit(corrida);
            Future<Integer> otra = hilos.submit(corrida);
            largada.countDown();

            int escalonesEjecutados = una.get(30, TimeUnit.SECONDS) + otra.get(30, TimeUnit.SECONDS);

            assertThat(escalonesEjecutados).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }

        Long notificaciones = tx.execute(estado ->
            em
                .createQuery("select count(n) from Notificacion n where n.incidente.id = :id", Long.class)
                .setParameter("id", incidenteId)
                .getSingleResult()
        );
        assertThat(notificaciones).isEqualTo(1);
    }
}
