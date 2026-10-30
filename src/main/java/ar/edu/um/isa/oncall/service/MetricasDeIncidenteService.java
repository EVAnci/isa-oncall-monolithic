package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.ObjetivoDeServicio;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoObjetivo;
import ar.edu.um.isa.oncall.repository.IncidenteRepository;
import ar.edu.um.isa.oncall.repository.MetricasDeIncidenteRepository;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO.EstadoMedicion;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO.Medicion;
import ar.edu.um.isa.oncall.service.dto.ReporteDeMetricasDTO;
import ar.edu.um.isa.oncall.service.dto.ReporteDeMetricasDTO.Resumen;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla de negocio B · Calculo de MTTA / MTTR contra los SLOs.
 *
 * <p>Cada incidente se mide dos veces, siempre desde {@code detectadoEn}:</p>
 * <ul>
 *   <li><strong>Tiempo hasta reconocer</strong> (la base del MTTA), hasta {@code reconocidoEn}.</li>
 *   <li><strong>Tiempo hasta resolver</strong> (la base del MTTR), hasta {@code resueltoEn}.
 *       {@code mitigadoEn} no cuenta como resolucion: mitigar es dejar de sangrar, no cerrar.</li>
 * </ul>
 *
 * <p>Cada medicion se compara contra el {@code ObjetivoDeServicio} del tipo correspondiente
 * cuya {@code severidadAplicable} coincide con la del incidente. Como un incidente puede
 * degradar varios servicios, se toma el objetivo <em>mas exigente</em> entre todos ellos: si
 * se cayo un servicio TIER1 junto con uno TIER3, la vara la pone el TIER1.</p>
 *
 * <p>Casos borde:</p>
 * <ul>
 *   <li>Un incidente mitigado o resuelto sin {@code reconocidoEn} se da por reconocido en ese
 *       momento: alguien lo atendio, aunque no apretara el boton. La medicion queda marcada
 *       como {@code implicito}.</li>
 *   <li>Un incidente todavia abierto cuyo objetivo ya vencio es {@code INCUMPLIDO}, no
 *       {@code EN_CURSO}: ya no hay forma de que lo cumpla.</li>
 *   <li>Marcas que no cierran (resuelto antes de detectado, o terminado sin marca de
 *       resolucion) dan {@code INCONSISTENTE} y no entran en los promedios.</li>
 *   <li>Sin objetivo para esa severidad, el tiempo se mide pero no se juzga.</li>
 * </ul>
 *
 * <p>El veredicto se persiste en {@code Incidente.cumplioObjetivo} recien cuando el incidente
 * termina: antes, cualquier valor seria provisorio.</p>
 */
@Service
@Transactional
public class MetricasDeIncidenteService {

    private static final Logger LOG = LoggerFactory.getLogger(MetricasDeIncidenteService.class);

    /** Periodo del reporte cuando no se indica uno: los ultimos treinta dias. */
    public static final Duration PERIODO_POR_DEFECTO = Duration.ofDays(30);

    /** Los tipos de objetivo que se miden por incidente. {@code DISPONIBILIDAD} es de periodo, no de incidente. */
    static final Set<TipoObjetivo> TIPOS_MEDIDOS = EnumSet.of(TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO, TipoObjetivo.TIEMPO_DE_RESOLUCION);

    private static final Set<EstadoIncidente> ESTADOS_TERMINADOS = EnumSet.of(EstadoIncidente.RESUELTO, EstadoIncidente.CERRADO);

    private final MetricasDeIncidenteRepository metricasDeIncidenteRepository;
    private final IncidenteRepository incidenteRepository;

    public MetricasDeIncidenteService(
        MetricasDeIncidenteRepository metricasDeIncidenteRepository,
        IncidenteRepository incidenteRepository
    ) {
        this.metricasDeIncidenteRepository = metricasDeIncidenteRepository;
        this.incidenteRepository = incidenteRepository;
    }

