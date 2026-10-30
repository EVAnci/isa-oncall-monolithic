package ar.edu.um.isa.oncall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.domain.EventoDeIncidente;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.Notificacion;
import ar.edu.um.isa.oncall.domain.PasoEscalamiento;
import ar.edu.um.isa.oncall.domain.PoliticaEscalamiento;
import ar.edu.um.isa.oncall.domain.Rotacion;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.Canal;
import ar.edu.um.isa.oncall.domain.enumeration.Criticidad;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoNotificacion;
import ar.edu.um.isa.oncall.domain.enumeration.ResultadoEscalamiento;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoEvento;
import ar.edu.um.isa.oncall.repository.EjecucionDeEscalamientoRepository;
import ar.edu.um.isa.oncall.repository.EscalamientoRepository;
import ar.edu.um.isa.oncall.repository.EventoDeIncidenteRepository;
import ar.edu.um.isa.oncall.repository.NotificacionRepository;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO.Estado;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * La cadena de escalamiento, sin base de datos y sin esperar: el tiempo lo pone cada test.
 *
 * Los repositorios mockeados guardan en listas, asi cada corrida ve lo que dejaron las
 * anteriores, igual que contra la base.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscalamientoServiceTest {

    private static final long INCIDENTE_ID = 1L;
    private static final Instant T0 = Instant.parse("2026-03-10T03:00:00Z");

    @Mock
    private EscalamientoRepository escalamientoRepository;

    @Mock
    private EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private EventoDeIncidenteRepository eventoDeIncidenteRepository;

    @Mock
    private GuardiaVigenteService guardiaVigenteService;

    private EscalamientoService servicio;

    private final List<EjecucionDeEscalamiento> ejecuciones = new ArrayList<>();
    private final List<Notificacion> notificaciones = new ArrayList<>();
    private final List<EventoDeIncidente> eventos = new ArrayList<>();
    private final List<PoliticaEscalamiento> politicas = new ArrayList<>();

    private Incidente incidente;
    private Servicio pagos;
    private Rotacion backend;
    private User ana;
    private User lider;
    private PoliticaEscalamiento politica;

    private long proximoId = 1;

    @BeforeEach
    void init() {
        servicio = new EscalamientoService(
            escalamientoRepository,
            ejecucionDeEscalamientoRepository,
            notificacionRepository,
            eventoDeIncidenteRepository,
            guardiaVigenteService
        );

        ana = usuario("ana");
        lider = usuario("lider");
        backend = new Rotacion().nombre("backend").zonaHoraria("UTC").activa(true);
        backend.setId(proximoId++);
        pagos = servicio("pagos-api", Criticidad.TIER1);

        // paso 1: push a la guardia, 5 min · paso 2: llamada a la guardia, 10 min · paso 3: mail al lider, 15 min
        politica = politica(pagos, 0);
        paso(politica, 1, 5, Canal.PUSH).rotacion(backend);
        paso(politica, 2, 10, Canal.LLAMADA).rotacion(backend);
        paso(politica, 3, 15, Canal.EMAIL).destinatarioDirecto(lider);

        incidente = new Incidente().titulo("Disco lleno").severidad(Severidad.SEV1).estado(EstadoIncidente.ABIERTO).detectadoEn(T0);
        incidente.setId(INCIDENTE_ID);
        incidente.addServicio(pagos);

        deGuardia(ana);

        when(escalamientoRepository.bloquear(INCIDENTE_ID)).thenAnswer(i -> Optional.of(incidente));
        when(escalamientoRepository.buscar(INCIDENTE_ID)).thenAnswer(i -> Optional.of(incidente));
        when(escalamientoRepository.buscarHistorial(INCIDENTE_ID)).thenAnswer(i -> new ArrayList<>(ejecuciones));
        when(escalamientoRepository.buscarPoliticasDeServicios(any())).thenAnswer(i -> politicas);
        when(ejecucionDeEscalamientoRepository.save(any())).thenAnswer(i -> {
            EjecucionDeEscalamiento ejecucion = i.getArgument(0);
            ejecucion.setId(proximoId++);
            ejecuciones.add(ejecucion);
            return ejecucion;
        });
        when(notificacionRepository.save(any())).thenAnswer(i -> {
            notificaciones.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(eventoDeIncidenteRepository.save(any())).thenAnswer(i -> {
            eventos.add(i.getArgument(0));
            return i.getArgument(0);
        });
    }

    // ---------------------------------------------------------------------
    // El camino feliz de la cadena
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("La primera corrida ejecuta el paso 1 en el acto: avisa a quien esta de guardia")
    void laPrimeraCorridaEjecutaElPaso1() {
        EstadoDeEscalamientoDTO estado = procesar(T0);

        assertThat(notificaciones).hasSize(1);
        Notificacion notificacion = notificaciones.get(0);
        assertThat(notificacion.getDestinatario()).isEqualTo(ana);
        assertThat(notificacion.getCanal()).isEqualTo(Canal.PUSH);
        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(notificacion.getDestino()).isEqualTo("ana@um.edu.ar");

        assertThat(estado.estado()).isEqualTo(Estado.EN_CURSO);
        assertThat(estado.nivel()).isEqualTo(1);
        assertThat(estado.vuelta()).isZero();
        assertThat(estado.proximoEscalamientoEn()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
        assertThat(estado.ejecutadosAhora()).hasSize(1);

        assertThat(eventos)
            .singleElement()
            .satisfies(evento -> {
                assertThat(evento.getTipo()).isEqualTo(TipoEvento.ESCALAMIENTO);
                assertThat(evento.getAutomatico()).isTrue();
            });
    }

    @Test
    @DisplayName("Idempotencia: si el proceso corre dos veces en el mismo instante, se notifica una sola vez")
    void correrDosVecesNoNotificaDosVeces() {
        procesar(T0);
        EstadoDeEscalamientoDTO segunda = procesar(T0);

        assertThat(notificaciones).hasSize(1);
        assertThat(ejecuciones).hasSize(1);
        assertThat(segunda.ejecutadosAhora()).isEmpty();
    }

    @Test
    @DisplayName("Antes de que venza la espera no se sube de nivel")
    void antesDeQueVenzaLaEsperaNoSeSube() {
        procesar(T0);
        procesar(T0.plus(Duration.ofMinutes(5)).minusSeconds(1));

        assertThat(notificaciones).hasSize(1);
    }

    @Test
    @DisplayName("Vencida la espera, sube al paso 2 con su propio canal")
    void vencidaLaEsperaSubeAlPaso2() {
        procesar(T0);
        EstadoDeEscalamientoDTO estado = procesar(T0.plus(Duration.ofMinutes(5)));

        assertThat(notificaciones).extracting(Notificacion::getCanal).containsExactly(Canal.PUSH, Canal.LLAMADA);
        assertThat(estado.nivel()).isEqualTo(2);
        assertThat(estado.proximoEscalamientoEn()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Si el proceso estuvo caido, al volver ejecuta un paso y espera: no dispara la rafaga acumulada")
    void despuesDeUnaCaidaNoHayRafaga() {
        procesar(T0);
        Instant mediaHoraDespues = T0.plus(Duration.ofMinutes(30));

        EstadoDeEscalamientoDTO estado = procesar(mediaHoraDespues);

        assertThat(notificaciones).hasSize(2);
        assertThat(estado.nivel()).isEqualTo(2);
        assertThat(estado.proximoEscalamientoEn()).isEqualTo(mediaHoraDespues.plus(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Un paso con espera 0 deja vencido al siguiente en la misma corrida")
    void unPasoSinEsperaEncadenaAlSiguiente() {
        politica
            .getPasos()
            .stream()
            .filter(p -> p.getOrden() == 1)
            .findFirst()
            .orElseThrow()
            .setEsperaMinutos(0);

        procesar(T0);

        assertThat(notificaciones).extracting(Notificacion::getCanal).containsExactly(Canal.PUSH, Canal.LLAMADA);
    }

    // ---------------------------------------------------------------------
    // A quien se avisa
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Un hueco en la guardia no frena la cadena: el paso queda sin destinatario y se sigue sin esperar")
    void unHuecoEnLaGuardiaPasaAlSiguienteSinEsperar() {
        deGuardia(null);

        EstadoDeEscalamientoDTO estado = procesar(T0);

        assertThat(ejecuciones)
            .extracting(EjecucionDeEscalamiento::getResultado)
            .containsExactly(
                ResultadoEscalamiento.SIN_DESTINATARIO,
                ResultadoEscalamiento.SIN_DESTINATARIO,
                ResultadoEscalamiento.NOTIFICADO
            );
        assertThat(notificaciones).singleElement().extracting(Notificacion::getDestinatario).isEqualTo(lider);
        assertThat(estado.nivel()).isEqualTo(3);
    }

    @Test
    @DisplayName("Si el paso tiene rotacion y destinatario directo, manda el destinatario directo")
    void elDestinatarioDirectoMandaSobreLaRotacion() {
        PasoEscalamiento paso1 = politica
            .getPasos()
            .stream()
            .filter(p -> p.getOrden() == 1)
            .findFirst()
            .orElseThrow();
        paso1.setDestinatarioDirecto(lider);

        procesar(T0);

        assertThat(notificaciones).singleElement().extracting(Notificacion::getDestinatario).isEqualTo(lider);
        assertThat(ejecuciones.get(0).getDetalle()).contains("manda el destinatario directo");
    }

    @Test
    @DisplayName("Un usuario desactivado no recibe avisos: el paso queda sin destinatario")
    void unUsuarioDesactivadoNoRecibeAvisos() {
        ana.setActivated(false);

        procesar(T0);

        assertThat(ejecuciones.get(0).getResultado()).isEqualTo(ResultadoEscalamiento.SIN_DESTINATARIO);
        assertThat(ejecuciones.get(0).getDetalle()).contains("no esta activo");
    }

    @Test
    @DisplayName("Con varios servicios afectados, se usa la politica del mas critico")
    void seUsaLaPoliticaDelServicioMasCritico() {
        Servicio reportes = servicio("reportes", Criticidad.TIER3);
        PoliticaEscalamiento deReportes = politica(reportes, 0);
        paso(deReportes, 1, 5, Canal.SMS).destinatarioDirecto(lider);
        incidente.addServicio(reportes);

        EstadoDeEscalamientoDTO estado = procesar(T0);

        assertThat(estado.politicaId()).isEqualTo(politica.getId());
        assertThat(notificaciones).singleElement().extracting(Notificacion::getCanal).isEqualTo(Canal.PUSH);
    }

    // ---------------------------------------------------------------------
    // Fin de la cadena
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Se acaban los niveles: queda AGOTADO, se deja asentado una vez y no se notifica mas")
    void alAcabarseLosNivelesQuedaAgotado() {
        recorrerLaPoliticaUnaVez();
        EstadoDeEscalamientoDTO estado = procesar(T0.plus(Duration.ofMinutes(30)));
        procesar(T0.plus(Duration.ofHours(5)));

        assertThat(estado.estado()).isEqualTo(Estado.AGOTADO);
        assertThat(estado.proximoEscalamientoEn()).isNull();
        assertThat(notificaciones).hasSize(3);
        assertThat(ejecuciones)
            .filteredOn(e -> e.getResultado() == ResultadoEscalamiento.AGOTADO)
            .hasSize(1);
        assertThat(eventos.get(eventos.size() - 1).getDetalle()).startsWith("Escalamiento agotado");
    }

    @Test
    @DisplayName("No se declara agotado antes de que venza la espera del ultimo paso")
    void noSeAgotaAntesDeQueVenzaElUltimoPaso() {
        recorrerLaPoliticaUnaVez();

        EstadoDeEscalamientoDTO estado = procesar(T0.plus(Duration.ofMinutes(30)).minusSeconds(1));

        assertThat(estado.estado()).isEqualTo(Estado.EN_CURSO);
    }

    @Test
    @DisplayName("Con repetirVeces = 1, al terminar los pasos se vuelve a empezar desde el paso 1")
    void conRepeticionesSeVuelveAEmpezar() {
        politica.setRepetirVeces(1);
        recorrerLaPoliticaUnaVez();

        EstadoDeEscalamientoDTO estado = procesar(T0.plus(Duration.ofMinutes(30)));

        assertThat(estado.vuelta()).isEqualTo(1);
        assertThat(estado.nivel()).isEqualTo(1);
        assertThat(notificaciones).hasSize(4);
        assertThat(notificaciones.get(3).getCanal()).isEqualTo(Canal.PUSH);
    }

    // ---------------------------------------------------------------------
    // Cortes
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Reconocido: la cadena se corta")
    void reconocidoCortaLaCadena() {
        procesar(T0);
        incidente.setReconocidoEn(T0.plus(Duration.ofMinutes(2)));

        EstadoDeEscalamientoDTO estado = procesar(T0.plus(Duration.ofMinutes(10)));

        assertThat(estado.estado()).isEqualTo(Estado.RECONOCIDO);
        assertThat(estado.proximoEscalamientoEn()).isNull();
        assertThat(notificaciones).hasSize(1);
    }

    @Test
    @DisplayName("Mitigado sin reconocer tambien corta: alguien lo esta atendiendo")
    void mitigadoCortaLaCadena() {
        incidente.setEstado(EstadoIncidente.MITIGADO);

        assertThat(procesar(T0).estado()).isEqualTo(Estado.RECONOCIDO);
        assertThat(notificaciones).isEmpty();
    }

    @Test
    @DisplayName("Resuelto o cerrado: la cadena se corta")
    void resueltoCortaLaCadena() {
        incidente.setEstado(EstadoIncidente.CERRADO);

        assertThat(procesar(T0).estado()).isEqualTo(Estado.RESUELTO);
        assertThat(notificaciones).isEmpty();
    }

    @Test
    @DisplayName("Silenciado: la cadena se pausa, y al vencer el silencio sigue donde estaba")
    void silenciadoPausaYDespuesSigue() {
        procesar(T0);
        incidente.setSilenciadoHasta(T0.plus(Duration.ofMinutes(60)));

        EstadoDeEscalamientoDTO durante = procesar(T0.plus(Duration.ofMinutes(10)));
        EstadoDeEscalamientoDTO despues = procesar(T0.plus(Duration.ofMinutes(61)));

        assertThat(durante.estado()).isEqualTo(Estado.SILENCIADO);
        assertThat(durante.proximoEscalamientoEn()).isEqualTo(T0.plus(Duration.ofMinutes(60)));
        assertThat(despues.estado()).isEqualTo(Estado.EN_CURSO);
        assertThat(despues.nivel()).isEqualTo(2);
        assertThat(notificaciones).hasSize(2);
    }

    @Test
    @DisplayName("Sin politica: se deja asentado una sola vez, y no se notifica a nadie")
    void sinPoliticaSeAsientaUnaVez() {
        politicas.clear();

        EstadoDeEscalamientoDTO primera = procesar(T0);
        procesar(T0.plus(Duration.ofMinutes(10)));

        assertThat(primera.estado()).isEqualTo(Estado.SIN_POLITICA);
        assertThat(ejecuciones)
            .singleElement()
            .extracting(EjecucionDeEscalamiento::getResultado)
            .isEqualTo(ResultadoEscalamiento.SIN_POLITICA);
        assertThat(notificaciones).isEmpty();
    }

    @Test
    @DisplayName("Una politica sin pasos cuenta como si no hubiera politica")
    void unaPoliticaSinPasosNoCuenta() {
        politica.getPasos().clear();

        assertThat(procesar(T0).estado()).isEqualTo(Estado.SIN_POLITICA);
    }

    @Test
    @DisplayName("Un incidente detectado en el futuro todavia no escala")
    void detectadoEnElFuturoNoEscala() {
        EstadoDeEscalamientoDTO estado = procesar(T0.minusSeconds(60));

        assertThat(estado.estado()).isEqualTo(Estado.PENDIENTE);
        assertThat(notificaciones).isEmpty();
    }

    @Test
    @DisplayName("Consultar el estado no ejecuta nada")
    void consultarNoEjecutaNada() {
        EstadoDeEscalamientoDTO estado = servicio.consultar(INCIDENTE_ID, T0).orElseThrow();

        assertThat(estado.estado()).isEqualTo(Estado.EN_CURSO);
        assertThat(estado.proximoEscalamientoEn()).isEqualTo(T0);
        assertThat(notificaciones).isEmpty();
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    @DisplayName("Un incidente que no existe da vacio")
    void unIncidenteQueNoExisteDaVacio() {
        when(escalamientoRepository.bloquear(99L)).thenReturn(Optional.empty());

        assertThat(servicio.procesar(99L, T0)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Siguiente escalon
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Si la politica se edita en medio del incidente, la cadena sigue desde el nivel en el que estaba")
    void siLaPoliticaSeEditaSigueDesdeElNivel() {
        PoliticaEscalamiento editada = politica(pagos, 0);
        PasoEscalamiento paso1 = paso(editada, 1, 5, Canal.PUSH);
        PasoEscalamiento paso3 = paso(editada, 3, 5, Canal.EMAIL);
        EjecucionDeEscalamiento enElNivel2 = new EjecucionDeEscalamiento().vuelta(0).nivel(2);

        Optional<EscalamientoService.Siguiente> siguiente = EscalamientoService.siguiente(
            Optional.of(enElNivel2),
            List.of(paso1, paso3),
            0
        );

        assertThat(siguiente).isPresent();
        assertThat(siguiente.orElseThrow().paso()).isEqualTo(paso3);
        assertThat(siguiente.orElseThrow().vuelta()).isZero();
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private EstadoDeEscalamientoDTO procesar(Instant ahora) {
        return servicio.procesar(INCIDENTE_ID, ahora).orElseThrow();
    }

    /** Pasos 1, 2 y 3 a los 0, 5 y 15 minutos. El ultimo vence a los 30. */
    private void recorrerLaPoliticaUnaVez() {
        procesar(T0);
        procesar(T0.plus(Duration.ofMinutes(5)));
        procesar(T0.plus(Duration.ofMinutes(15)));
        assertThat(notificaciones).hasSize(3);
    }

    private void deGuardia(User usuario) {
        GuardiaVigenteDTO guardia = new GuardiaVigenteDTO(
            backend.getId(),
            backend.getNombre(),
            "UTC",
            T0,
            null,
            usuario == null ? GuardiaVigenteDTO.Cobertura.SIN_COBERTURA : GuardiaVigenteDTO.Cobertura.CUBIERTA,
            null,
            null,
            usuario == null ? "Ningun turno cubre el instante." : "De guardia: " + usuario.getLogin() + "."
        );
        when(guardiaVigenteService.resolver(eq(backend), any(Instant.class))).thenReturn(
            new GuardiaVigenteService.Resolucion(guardia, usuario)
        );
    }

    private User usuario(String login) {
        User usuario = new User();
        usuario.setId(proximoId++);
        usuario.setLogin(login);
        usuario.setEmail(login + "@um.edu.ar");
        usuario.setActivated(true);
        return usuario;
    }

    private Servicio servicio(String nombre, Criticidad criticidad) {
        Servicio nuevo = new Servicio().nombre(nombre).criticidad(criticidad);
        nuevo.setId(proximoId++);
        return nuevo;
    }

    private PoliticaEscalamiento politica(Servicio servicioDeLaPolitica, int repetirVeces) {
        PoliticaEscalamiento nueva = new PoliticaEscalamiento()
            .nombre("politica de " + servicioDeLaPolitica.getNombre())
            .repetirVeces(repetirVeces)
            .servicio(servicioDeLaPolitica);
        nueva.setId(proximoId++);
        politicas.add(nueva);
        return nueva;
    }

    private PasoEscalamiento paso(PoliticaEscalamiento politicaDelPaso, int orden, int esperaMinutos, Canal canal) {
        PasoEscalamiento paso = new PasoEscalamiento().orden(orden).esperaMinutos(esperaMinutos).canal(canal);
        paso.setId(proximoId++);
        politicaDelPaso.addPaso(paso);
        return paso;
    }
}
