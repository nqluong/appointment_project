package org.project.appointment_project.user.mapper;

import org.mapstruct.*;
import org.project.appointment_project.user.dto.request.SpecialtyRequest;
import org.project.appointment_project.user.dto.request.SpecialtyUpdate;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.model.Specialty;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SpecialtyMapper {
    Specialty toEntity(SpecialtyRequest dto);

    @Mapping(target = "specialtyId", source = "id")
    SpecialtyResponse toResponseDto(Specialty entity);

    List<SpecialtyResponse> toResponseDtoList(List<Specialty> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(SpecialtyUpdate dto, @MappingTarget Specialty entity);
}