    /**
     * Evalua un incidente sin modificar nada.
     *
     * @param incidenteId el incidente.
     * @param ahora contra que instante se miden los tiempos que siguen corriendo.
     * @return la evaluacion, o vacio si el incidente no existe.
     */
    @Transactional(readOnly = true)
    public Optional<EvaluacionDeIncidenteDTO> evaluar(Long incidenteId, Instant ahora) {
        return metricasDeIncidenteRepository
            .buscarConServicios(incidenteId)
            .map(incidente -> evaluar(incidente, objetivosDe(List.of(incidente)), ahora));
    }

    /**
     * Evalua un incidente y, si ya termino, guarda el veredicto en {@code cumplioObjetivo}.
     * Es idempotente: evaluar dos veces un incidente terminado da lo mismo.
     *
     * @param incidenteId el incidente.
     * @param ahora contra que instante se miden los tiempos que siguen corriendo.
     * @return la evaluacion, o vacio si el incidente no existe.
     */
    public Optional<EvaluacionDeIncidenteDTO> registrarCumplimiento(Long incidenteId, Instant ahora) {
        return metricasDeIncidenteRepository.buscarConServicios(incidenteId).map(incidente -> registrarCumplimiento(incidente, ahora));
    }

    /**
     * Igual que {@link #registrarCumplimiento(Long, Instant)}, para quien ya tiene el incidente.
     *
     * @param incidente el incidente, con sus servicios accesibles.
     * @param ahora contra que instante se miden los tiempos que siguen corriendo.
     * @return la evaluacion.
     */
    public EvaluacionDeIncidenteDTO registrarCumplimiento(Incidente incidente, Instant ahora) {
        EvaluacionDeIncidenteDTO evaluacion = evaluar(incidente, objetivosDe(List.of(incidente)), ahora);
        if (estaTerminado(incidente)) {
            incidente.setCumplioObjetivo(evaluacion.cumplioObjetivo());
            incidenteRepository.save(incidente);
            LOG.debug("Incidente {}: cumplioObjetivo = {}", incidente.getId(), evaluacion.cumplioObjetivo());
        }
        return evaluacion;
    }

    /**
     * MTTA, MTTR y cumplimiento de los incidentes detectados en un periodo.
     *
     * @param desde inicio del periodo, inclusive.
     * @param hasta fin del periodo, exclusive.
     * @param servicioId si no es {@code null}, solo los incidentes que afectaron a ese servicio.
     * @param ahora contra que instante se miden los incidentes que siguen abiertos.
     * @return el reporte.
     */
    @Transactional(readOnly = true)
    public ReporteDeMetricasDTO reporte(Instant desde, Instant hasta, Long servicioId, Instant ahora) {
        List<Incidente> incidentes =
            servicioId == null
                ? metricasDeIncidenteRepository.buscarDetectadosEntre(desde, hasta)
                : metricasDeIncidenteRepository.buscarDetectadosEntreSobreServicio(desde, hasta, servicioId);
        List<ObjetivoDeServicio> objetivos = objetivosDe(incidentes);

        List<EvaluacionDeIncidenteDTO> evaluaciones = incidentes
            .stream()
            .map(incidente -> evaluar(incidente, objetivos, ahora))
            .toList();

        List<Resumen> porSeveridad = Arrays.stream(Severidad.values())
            .map(severidad ->
                resumir(
                    severidad,
                    evaluaciones
                        .stream()
                        .filter(evaluacion -> evaluacion.severidad() == severidad)
                        .toList()
                )
            )
            .filter(resumen -> resumen.incidentes() > 0)
            .toList();

        return new ReporteDeMetricasDTO(desde, hasta, servicioId, resumir(null, evaluaciones), porSeveridad);
    }

