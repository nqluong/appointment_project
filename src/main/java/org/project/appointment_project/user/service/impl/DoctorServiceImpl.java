package org.project.appointment_project.user.service.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.common.redis.RedisCacheService;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.repository.DoctorRepository;
import org.project.appointment_project.user.service.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    DoctorRepository doctorRepository;
    PageMapper pageMapper;
    RedisCacheService redisCacheService;
    private static final String PROFILE_CACHE_PREFIX = "doctor:profile:";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getAllDoctors(Pageable pageable) {
        Page<DoctorResponse> doctorPage = doctorRepository.findAllDoctors(pageable);
        return pageMapper.toPageResponse(doctorPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctorsWithFilters(String specialtyName, Pageable pageable) {
        Page<DoctorResponse> doctorPage = doctorRepository.findDoctorsWithFilters(specialtyName, pageable);
        return pageMapper.toPageResponse(doctorPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> searchDoctors(String keyword, Pageable pageable) {
        Page<DoctorResponse> doctorPage = doctorRepository.searchDoctorsByKeyword(keyword, pageable);
        return pageMapper.toPageResponse(doctorPage);
    }

    @Override
    public DoctorResponse getDoctorById(UUID doctorId) {
        String cacheKey = PROFILE_CACHE_PREFIX + doctorId;
        Object cached = redisCacheService.get(cacheKey);

        if (cached != null) {
            log.debug("Doctor profile cache HIT for ID: {}", doctorId);
            return (DoctorResponse) cached;
        }

        log.debug("Doctor profile cache MISS for ID: {}", doctorId);

        DoctorResponse response = doctorRepository.findById(doctorId)
                .map(user -> {
                    var up = user.getUserProfile();
                    var mp = user.getMedicalProfile();
                    return new DoctorResponse(
                            user.getId(),
                            up.getFirstName(),
                            up.getLastName(),
                            up.getAvatarUrl(),
                            mp.getQualification(),
                            mp.getConsultationFee(),
                            mp.getYearsOfExperience(),
                            up.getGender() != null ? up.getGender().toString() : null,
                            up.getPhone(),
                            mp.getSpecialty() != null ? mp.getSpecialty().getName() : null,
                            user.isActive(),
                            mp.isDoctorApproved()
                    );
                })
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        redisCacheService.set(cacheKey, response, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
        return response;
    }
}
