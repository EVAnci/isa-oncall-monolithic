package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.Rotacion;
import ar.edu.um.isa.oncall.domain.TurnoDeGuardia;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.RolDeGuardia;
import ar.edu.um.isa.oncall.repository.GuardiaVigenteRepository;
import ar.edu.um.isa.oncall.repository.RotacionRepository;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO.Cobertura;
import ar.edu.um.isa.oncall.service.dto.GuardiaVigenteDTO.TurnoVigente;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla de negocio C · Quien esta de guardia en un instante dado.
 *
 * <p>Dada una rotacion y un instante, devuelve la persona responsable. El criterio:</p>
 * <ol>
 *   <li>Un turno cubre un instante si {@code desde <= t < hasta}. El extremo derecho es
 *       abierto: en el relevo de las 09:00 ya manda el turno que empieza.</li>
 *   <li>Los turnos se separan por rol. Un turno sin rol cargado es {@code PRIMARIO}: es lo que
 *       eran todos antes de que existiera el campo.</li>
 *   <li>Dentro de un mismo rol, un reemplazo pisa a cualquier turno regular.</li>
 *   <li>Si aun asi quedan dos turnos solapados, gana el que empezo despues: en un solape de
 *       relevo (uno termina 09:00, otro empieza 08:30) la guardia ya la tomo quien entra.
 *       Empate exacto: gana el cargado ultimo.</li>
 *   <li>Si no hay primario pero si secundario, atiende el secundario. Es preferible despertar
 *       al respaldo que no despertar a nadie.</li>
 *   <li>Si no hay ningun turno, la respuesta es {@code SIN_COBERTURA}. Un hueco es un estado
 *       valido de la rotacion, no un error: quien pregunta decide que hacer con el.</li>
 * </ol>
 *
 * <p>Zonas horarias: los turnos se guardan como {@link Instant}, asi que la resolucion no
 * depende de la zona ni del horario de verano. La zona de la rotacion solo se usa para
 * mostrar la hora local y para interpretar una consulta hecha en hora local
 * ({@link #resolverEnHoraLocal}), que es donde el horario de verano si importa.</p>
 */
@Service
@Transactional(readOnly = true)
public class GuardiaVigenteService {

    private static final Logger LOG = LoggerFactory.getLogger(GuardiaVigenteService.class);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Orden de prioridad dentro de un rol: reemplazos primero, despues el que empezo ultimo. */
    static final Comparator<TurnoDeGuardia> PRIORIDAD = Comparator.comparing((TurnoDeGuardia turno) ->
        Boolean.TRUE.equals(turno.getEsReemplazo())
    )
        .reversed()
        .thenComparing(TurnoDeGuardia::getDesde, Comparator.reverseOrder())
        .thenComparing(TurnoDeGuardia::getId, Comparator.nullsFirst(Comparator.reverseOrder()));

    private final GuardiaVigenteRepository guardiaVigenteRepository;
    private final RotacionRepository rotacionRepository;

    public GuardiaVigenteService(GuardiaVigenteRepository guardiaVigenteRepository, RotacionRepository rotacionRepository) {
        this.guardiaVigenteRepository = guardiaVigenteRepository;
        this.rotacionRepository = rotacionRepository;
    }

    /**
     * Resultado completo de la regla: la respuesta para afuera y la persona para adentro.
     *
     * @param guardia la respuesta tal como la ve un cliente REST.
     * @param responsable el usuario que tiene que atender, o {@code null} si no hay cobertura.
     */
    public record Resolucion(GuardiaVigenteDTO guardia, User responsable) {}

    /**
     * Quien esta de guardia en la rotacion en el instante dado.
     *
     * @param rotacionId la rotacion.
     * @param instante el instante a resolver.
     * @return la guardia vigente, o vacio si la rotacion no existe.
     */
    public Optional<GuardiaVigenteDTO> resolver(Long rotacionId, Instant instante) {
        return rotacionRepository.findById(rotacionId).map(rotacion -> resolver(rotacion, instante).guardia());
    }

    /**
     * Igual que {@link #resolver(Long, Instant)}, pero el instante viene en la hora local de la
     * rotacion. Es lo que pregunta una persona: "quien esta de guardia el martes a las 3".
     *
     * <p>El horario de verano hace que algunas horas locales no existan y otras existan dos
     * veces. Se resuelve como {@link ZonedDateTime#ofLocal}: una hora que no existe (salto hacia
     * adelante) se corre hacia adelante tanto como dure el salto; una hora que existe dos veces
     * (atraso) se toma en su primera ocurrencia.</p>
     *
     * @param rotacionId la rotacion.
     * @param horaLocal la hora, sin zona, en la zona de la rotacion.
     * @return la guardia vigente, o vacio si la rotacion no existe.
     */
    public Optional<GuardiaVigenteDTO> resolverEnHoraLocal(Long rotacionId, LocalDateTime horaLocal) {
        return rotacionRepository
            .findById(rotacionId)
            .map(rotacion -> resolver(rotacion, aInstante(horaLocal, zonaDe(rotacion))).guardia());
    }

    /**
     * La regla propiamente dicha. Es lo que usa el escalamiento para saber a quien avisar.
     *
     * @param rotacion la rotacion.
     * @param instante el instante a resolver.
     * @return la respuesta y el usuario responsable.
     */
    public Resolucion resolver(Rotacion rotacion, Instant instante) {
        ZoneId zona = zonaDe(rotacion);
        String avisoZona = esZonaValida(rotacion.getZonaHoraria())
            ? ""
            : " La zona horaria '" + rotacion.getZonaHoraria() + "' no es valida: se muestra en UTC.";

        if (!Boolean.TRUE.equals(rotacion.getActiva())) {
            return sinResponsable(rotacion, instante, zona, Cobertura.ROTACION_INACTIVA, "La rotacion esta inactiva." + avisoZona);
        }

        List<TurnoDeGuardia> vigentes = guardiaVigenteRepository.buscarTurnosQueCubren(rotacion.getId(), instante);
        List<TurnoDeGuardia> primarios = delRol(vigentes, RolDeGuardia.PRIMARIO);
        List<TurnoDeGuardia> secundarios = delRol(vigentes, RolDeGuardia.SECUNDARIO);

        if (primarios.isEmpty() && secundarios.isEmpty()) {
            return sinResponsable(
                rotacion,
                instante,
                zona,
                Cobertura.SIN_COBERTURA,
                "Ningun turno cubre el " + hora(instante, zona) + "." + avisoZona
            );
        }

        StringBuilder motivo = new StringBuilder();
        Cobertura cobertura;
        TurnoDeGuardia primario;
        if (primarios.isEmpty()) {
            cobertura = Cobertura.CUBIERTA_POR_SECUNDARIO;
            primario = secundarios.remove(0);
            motivo.append("No hay turno primario vigente: atiende el secundario. ");
        } else {
            cobertura = Cobertura.CUBIERTA;
            primario = primarios.get(0);
            motivo.append(explicarEleccion(primarios));
        }
        motivo.append("De guardia: ").append(describir(primario, zona)).append('.');

        TurnoDeGuardia secundario = secundarios
            .stream()
            .filter(turno -> !mismaPersona(turno, primario))
            .findFirst()
            .orElse(null);
        if (secundario != null) {
            motivo.append(" Respaldo: ").append(describir(secundario, zona)).append('.');
        }
        motivo.append(avisoZona);

        LOG.debug("Guardia de la rotacion {} en {}: {}", rotacion.getId(), instante, primario.getResponsable().getLogin());

        GuardiaVigenteDTO guardia = new GuardiaVigenteDTO(
            rotacion.getId(),
            rotacion.getNombre(),
            zona.getId(),
            instante,
            instante.atZone(zona).toOffsetDateTime(),
            cobertura,
            aTurnoVigente(primario),
            secundario == null ? null : aTurnoVigente(secundario),
            motivo.toString().trim()
        );
        return new Resolucion(guardia, primario.getResponsable());
    }

    static Instant aInstante(LocalDateTime horaLocal, ZoneId zona) {
        return ZonedDateTime.ofLocal(horaLocal, zona, null).toInstant();
    }

    static ZoneId zonaDe(Rotacion rotacion) {
        return esZonaValida(rotacion.getZonaHoraria()) ? ZoneId.of(rotacion.getZonaHoraria()) : ZoneOffset.UTC;
    }

    private static boolean esZonaValida(String zonaHoraria) {
        if (zonaHoraria == null) {
            return false;
        }
        try {
            ZoneId.of(zonaHoraria);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private List<TurnoDeGuardia> delRol(List<TurnoDeGuardia> turnos, RolDeGuardia rol) {
        List<TurnoDeGuardia> delRol = new ArrayList<>(
            turnos
                .stream()
                .filter(turno -> turno.getResponsable() != null)
                .filter(turno -> Objects.requireNonNullElse(turno.getRol(), RolDeGuardia.PRIMARIO) == rol)
                .toList()
        );
        delRol.sort(PRIORIDAD);
        return delRol;
    }

    /**
     * Cuando hay mas de un primario vigente, deja escrito por que gano el que gano.
     */
    private String explicarEleccion(List<TurnoDeGuardia> primarios) {
        if (primarios.size() < 2) {
            return "";
        }
        TurnoDeGuardia ganador = primarios.get(0);
        TurnoDeGuardia segundo = primarios.get(1);
        if (Boolean.TRUE.equals(ganador.getEsReemplazo()) && !Boolean.TRUE.equals(segundo.getEsReemplazo())) {
            return "El reemplazo #" + ganador.getId() + " pisa al turno regular #" + segundo.getId() + ". ";
        }
        return "Se solapan los turnos #" + ganador.getId() + " y #" + segundo.getId() + ": gana el que empezo despues. ";
    }

    private Resolucion sinResponsable(Rotacion rotacion, Instant instante, ZoneId zona, Cobertura cobertura, String motivo) {
        GuardiaVigenteDTO guardia = new GuardiaVigenteDTO(
            rotacion.getId(),
            rotacion.getNombre(),
            zona.getId(),
            instante,
            instante.atZone(zona).toOffsetDateTime(),
            cobertura,
            null,
            null,
            motivo.trim()
        );
        return new Resolucion(guardia, null);
    }

    private static boolean mismaPersona(TurnoDeGuardia uno, TurnoDeGuardia otro) {
        return Objects.equals(uno.getResponsable().getId(), otro.getResponsable().getId());
    }

    private static TurnoVigente aTurnoVigente(TurnoDeGuardia turno) {
        return new TurnoVigente(
            turno.getId(),
            turno.getResponsable().getId(),
            turno.getResponsable().getLogin(),
            turno.getDesde(),
            turno.getHasta(),
            Objects.requireNonNullElse(turno.getRol(), RolDeGuardia.PRIMARIO),
            Boolean.TRUE.equals(turno.getEsReemplazo())
        );
    }

    private static String describir(TurnoDeGuardia turno, ZoneId zona) {
        return (
            turno.getResponsable().getLogin() +
            " (turno #" +
            turno.getId() +
            (Boolean.TRUE.equals(turno.getEsReemplazo()) ? ", reemplazo" : "") +
            ", " +
            hora(turno.getDesde(), zona) +
            " a " +
            hora(turno.getHasta(), zona) +
            ")"
        );
    }

    private static String hora(Instant instante, ZoneId zona) {
        return HORA.format(instante.atZone(zona)) + " " + zona.getId();
    }
}
