package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.mapper.DoctorMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.DoctorRepository;
import org.project.appointment_project.user.service.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    DoctorRepository doctorRepository;
    DoctorMapper doctorMapper;
    PageMapper pageMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getAllDoctors(Pageable pageable) {
        Page<User> doctorPage = doctorRepository.findAllApprovedDoctors(pageable);

        return pageMapper.toPageResponse(doctorPage, doctorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctorsWithFilters(String specialtyName, Pageable pageable) {
        Page<User> doctorPage = doctorRepository.findDoctorsWithFilters(specialtyName, pageable);

        return pageMapper.toPageResponse(doctorPage, doctorMapper::toResponse);
    }
}
