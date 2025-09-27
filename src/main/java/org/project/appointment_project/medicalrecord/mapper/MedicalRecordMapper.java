package org.project.appointment_project.medicalrecord.mapper;

import org.mapstruct.*;
import org.project.appointment_project.medicalrecord.dto.request.CreateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.model.MedicalRecord;

import java.time.LocalDate;
import java.time.Period;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MedicalRecordMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appointment", ignore = true) // Sẽ được set trong service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MedicalRecord toEntity(CreateMedicalRecordRequest request);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateMedicalRecordRequest request, @MappingTarget MedicalRecord medicalRecord);

}