    /**
     * La regla propiamente dicha, sin base de datos: un incidente, los objetivos candidatos y
     * el reloj.
     */
    EvaluacionDeIncidenteDTO evaluar(Incidente incidente, Collection<ObjetivoDeServicio> objetivos, Instant ahora) {
        Set<Long> servicioIds = incidente.getServicios().stream().map(Servicio::getId).collect(Collectors.toSet());
        List<ObjetivoDeServicio> aplicables = objetivos
            .stream()
            .filter(objetivo -> objetivo.getServicio() != null && servicioIds.contains(objetivo.getServicio().getId()))
            .filter(objetivo -> objetivo.getSeveridadAplicable() == incidente.getSeveridad())
            .toList();

        boolean terminado = estaTerminado(incidente);
        Instant detectadoEn = incidente.getDetectadoEn();

        Instant reconocidoEn = incidente.getReconocidoEn();
        boolean reconocimientoImplicito = false;
        if (reconocidoEn == null) {
            reconocidoEn = primero(incidente.getMitigadoEn(), incidente.getResueltoEn());
            reconocimientoImplicito = reconocidoEn != null;
        }

        Medicion reconocimiento = medir(
            TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO,
            detectadoEn,
            reconocidoEn,
            reconocimientoImplicito,
            terminado && reconocidoEn == null,
            masExigente(aplicables, TipoObjetivo.TIEMPO_DE_RECONOCIMIENTO),
            ahora
        );
        Medicion resolucion = medir(
            TipoObjetivo.TIEMPO_DE_RESOLUCION,
            detectadoEn,
            incidente.getResueltoEn(),
            false,
            terminado && incidente.getResueltoEn() == null,
            masExigente(aplicables, TipoObjetivo.TIEMPO_DE_RESOLUCION),
            ahora
        );

        return new EvaluacionDeIncidenteDTO(
            incidente.getId(),
            incidente.getTitulo(),
            incidente.getSeveridad(),
            incidente.getEstado(),
            detectadoEn,
            ahora,
            reconocimiento,
            resolucion,
            veredicto(reconocimiento, resolucion)
        );
    }

    /**
     * Mide un tramo y lo juzga contra el objetivo, si hay.
     *
     * @param faltaMarca el incidente termino pero la marca que cierra el tramo no esta.
     */
    private Medicion medir(
        TipoObjetivo tipo,
        Instant desde,
        Instant hasta,
        boolean implicito,
        boolean faltaMarca,
        Optional<ObjetivoDeServicio> objetivo,
        Instant ahora
    ) {
        Integer minutosObjetivo = objetivo.map(ObjetivoDeServicio::getMinutosObjetivo).orElse(null);
        Long objetivoId = objetivo.map(ObjetivoDeServicio::getId).orElse(null);
        String servicio = objetivo.map(o -> o.getServicio().getNombre()).orElse(null);

        if (faltaMarca || (hasta != null && hasta.isBefore(desde))) {
            return new Medicion(tipo, EstadoMedicion.INCONSISTENTE, hasta, implicito, null, minutosObjetivo, objetivoId, servicio);
        }

        Long segundos = hasta == null ? null : Duration.between(desde, hasta).toSeconds();

        EstadoMedicion estado;
        if (objetivo.isEmpty()) {
            estado = EstadoMedicion.SIN_OBJETIVO;
        } else {
            Duration limite = Duration.ofMinutes(minutosObjetivo);
            if (hasta != null) {
                estado = Duration.between(desde, hasta).compareTo(limite) <= 0 ? EstadoMedicion.CUMPLIDO : EstadoMedicion.INCUMPLIDO;
            } else {
                estado = Duration.between(desde, ahora).compareTo(limite) > 0 ? EstadoMedicion.INCUMPLIDO : EstadoMedicion.EN_CURSO;
            }
        }
        return new Medicion(tipo, estado, hasta, implicito, segundos, minutosObjetivo, objetivoId, servicio);
    }

