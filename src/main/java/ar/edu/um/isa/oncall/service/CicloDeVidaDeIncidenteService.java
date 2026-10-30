package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.EventoDeIncidente;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.TipoEvento;
import ar.edu.um.isa.oncall.repository.EscalamientoRepository;
import ar.edu.um.isa.oncall.repository.EventoDeIncidenteRepository;
import ar.edu.um.isa.oncall.repository.UserRepository;
import ar.edu.um.isa.oncall.service.dto.EstadoDeIncidenteDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las acciones de una persona sobre un incidente: reconocer, mitigar, resolver y silenciar.
 *
 * <p>El ABM generado permite escribir cualquier marca de tiempo a mano; esto es el camino
 * normal, el que respeta la maquina de estados, deja rastro en la linea de tiempo y corta el
 * escalamiento. Cada accion es idempotente: repetirla no cambia nada.</p>
 *
 * <p>La fila del incidente se bloquea igual que en {@link EscalamientoService}: si alguien
 * reconoce justo mientras corre el escalamiento, una de las dos operaciones espera a la otra.
 * O se notifico antes del reconocimiento, o no se notifica.</p>
 *
 * <p>Nunca se inventan marcas: mitigar o resolver sin haber reconocido deja
 * {@code reconocidoEn} vacio. La regla de MTTA lo da por reconocido implicitamente, pero el
 * dato crudo muestra lo que paso.</p>
 */
@Service
@Transactional
public class CicloDeVidaDeIncidenteService {

    private static final Logger LOG = LoggerFactory.getLogger(CicloDeVidaDeIncidenteService.class);

    /** Silencio maximo: un dia. Un silencio mas largo es un incidente abandonado. */
    public static final int MAX_MINUTOS_DE_SILENCIO = 1440;

    private static final Set<EstadoIncidente> ESTADOS_TERMINADOS = EnumSet.of(EstadoIncidente.RESUELTO, EstadoIncidente.CERRADO);

    private static final int MAX_DETALLE = 500;

    private final EscalamientoRepository escalamientoRepository;
    private final EventoDeIncidenteRepository eventoDeIncidenteRepository;
    private final UserRepository userRepository;
    private final MetricasDeIncidenteService metricasDeIncidenteService;

    public CicloDeVidaDeIncidenteService(
        EscalamientoRepository escalamientoRepository,
        EventoDeIncidenteRepository eventoDeIncidenteRepository,
        UserRepository userRepository,
        MetricasDeIncidenteService metricasDeIncidenteService
    ) {
        this.escalamientoRepository = escalamientoRepository;
        this.eventoDeIncidenteRepository = eventoDeIncidenteRepository;
        this.userRepository = userRepository;
        this.metricasDeIncidenteService = metricasDeIncidenteService;
    }

    /**
     * Reconocer: "lo tengo". Corta el escalamiento. Si el incidente no tenia comandante, quien
     * reconoce pasa a serlo.
     *
     * @param incidenteId el incidente.
     * @param login quien reconoce.
     * @param ahora el instante del reconocimiento.
     * @return como quedo, o vacio si el incidente no existe.
     * @throws TransicionInvalidaException si el incidente ya termino.
     */
    public Optional<EstadoDeIncidenteDTO> reconocer(Long incidenteId, String login, Instant ahora) {
        return escalamientoRepository.bloquear(incidenteId).map(incidente -> {
            exigirAbierto(incidente, "reconocer");
            boolean cambio = false;
            if (incidente.getReconocidoEn() == null) {
                incidente.setReconocidoEn(ahora);
                cambio = true;
            }
            if (incidente.getEstado() == EstadoIncidente.ABIERTO) {
                incidente.setEstado(EstadoIncidente.RECONOCIDO);
                cambio = true;
            }
            if (incidente.getComandante() == null && login != null) {
                userRepository.findOneByLogin(login).ifPresent(incidente::setComandante);
            }
            String mensaje = cambio ? "Reconocido por " + login + ". El escalamiento se corta." : "Ya estaba reconocido: no cambia nada.";
            if (cambio) {
                registrar(incidente, TipoEvento.CAMBIO_DE_ESTADO, mensaje, ahora);
            }
            return aEstado(incidente, cambio, mensaje);
        });
    }

    /**
     * Mitigar: el impacto se corto, aunque la causa siga ahi. Tambien corta el escalamiento.
     *
     * @param incidenteId el incidente.
     * @param login quien mitiga.
     * @param ahora el instante de la mitigacion.
     * @return como quedo, o vacio si el incidente no existe.
     * @throws TransicionInvalidaException si el incidente ya termino.
     */
    public Optional<EstadoDeIncidenteDTO> mitigar(Long incidenteId, String login, Instant ahora) {
        return escalamientoRepository.bloquear(incidenteId).map(incidente -> {
            exigirAbierto(incidente, "mitigar");
            boolean cambio = false;
            if (incidente.getMitigadoEn() == null) {
                incidente.setMitigadoEn(ahora);
                cambio = true;
            }
            if (incidente.getEstado() != EstadoIncidente.MITIGADO) {
                incidente.setEstado(EstadoIncidente.MITIGADO);
                cambio = true;
            }
            String mensaje = cambio ? "Mitigado por " + login + "." : "Ya estaba mitigado: no cambia nada.";
            if (cambio) {
                registrar(incidente, TipoEvento.CAMBIO_DE_ESTADO, mensaje, ahora);
            }
            return aEstado(incidente, cambio, mensaje);
        });
    }

