package ar.edu.um.isa.oncall.service.mapper;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.PasoEscalamiento;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.service.dto.EjecucionDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.IncidenteDTO;
import ar.edu.um.isa.oncall.service.dto.PasoEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link EjecucionDeEscalamiento} and its DTO {@link EjecucionDeEscalamientoDTO}.
 */
@Mapper(componentModel = "spring")
public interface EjecucionDeEscalamientoMapper extends EntityMapper<EjecucionDeEscalamientoDTO, EjecucionDeEscalamiento> {
    @Mapping(target = "incidente", source = "incidente", qualifiedByName = "incidenteTitulo")
    @Mapping(target = "paso", source = "paso", qualifiedByName = "pasoEscalamientoOrden")
    @Mapping(target = "destinatario", source = "destinatario", qualifiedByName = "userLogin")
    EjecucionDeEscalamientoDTO toDto(EjecucionDeEscalamiento s);

    @Named("incidenteTitulo")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "titulo")
    IncidenteDTO toDtoIncidenteTitulo(Incidente incidente);

    @Named("pasoEscalamientoOrden")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "orden", source = "orden")
    PasoEscalamientoDTO toDtoPasoEscalamientoOrden(PasoEscalamiento pasoEscalamiento);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