    /**
     * El veredicto global: incumplir cualquiera de los dos objetivos es incumplir.
     *
     * @return {@code false} si algo se incumplio, {@code true} si todo lo que tenia objetivo se
     *         cumplio, y {@code null} si los datos no cierran, si falta terminar algo o si no
     *         habia ningun objetivo contra el que medir.
     */
    static Boolean veredicto(Medicion... mediciones) {
        Set<EstadoMedicion> estados = Stream.of(mediciones)
            .map(Medicion::estado)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EstadoMedicion.class)));
        if (estados.contains(EstadoMedicion.INCONSISTENTE)) {
            return null;
        }
        if (estados.contains(EstadoMedicion.INCUMPLIDO)) {
            return false;
        }
        if (estados.contains(EstadoMedicion.EN_CURSO) || !estados.contains(EstadoMedicion.CUMPLIDO)) {
            return null;
        }
        return true;
    }

    static Resumen resumir(Severidad severidad, List<EvaluacionDeIncidenteDTO> evaluaciones) {
        List<Long> tiemposDeReconocimiento = segundosMedidos(evaluaciones, EvaluacionDeIncidenteDTO::reconocimiento);
        List<Long> tiemposDeResolucion = segundosMedidos(evaluaciones, EvaluacionDeIncidenteDTO::resolucion);
        long inconsistentes = evaluaciones
            .stream()
            .filter(
                evaluacion ->
                    evaluacion.reconocimiento().estado() == EstadoMedicion.INCONSISTENTE ||
                    evaluacion.resolucion().estado() == EstadoMedicion.INCONSISTENTE
            )
            .count();

        return new Resumen(
            severidad,
            evaluaciones.size(),
            tiemposDeReconocimiento.size(),
            tiemposDeResolucion.size(),
            inconsistentes,
            promedioEnMinutos(tiemposDeReconocimiento),
            promedioEnMinutos(tiemposDeResolucion),
            porcentajeDeCumplimiento(evaluaciones, EvaluacionDeIncidenteDTO::reconocimiento),
            porcentajeDeCumplimiento(evaluaciones, EvaluacionDeIncidenteDTO::resolucion)
        );
    }

    private List<ObjetivoDeServicio> objetivosDe(Collection<Incidente> incidentes) {
        Set<Long> servicioIds = incidentes
            .stream()
            .flatMap(incidente -> incidente.getServicios().stream())
            .map(Servicio::getId)
            .collect(Collectors.toSet());
        if (servicioIds.isEmpty()) {
            return List.of();
        }
        return metricasDeIncidenteRepository.buscarObjetivos(servicioIds, TIPOS_MEDIDOS);
    }

    private static Optional<ObjetivoDeServicio> masExigente(List<ObjetivoDeServicio> aplicables, TipoObjetivo tipo) {
        return aplicables
            .stream()
            .filter(objetivo -> objetivo.getTipo() == tipo)
            .min(
                Comparator.comparing(ObjetivoDeServicio::getMinutosObjetivo).thenComparing(
                    ObjetivoDeServicio::getId,
                    Comparator.nullsLast(Comparator.naturalOrder())
                )
            );
    }

    private static boolean estaTerminado(Incidente incidente) {
        return ESTADOS_TERMINADOS.contains(incidente.getEstado()) || incidente.getResueltoEn() != null;
    }

    private static Instant primero(Instant uno, Instant otro) {
        return Stream.of(uno, otro).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
    }

    private static List<Long> segundosMedidos(
        List<EvaluacionDeIncidenteDTO> evaluaciones,
        Function<EvaluacionDeIncidenteDTO, Medicion> medicion
    ) {
        return evaluaciones.stream().map(medicion).map(Medicion::segundos).filter(Objects::nonNull).toList();
    }

    private static Double promedioEnMinutos(List<Long> segundos) {
        if (segundos.isEmpty()) {
            return null;
        }
        double promedio = segundos.stream().mapToLong(Long::longValue).average().orElseThrow() / 60.0;
        return redondear(promedio);
    }

    private static Double porcentajeDeCumplimiento(
        List<EvaluacionDeIncidenteDTO> evaluaciones,
        Function<EvaluacionDeIncidenteDTO, Medicion> medicion
    ) {
        long cumplidos = contar(evaluaciones, medicion, EstadoMedicion.CUMPLIDO);
        long juzgados = cumplidos + contar(evaluaciones, medicion, EstadoMedicion.INCUMPLIDO);
        return juzgados == 0 ? null : redondear((100.0 * cumplidos) / juzgados);
    }

    private static long contar(
        List<EvaluacionDeIncidenteDTO> evaluaciones,
        Function<EvaluacionDeIncidenteDTO, Medicion> medicion,
        EstadoMedicion estado
    ) {
        return evaluaciones
            .stream()
            .map(medicion)
            .filter(m -> m.estado() == estado)
            .count();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