    /**
     * Resolver: el incidente termino. Ademas de cortar el escalamiento, calcula y guarda si se
     * cumplieron los objetivos (regla B).
     *
     * @param incidenteId el incidente.
     * @param login quien resuelve.
     * @param ahora el instante de la resolucion.
     * @return como quedo, o vacio si el incidente no existe.
     */
    public Optional<EstadoDeIncidenteDTO> resolver(Long incidenteId, String login, Instant ahora) {
        return escalamientoRepository.bloquear(incidenteId).map(incidente -> {
            if (ESTADOS_TERMINADOS.contains(incidente.getEstado())) {
                return aEstado(incidente, false, "Ya estaba " + incidente.getEstado() + ": no cambia nada.");
            }
            if (incidente.getResueltoEn() == null) {
                incidente.setResueltoEn(ahora);
            }
            incidente.setEstado(EstadoIncidente.RESUELTO);
            metricasDeIncidenteService.registrarCumplimiento(incidente, ahora);

            String mensaje =
                "Resuelto por " +
                login +
                "." +
                (incidente.getCumplioObjetivo() == null
                    ? " No hay veredicto de objetivos."
                    : incidente.getCumplioObjetivo()
                      ? " Cumplio sus objetivos."
                      : " No cumplio sus objetivos.");
            registrar(incidente, TipoEvento.RESOLUCION, mensaje, ahora);
            return aEstado(incidente, true, mensaje);
        });
    }

    /**
     * Silenciar: pone en pausa el escalamiento. Al vencer, la cadena sigue donde estaba.
     *
     * @param incidenteId el incidente.
     * @param minutos cuanto dura el silencio; {@code 0} levanta un silencio vigente.
     * @param login quien silencia.
     * @param ahora el instante del pedido.
     * @return como quedo, o vacio si el incidente no existe.
     * @throws TransicionInvalidaException si el incidente ya termino.
     * @throws IllegalArgumentException si los minutos estan fuera de rango.
     */
    public Optional<EstadoDeIncidenteDTO> silenciar(Long incidenteId, int minutos, String login, Instant ahora) {
        if (minutos < 0 || minutos > MAX_MINUTOS_DE_SILENCIO) {
            throw new IllegalArgumentException("Los minutos de silencio van de 0 a " + MAX_MINUTOS_DE_SILENCIO + ", y son " + minutos);
        }
        return escalamientoRepository.bloquear(incidenteId).map(incidente -> {
            exigirAbierto(incidente, "silenciar");
            String mensaje;
            if (minutos == 0) {
                incidente.setSilenciadoHasta(null);
                mensaje = "Silencio levantado por " + login + ". El escalamiento sigue en la proxima corrida.";
            } else {
                Instant hasta = ahora.plus(Duration.ofMinutes(minutos));
                incidente.setSilenciadoHasta(hasta);
                mensaje = "Silenciado por " + login + " durante " + minutos + " minutos, hasta " + hasta + ".";
            }
            registrar(incidente, TipoEvento.NOTA, mensaje, ahora);
            return aEstado(incidente, true, mensaje);
        });
    }

    private void exigirAbierto(Incidente incidente, String accion) {
        if (ESTADOS_TERMINADOS.contains(incidente.getEstado())) {
            throw new TransicionInvalidaException(incidente.getId(), incidente.getEstado(), accion);
        }
    }

    private void registrar(Incidente incidente, TipoEvento tipo, String detalle, Instant ocurridoEn) {
        LOG.debug("Incidente {}: {}", incidente.getId(), detalle);
        eventoDeIncidenteRepository.save(
            new EventoDeIncidente()
                .tipo(tipo)
                .detalle(detalle.length() <= MAX_DETALLE ? detalle : detalle.substring(0, MAX_DETALLE - 3) + "...")
                .ocurridoEn(ocurridoEn)
                .automatico(false)
                .incidente(incidente)
        );
    }

    private static EstadoDeIncidenteDTO aEstado(Incidente incidente, boolean cambio, String mensaje) {
        return new EstadoDeIncidenteDTO(
            incidente.getId(),
            incidente.getEstado(),
            incidente.getDetectadoEn(),
            incidente.getReconocidoEn(),
            incidente.getMitigadoEn(),
            incidente.getResueltoEn(),
            incidente.getSilenciadoHasta(),
            incidente.getComandante() != null ? incidente.getComandante().getLogin() : null,
            incidente.getCumplioObjetivo(),
            cambio,
            mensaje
        );
    }
}
