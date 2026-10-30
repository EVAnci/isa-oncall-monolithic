package ar.edu.um.isa.oncall.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.ObjetivoDeServicio;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.enumeration.Criticidad;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoObjetivo;
import ar.edu.um.isa.oncall.repository.IncidenteRepository;
import ar.edu.um.isa.oncall.repository.MetricasDeIncidenteRepository;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO.EstadoMedicion;
import ar.edu.um.isa.oncall.service.dto.ReporteDeMetricasDTO;
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

/**
 * La regla de MTTA / MTTR, probada sin base de datos: son cuentas sobre marcas de tiempo.
 */
@ExtendWith(MockitoExtension.class)
class MetricasDeIncidenteServiceTest {

    private static final Instant DETECTADO = Instant.parse("2026-03-10T03:00:00Z");

    @Mock
    private MetricasDeIncidenteRepository metricasDeIncidenteRepository;

    @Mock
    private IncidenteRepository incidenteRepository;

    private MetricasDeIncidenteService servicio;

    private Servicio pagos;

    private final List<ObjetivoDeServicio> objetivos = new ArrayList<>();

    private long proximoId = 1;

    @BeforeEach
    void init() {
        servicio = new MetricasDeIncidenteService(metricasDeIncidenteRepository, incidenteRepository);
        pagos = servicio("pagos-api", Criticidad.TIER1);
        // SEV1 de pagos: 5 minutos para reconocer, 60 para resolver
        objetivo(pagos, TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO, Severidad.SEV1, 5);
        objetivo(pagos, TipoObjetivo.TIEMPO_DE_RESOLUCION, Severidad.SEV1, 60);
    }

