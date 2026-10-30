package ar.edu.um.isa.oncall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.isa.oncall.domain.Rotacion;
import ar.edu.um.isa.oncall.domain.TurnoDeGuardia;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.RolDeGuardia;
import ar.edu.um.isa.oncall.domain.enumeration.TipoRotacion;
import ar.edu.um.isa.oncall.repository.GuardiaVigenteRepository;
import ar.edu.um.isa.oncall.repository.RotacionRepository;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO.Cobertura;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La regla de guardia vigente, sin base de datos. El repositorio devuelve los turnos que
 * "cubren" el instante filtrando una lista en memoria con el mismo criterio que la consulta.
 */
@ExtendWith(MockitoExtension.class)
class GuardiaVigenteServiceTest {

    private static final long ROTACION_ID = 3L;
    private static final Instant NUEVE = Instant.parse("2026-03-10T09:00:00Z");

    @Mock
    private GuardiaVigenteRepository guardiaVigenteRepository;

    @Mock
    private RotacionRepository rotacionRepository;

    private GuardiaVigenteService servicio;

    private Rotacion rotacion;

    private final List<TurnoDeGuardia> turnos = new ArrayList<>();

    private long proximoId = 1;

    @BeforeEach
    void init() {
        servicio = new GuardiaVigenteService(guardiaVigenteRepository, rotacionRepository);
        rotacion = new Rotacion().nombre("backend").tipo(TipoRotacion.SEMANAL).zonaHoraria("UTC").activa(true);
        rotacion.setId(ROTACION_ID);
    }

    // ---------------------------------------------------------------------
    // Cobertura simple y bordes del intervalo
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Un turno que cubre el instante define al responsable")
    void unTurnoQueCubreElInstante() {
        turno("ana", "08:00", "16:00");

        GuardiaVigenteService.Resolucion resolucion = resolver(NUEVE);

        assertThat(resolucion.guardia().cobertura()).isEqualTo(Cobertura.CUBIERTA);
        assertThat(resolucion.guardia().primario().login()).isEqualTo("ana");
        assertThat(resolucion.responsable().getLogin()).isEqualTo("ana");
        assertThat(resolucion.guardia().secundario()).isNull();
    }

    @Test
    @DisplayName("El intervalo es semiabierto: en el instante del relevo manda el turno que empieza")
    void enElRelevoMandaElTurnoQueEmpieza() {
        turno("ana", "01:00", "09:00");
        turno("beto", "09:00", "17:00");

        assertThat(resolver(NUEVE).guardia().primario().login()).isEqualTo("beto");
        assertThat(resolver(NUEVE.minusSeconds(1)).guardia().primario().login()).isEqualTo("ana");
    }

    @Test
    @DisplayName("Un hueco no es una excepcion: responde SIN_COBERTURA y nadie de guardia")
    void unHuecoRespondeSinCobertura() {
        turno("ana", "01:00", "08:00");
        turno("beto", "10:00", "17:00");

        GuardiaVigenteService.Resolucion resolucion = resolver(NUEVE);

        assertThat(resolucion.guardia().cobertura()).isEqualTo(Cobertura.SIN_COBERTURA);
        assertThat(resolucion.guardia().primario()).isNull();
        assertThat(resolucion.responsable()).isNull();
        assertThat(resolucion.guardia().motivo()).contains("Ningun turno cubre");
    }

    @Test
    @DisplayName("Una rotacion inactiva no responde por nadie, aunque tenga turnos")
    void unaRotacionInactivaNoRespondePorNadie() {
        rotacion.setActiva(false);

        GuardiaVigenteService.Resolucion resolucion = servicio.resolver(rotacion, NUEVE);

        assertThat(resolucion.guardia().cobertura()).isEqualTo(Cobertura.ROTACION_INACTIVA);
        assertThat(resolucion.responsable()).isNull();
        verify(guardiaVigenteRepository, never()).buscarTurnosQueCubren(anyLong(), any());
    }

