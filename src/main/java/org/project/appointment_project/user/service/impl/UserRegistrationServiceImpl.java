package org.project.appointment_project.user.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.BaseUserRegistrationRequest;
import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.request.PatientRegistrationRequest;
import org.project.appointment_project.user.dto.response.UserRegistrationResponse;
import org.project.appointment_project.user.mapper.UserRegistrationMapper;
import org.project.appointment_project.user.model.*;
import org.project.appointment_project.user.repository.RoleRepository;
import org.project.appointment_project.user.repository.SpecialtyRepository;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.repository.UserRoleRepositoryJdbcImpl;
import org.project.appointment_project.user.service.UserRegistrationService;
import org.project.appointment_project.user.service.UserRegistrationValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRegistrationServiceImpl implements UserRegistrationService {
    UserRepository userRepository;
    SpecialtyRepository specialtyRepository;
    UserRegistrationMapper userRegistrationMapper;
    UserRegistrationValidator userRegistrationValidator;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    UserRoleRepositoryJdbcImpl userRoleRepositoryJdbc;

    @Override
    @Transactional
    public UserRegistrationResponse registerPatient(PatientRegistrationRequest request) {
        userRegistrationValidator.validatePatientRegistration(request);

        try {
            User user = createUser(request);
            User savedUser = userRepository.save(user);

            UserProfile userProfile = createUserProfile(request, savedUser);
            MedicalProfile medicalProfile = createPatientMedicalProfile(request, savedUser);
            assignRole(savedUser, "PATIENT");
            return userRegistrationMapper.toRegistrationResponse(savedUser, userProfile, medicalProfile, "PATIENT");
        }catch (Exception e){
            log.error("Error during patient registration: {}", e.getMessage());
            throw new CustomException(ErrorCode.REGISTRATION_FAILED);
        }
    }

    @Override
    @Transactional
    public UserRegistrationResponse registerDoctor(DoctorRegistrationRequest request) {
        userRegistrationValidator.validateDoctorRegistration(request);

        try {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new CustomException(ErrorCode.SPECIALTY_NOT_FOUND, "Specialty not found with ID: " + request.getSpecialtyId()));


            User user = createUser(request);
            User savedUser = userRepository.save(user);

            UserProfile userProfile = createUserProfile(request, savedUser);

            MedicalProfile medicalProfile = createDoctorMedicalProfile(request, savedUser, specialty);
            assignRole(savedUser, "DOCTOR");
            log.info("Doctor registration completed successfully for user: {}", savedUser.getUsername());

            return userRegistrationMapper.toRegistrationResponse(
                    savedUser,
                    userProfile,
                    medicalProfile,
                    "DOCTOR"
            );

        } catch (Exception e) {
            log.error("Error during doctor registration: {}", e.getMessage());
           throw new CustomException(ErrorCode.REGISTRATION_FAILED);
        }
    }

    private User createUser(BaseUserRegistrationRequest request) {
        User user = userRegistrationMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return user;
    }

    private UserProfile createUserProfile(BaseUserRegistrationRequest request, User user) {
        UserProfile userProfile = userRegistrationMapper.toUserProfile(request);
        userProfile.setUser(user);
        user.setUserProfile(userProfile);
        return userProfile;
    }

    private MedicalProfile createPatientMedicalProfile(PatientRegistrationRequest request, User user) {
        MedicalProfile medicalProfile = userRegistrationMapper.toPatientMedicalProfile(request);
        medicalProfile.setUser(user);
        user.setMedicalProfile(medicalProfile);
        return medicalProfile;
    }

    private MedicalProfile createDoctorMedicalProfile(DoctorRegistrationRequest request, User user, Specialty specialty) {
        MedicalProfile medicalProfile = userRegistrationMapper.toDoctorMedicalProfile(request);
        medicalProfile.setUser(user);
        medicalProfile.setLicenseNumber(request.getLicenseNumber());
        medicalProfile.setSpecialty(specialty);
        medicalProfile.setQualification(request.getQualification());
        medicalProfile.setYearsOfExperience(request.getYearsOfExperience());
        medicalProfile.setConsultationFee(request.getConsultationFee());
        medicalProfile.setBio(request.getBio());
        user.setMedicalProfile(medicalProfile);
        return medicalProfile;
    }

    private void assignRole(User user, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new CustomException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + roleName));
//        UserRole userRole = UserRole.builder()
//                .user(user)
//                .role(role)
//                .assignedAt(LocalDateTime.now())
//                .isActive(true)
//                .build();
        userRoleRepositoryJdbc.assignRoleToUserOnRegistration(user.getId(), role.getId());
    }
}