    // ---------------------------------------------------------------------
    // Una medicion contra su objetivo
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Reconocido y resuelto dentro de objetivo: cumple")
    void dentroDeObjetivoCumple() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(4)).resueltoEn(minutos(50)).estado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.CUMPLIDO);
        assertThat(evaluacion.reconocimiento().segundos()).isEqualTo(240);
        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.CUMPLIDO);
        assertThat(evaluacion.resolucion().segundos()).isEqualTo(3000);
        assertThat(evaluacion.cumplioObjetivo()).isTrue();
    }

    @Test
    @DisplayName("Justo en el limite cuenta como cumplido")
    void justoEnElLimiteCumple() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(5));

        assertThat(evaluar(incidente, minutos(6)).reconocimiento().estado()).isEqualTo(EstadoMedicion.CUMPLIDO);
    }

    @Test
    @DisplayName("Un segundo pasado el limite ya es incumplido")
    void unSegundoPasadoElLimiteIncumple() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(5).plusSeconds(1));

        assertThat(evaluar(incidente, minutos(6)).reconocimiento().estado()).isEqualTo(EstadoMedicion.INCUMPLIDO);
    }

    @Test
    @DisplayName("Reconocer tarde alcanza para incumplir, aunque se resuelva a tiempo")
    void reconocerTardeIncumpleAunqueSeResuelvaATiempo() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(10)).resueltoEn(minutos(30)).estado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.INCUMPLIDO);
        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.CUMPLIDO);
        assertThat(evaluacion.cumplioObjetivo()).isFalse();
    }

    // ---------------------------------------------------------------------
    // Incidentes abiertos
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Abierto y dentro de plazo: en curso, sin veredicto")
    void abiertoDentroDePlazoEstaEnCurso() {
        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente(pagos), minutos(3));

        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.EN_CURSO);
        assertThat(evaluacion.reconocimiento().segundos()).isNull();
        assertThat(evaluacion.cumplioObjetivo()).isNull();
    }

    @Test
    @DisplayName("Abierto con el objetivo ya vencido: incumplido, aunque todavia no se haya reconocido")
    void abiertoConObjetivoVencidoYaIncumple() {
        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente(pagos), minutos(7));

        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.INCUMPLIDO);
        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.EN_CURSO);
        assertThat(evaluacion.cumplioObjetivo()).isFalse();
    }

    // ---------------------------------------------------------------------
    // Casos borde
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Resuelto sin reconocer: se da por reconocido al resolverse, y queda marcado como implicito")
    void resueltoSinReconocerSeDaPorReconocidoAlResolverse() {
        Incidente incidente = incidente(pagos).resueltoEn(minutos(3)).estado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.reconocimiento().implicito()).isTrue();
        assertThat(evaluacion.reconocimiento().hasta()).isEqualTo(minutos(3));
        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.CUMPLIDO);
    }

    @Test
    @DisplayName("Mitigado sin reconocer: el reconocimiento implicito es la mitigacion, que es anterior a la resolucion")
    void mitigadoSinReconocerTomaLaMitigacion() {
        Incidente incidente = incidente(pagos).mitigadoEn(minutos(8)).resueltoEn(minutos(40)).estado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.reconocimiento().hasta()).isEqualTo(minutos(8));
        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.INCUMPLIDO);
    }

    @Test
    @DisplayName("Mitigar no es resolver: el tiempo de resolucion sigue corriendo")
    void mitigarNoEsResolver() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(1)).mitigadoEn(minutos(10)).estado(EstadoIncidente.MITIGADO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(30));

        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.EN_CURSO);
        assertThat(evaluacion.resolucion().hasta()).isNull();
    }

    @Test
    @DisplayName("Resuelto antes de detectado: inconsistente, sin veredicto")
    void resueltoAntesDeDetectadoEsInconsistente() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(1)).resueltoEn(minutos(-10)).estado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.INCONSISTENTE);
        assertThat(evaluacion.resolucion().segundos()).isNull();
        assertThat(evaluacion.cumplioObjetivo()).isNull();
    }

    @Test
    @DisplayName("Cerrado sin marca de resolucion: inconsistente")
    void cerradoSinResolucionEsInconsistente() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(1)).estado(EstadoIncidente.CERRADO);

        assertThat(evaluar(incidente, minutos(120)).resolucion().estado()).isEqualTo(EstadoMedicion.INCONSISTENTE);
    }

    @Test
    @DisplayName("Sin objetivo para la severidad: se mide pero no se juzga")
    void sinObjetivoSeMidePeroNoSeJuzga() {
        Incidente incidente = incidente(pagos).severidad(Severidad.SEV3).reconocidoEn(minutos(30)).resueltoEn(minutos(500));
        incidente.setEstado(EstadoIncidente.RESUELTO);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(600));

        assertThat(evaluacion.reconocimiento().estado()).isEqualTo(EstadoMedicion.SIN_OBJETIVO);
        assertThat(evaluacion.reconocimiento().segundos()).isEqualTo(1800);
        assertThat(evaluacion.cumplioObjetivo()).isNull();
    }

    @Test
    @DisplayName("Varios servicios afectados: manda el objetivo mas exigente")
    void conVariosServiciosMandaElObjetivoMasExigente() {
        Servicio reportes = servicio("reportes", Criticidad.TIER3);
        objetivo(reportes, TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO, Severidad.SEV1, 30);
        ObjetivoDeServicio exigente = objetivo(reportes, TipoObjetivo.TIEMPO_DE_RESOLUCION, Severidad.SEV1, 20);
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(4)).resueltoEn(minutos(25)).estado(EstadoIncidente.RESUELTO);
        incidente.addServicio(reportes);

        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, minutos(120));

        assertThat(evaluacion.reconocimiento().minutosObjetivo()).isEqualTo(5);
        assertThat(evaluacion.reconocimiento().servicioDelObjetivo()).isEqualTo("pagos-api");
        assertThat(evaluacion.resolucion().minutosObjetivo()).isEqualTo(20);
        assertThat(evaluacion.resolucion().objetivoId()).isEqualTo(exigente.getId());
        assertThat(evaluacion.resolucion().estado()).isEqualTo(EstadoMedicion.INCUMPLIDO);
    }

    @Test
    @DisplayName("Los objetivos de servicios que el incidente no afecto no cuentan")
    void losObjetivosDeOtrosServiciosNoCuentan() {
        Servicio otro = servicio("otro", Criticidad.TIER1);
        objetivo(otro, TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO, Severidad.SEV1, 1);
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(4));

        assertThat(evaluar(incidente, minutos(10)).reconocimiento().minutosObjetivo()).isEqualTo(5);
    }

    // ---------------------------------------------------------------------
    // Persistencia del veredicto
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("El veredicto se guarda cuando el incidente termino")
    void elVeredictoSeGuardaCuandoTermina() {
        Incidente incidente = incidente(pagos).reconocidoEn(minutos(2)).resueltoEn(minutos(20)).estado(EstadoIncidente.RESUELTO);
        when(metricasDeIncidenteRepository.buscarObjetivos(any(), any())).thenReturn(objetivos);

        servicio.registrarCumplimiento(incidente, minutos(30));

        assertThat(incidente.getCumplioObjetivo()).isTrue();
        verify(incidenteRepository).save(incidente);
    }

    @Test
    @DisplayName("Con el incidente abierto no se guarda nada: cualquier veredicto seria provisorio")
    void conElIncidenteAbiertoNoSeGuardaNada() {
        Incidente incidente = incidente(pagos);
        when(metricasDeIncidenteRepository.buscarObjetivos(any(), any())).thenReturn(objetivos);

        EvaluacionDeIncidenteDTO evaluacion = servicio.registrarCumplimiento(incidente, minutos(30));

        assertThat(evaluacion.cumplioObjetivo()).isFalse();
        assertThat(incidente.getCumplioObjetivo()).isNull();
        verify(incidenteRepository, never()).save(any());
    }

    // ---------------------------------------------------------------------
    // Reporte del periodo
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("El reporte promedia, abre por severidad y calcula el cumplimiento sobre los que tienen veredicto")
    void elReportePromediaYAbrePorSeveridad() {
        Instant ahora = minutos(600);
        List<Incidente> incidentes = List.of(
            // SEV1: reconocen en 2 y en 10 minutos (MTTA 6), resuelven en 30 y 50 (MTTR 40)
            incidente(pagos)
                .reconocidoEn(minutos(2))
                .resueltoEn(minutos(30))
                .estado(EstadoIncidente.RESUELTO),
            incidente(pagos).reconocidoEn(minutos(10)).resueltoEn(minutos(50)).estado(EstadoIncidente.RESUELTO),
            // SEV1 inconsistente: queda afuera de los promedios
            incidente(pagos)
                .reconocidoEn(minutos(1))
                .resueltoEn(minutos(-1))
                .estado(EstadoIncidente.RESUELTO),
            // SEV2 sin objetivos, todavia abierto
            incidente(pagos).severidad(Severidad.SEV2)
        );
        Instant desde = DETECTADO.minus(Duration.ofDays(1));
        when(metricasDeIncidenteRepository.buscarDetectadosEntre(desde, ahora)).thenReturn(incidentes);
        when(metricasDeIncidenteRepository.buscarObjetivos(any(), any())).thenReturn(objetivos);

        ReporteDeMetricasDTO reporte = servicio.reporte(desde, ahora, null, ahora);

        assertThat(reporte.total().incidentes()).isEqualTo(4);
        assertThat(reporte.total().inconsistentes()).isEqualTo(1);
        assertThat(reporte.porSeveridad())
            .extracting(ReporteDeMetricasDTO.Resumen::severidad)
            .containsExactly(Severidad.SEV1, Severidad.SEV2);

        ReporteDeMetricasDTO.Resumen sev1 = reporte.porSeveridad().get(0);
        assertThat(sev1.incidentes()).isEqualTo(3);
        assertThat(sev1.reconocidos()).isEqualTo(3);
        assertThat(sev1.resueltos()).isEqualTo(2);
        assertThat(sev1.mttaMinutos()).isEqualTo(4.3); // (2 + 10 + 1) / 3
        assertThat(sev1.mttrMinutos()).isEqualTo(40.0);
        assertThat(sev1.cumplimientoReconocimiento()).isEqualTo(66.7); // 2 de 3
        assertThat(sev1.cumplimientoResolucion()).isEqualTo(100.0); // 2 de 2, el inconsistente no juzga

        ReporteDeMetricasDTO.Resumen sev2 = reporte.porSeveridad().get(1);
        assertThat(sev2.mttaMinutos()).isNull();
        assertThat(sev2.cumplimientoReconocimiento()).isNull();
    }

    @Test
    @DisplayName("Un periodo sin incidentes da un reporte vacio, sin dividir por cero")
    void unPeriodoSinIncidentes() {
        when(metricasDeIncidenteRepository.buscarDetectadosEntre(any(), any())).thenReturn(List.of());

        ReporteDeMetricasDTO reporte = servicio.reporte(DETECTADO, minutos(60), null, minutos(60));

        assertThat(reporte.total().incidentes()).isZero();
        assertThat(reporte.total().mttaMinutos()).isNull();
        assertThat(reporte.porSeveridad()).isEmpty();
    }

    @Test
    @DisplayName("Un incidente que no existe da vacio")
    void unIncidenteQueNoExisteDaVacio() {
        when(metricasDeIncidenteRepository.buscarConServicios(99L)).thenReturn(Optional.empty());

        assertThat(servicio.evaluar(99L, DETECTADO)).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------

    private EvaluacionDeIncidenteDTO evaluar(Incidente incidente, Instant ahora) {
        return servicio.evaluar(incidente, objetivos, ahora);
    }

    private static Instant minutos(long minutos) {
        return DETECTADO.plus(Duration.ofMinutes(minutos));
    }

    private Incidente incidente(Servicio servicioAfectado) {
        Incidente incidente = new Incidente()
            .titulo("incidente " + proximoId)
            .severidad(Severidad.SEV1)
            .estado(EstadoIncidente.ABIERTO)
            .detectadoEn(DETECTADO);
        incidente.setId(proximoId++);
        incidente.addServicio(servicioAfectado);
        return incidente;
    }

    private Servicio servicio(String nombre, Criticidad criticidad) {
        Servicio nuevo = new Servicio().nombre(nombre).criticidad(criticidad);
        nuevo.setId(proximoId++);
        return nuevo;
    }

    private ObjetivoDeServicio objetivo(Servicio servicioDelObjetivo, TipoObjetivo tipo, Severidad severidad, int minutos) {
        ObjetivoDeServicio objetivo = new ObjetivoDeServicio()
            .tipo(tipo)
            .severidadAplicable(severidad)
            .minutosObjetivo(minutos)
            .servicio(servicioDelObjetivo);
        objetivo.setId(proximoId++);
        objetivos.add(objetivo);
        return objetivo;
    }
}