    // ---------------------------------------------------------------------
    // Solapes y reemplazos
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Dos turnos solapados (uno termina 09:00, otro empieza 08:30): gana el que empezo despues")
    void enUnSolapeGanaElQueEmpezoDespues() {
        turno("ana", "01:00", "09:00");
        turno("beto", "08:30", "17:00");

        GuardiaVigenteDTO guardia = resolver(Instant.parse("2026-03-10T08:45:00Z")).guardia();

        assertThat(guardia.primario().login()).isEqualTo("beto");
        assertThat(guardia.motivo()).contains("Se solapan");
    }

    @Test
    @DisplayName("Un reemplazo pisa al turno regular, aunque el regular haya empezado despues")
    void unReemplazoPisaAlTurnoRegular() {
        TurnoDeGuardia reemplazo = turno("carla", "00:00", "23:00");
        reemplazo.setEsReemplazo(true);
        turno("ana", "08:00", "16:00");

        GuardiaVigenteDTO guardia = resolver(NUEVE).guardia();

        assertThat(guardia.primario().login()).isEqualTo("carla");
        assertThat(guardia.primario().esReemplazo()).isTrue();
        assertThat(guardia.motivo()).contains("reemplazo #" + reemplazo.getId() + " pisa");
    }

    @Test
    @DisplayName("Dos turnos identicos: gana el cargado ultimo")
    void anteUnEmpateExactoGanaElCargadoUltimo() {
        turno("ana", "08:00", "16:00");
        turno("beto", "08:00", "16:00");

        assertThat(resolver(NUEVE).guardia().primario().login()).isEqualTo("beto");
    }

    // ---------------------------------------------------------------------
    // Primario y secundario
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Primario y secundario simultaneos: responde el primario, con el secundario de respaldo")
    void primarioYSecundario() {
        turno("ana", "08:00", "16:00");
        turno("beto", "08:00", "16:00").setRol(RolDeGuardia.SECUNDARIO);

        GuardiaVigenteDTO guardia = resolver(NUEVE).guardia();

        assertThat(guardia.cobertura()).isEqualTo(Cobertura.CUBIERTA);
        assertThat(guardia.primario().login()).isEqualTo("ana");
        assertThat(guardia.secundario().login()).isEqualTo("beto");
    }

    @Test
    @DisplayName("Sin primario vigente, atiende el secundario")
    void sinPrimarioAtiendeElSecundario() {
        turno("ana", "01:00", "08:00");
        turno("beto", "08:00", "16:00").setRol(RolDeGuardia.SECUNDARIO);

        GuardiaVigenteService.Resolucion resolucion = resolver(NUEVE);

        assertThat(resolucion.guardia().cobertura()).isEqualTo(Cobertura.CUBIERTA_POR_SECUNDARIO);
        assertThat(resolucion.responsable().getLogin()).isEqualTo("beto");
        assertThat(resolucion.guardia().secundario()).isNull();
    }

    @Test
    @DisplayName("Un turno sin rol cargado es primario")
    void unTurnoSinRolEsPrimario() {
        turno("ana", "08:00", "16:00").setRol(null);

        GuardiaVigenteDTO guardia = resolver(NUEVE).guardia();

        assertThat(guardia.cobertura()).isEqualTo(Cobertura.CUBIERTA);
        assertThat(guardia.primario().rol()).isEqualTo(RolDeGuardia.PRIMARIO);
    }

    @Test
    @DisplayName("Si la misma persona es primaria y secundaria, no figura como su propio respaldo")
    void laMismaPersonaNoEsSuPropioRespaldo() {
        TurnoDeGuardia primario = turno("ana", "08:00", "16:00");
        TurnoDeGuardia secundario = turno("ana", "08:00", "16:00");
        secundario.setResponsable(primario.getResponsable());
        secundario.setRol(RolDeGuardia.SECUNDARIO);

        assertThat(resolver(NUEVE).guardia().secundario()).isNull();
    }

    // ---------------------------------------------------------------------
    // Zonas horarias y horario de verano
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("La hora local se expresa en la zona de la rotacion")
    void laHoraLocalSeExpresaEnLaZonaDeLaRotacion() {
        rotacion.setZonaHoraria("America/Argentina/Mendoza");
        turno("ana", "08:00", "16:00");

        GuardiaVigenteDTO guardia = resolver(NUEVE).guardia();

        assertThat(guardia.zonaHoraria()).isEqualTo("America/Argentina/Mendoza");
        assertThat(guardia.horaLocal().toString()).isEqualTo("2026-03-10T06:00-03:00");
    }

    @Test
    @DisplayName("Una zona horaria invalida no rompe la regla: se muestra en UTC y se avisa")
    void unaZonaInvalidaSeMuestraEnUtc() {
        rotacion.setZonaHoraria("Marte/Olympus");
        turno("ana", "08:00", "16:00");

        GuardiaVigenteDTO guardia = resolver(NUEVE).guardia();

        assertThat(guardia.zonaHoraria()).isEqualTo("Z");
        assertThat(guardia.primario().login()).isEqualTo("ana");
        assertThat(guardia.motivo()).contains("no es valida");
    }

    @Test
    @DisplayName("Horario de verano, salto hacia adelante: una hora local que no existe se corre hacia adelante")
    void unaHoraLocalQueNoExisteSeCorreHaciaAdelante() {
        // 2026-03-08 en Nueva York: a las 02:00 se pasa a las 03:00. Las 02:30 no existen.
        Instant instante = GuardiaVigenteService.aInstante(LocalDateTime.parse("2026-03-08T02:30:00"), ZoneId.of("America/New_York"));

        assertThat(instante).isEqualTo(Instant.parse("2026-03-08T07:30:00Z")); // 03:30 EDT
    }

    @Test
    @DisplayName("Horario de verano, atraso: una hora local que existe dos veces se toma en su primera ocurrencia")
    void unaHoraLocalRepetidaSeTomaEnSuPrimeraOcurrencia() {
        // 2026-11-01 en Nueva York: a las 02:00 se vuelve a la 01:00. Las 01:30 pasan dos veces.
        Instant instante = GuardiaVigenteService.aInstante(LocalDateTime.parse("2026-11-01T01:30:00"), ZoneId.of("America/New_York"));

        assertThat(instante).isEqualTo(Instant.parse("2026-11-01T05:30:00Z")); // 01:30 EDT, no EST
    }

    @Test
    @DisplayName("Un turno que atraviesa el cambio de hora dura lo que dura en tiempo real")
    void unTurnoQueAtraviesaElCambioDeHora() {
        rotacion.setZonaHoraria("America/New_York");
        // de 00:00 a 08:00 hora local del dia del salto: son 7 horas reales, no 8
        TurnoDeGuardia turno = turnoEntre("ana", Instant.parse("2026-03-08T05:00:00Z"), Instant.parse("2026-03-08T12:00:00Z"));

        GuardiaVigenteDTO guardia = resolver(Instant.parse("2026-03-08T11:59:59Z")).guardia();

        assertThat(guardia.primario().turnoId()).isEqualTo(turno.getId());
        assertThat(guardia.horaLocal().toString()).isEqualTo("2026-03-08T07:59:59-04:00");
    }

    @Test
    @DisplayName("Una rotacion que no existe da vacio, no una excepcion")
    void unaRotacionQueNoExisteDaVacio() {
        when(rotacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(servicio.resolver(99L, NUEVE)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private GuardiaVigenteService.Resolucion resolver(Instant instante) {
        when(guardiaVigenteRepository.buscarTurnosQueCubren(ROTACION_ID, instante)).thenReturn(
            turnos
                .stream()
                .filter(t -> !t.getDesde().isAfter(instante) && t.getHasta().isAfter(instante))
                .toList()
        );
        return servicio.resolver(rotacion, instante);
    }

    /** Un turno del 10 de marzo de 2026, en UTC, entre dos horas "HH:mm". */
    private TurnoDeGuardia turno(String login, String desde, String hasta) {
        return turnoEntre(login, Instant.parse("2026-03-10T" + desde + ":00Z"), Instant.parse("2026-03-10T" + hasta + ":00Z"));
    }

    private TurnoDeGuardia turnoEntre(String login, Instant desde, Instant hasta) {
        User usuario = new User();
        usuario.setId(proximoId * 100);
        usuario.setLogin(login);
        TurnoDeGuardia turno = new TurnoDeGuardia()
            .desde(desde)
            .hasta(hasta)
            .esReemplazo(false)
            .rol(RolDeGuardia.PRIMARIO)
            .rotacion(rotacion)
            .responsable(usuario);
        turno.setId(proximoId++);
        turnos.add(turno);
        return turno;
    }
}
