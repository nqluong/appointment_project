package org.project.appointment_project.schedule.service.impl;

import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;
import org.project.appointment_project.schedule.model.DoctorSchedule;
import org.project.appointment_project.schedule.service.DoctorSearchSpecificationService;
import org.project.appointment_project.schedule.service.SearchRequestValidator;
import org.project.appointment_project.user.model.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorSearchSpecificationServiceImpl implements DoctorSearchSpecificationService {
    SearchRequestValidator validator;

    @Override
    public Specification<User> buildDoctorSearchSpecification(DoctorSearchRequest request) {
        validator.validateSearchRequest(request);

        return (root, query, criteriaBuilder) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();

                // Luôn lọc user có role DOCTOR
                addDoctorRoleFilter(root, criteriaBuilder, predicates);

                // Chỉ join các bảng cần thiết dựa trên search criteria
                Join<User, MedicalProfile> medicalJoin = null;
                Join<User, UserProfile> userProfileJoin = null;
                Join<MedicalProfile, Specialty> specialtyJoin = null;
                Join<User, DoctorSchedule> scheduleJoin = null;

                // Join MedicalProfile nếu cần thiết
                if (needsMedicalProfileJoin(request)) {
                    medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                }

                // Join UserProfile nếu cần tìm theo tên
                if (StringUtils.hasText(request.getDoctorName())) {
                    userProfileJoin = root.join("userProfile", JoinType.LEFT);
                    addDoctorNameFilter(request.getDoctorName(), userProfileJoin, criteriaBuilder, predicates);
                }

                // Join Specialty nếu cần tìm theo chuyên khoa
                if (request.getSpecialtyId() != null || StringUtils.hasText(request.getSpecialtyName())) {
                    if (medicalJoin == null) {
                        medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                    }
                    specialtyJoin = medicalJoin.join("specialty", JoinType.LEFT);
                    addSpecialtyFilter(request, specialtyJoin, criteriaBuilder, predicates);
                }

                // Lọc theo trạng thái phê duyệt
                if (request.getIsApproved() != null) {
                    if (medicalJoin == null) {
                        medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                    }
                    addApprovalStatusFilter(request.getIsApproved(), medicalJoin, criteriaBuilder, predicates);
                }

                // Lọc theo kinh nghiệm
                if (request.getMinExperience() != null || request.getMaxExperience() != null) {
                    if (medicalJoin == null) {
                        medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                    }
                    addExperienceFilter(request, medicalJoin, criteriaBuilder, predicates);
                }

                // Lọc theo giá khám
                if (request.getMinConsultationFee() != null || request.getMaxConsultationFee() != null) {
                    if (medicalJoin == null) {
                        medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                    }
                    addConsultationFeeFilter(request, medicalJoin, criteriaBuilder, predicates);
                }

                // Lọc theo bằng cấp
                if (StringUtils.hasText(request.getQualification())) {
                    if (medicalJoin == null) {
                        medicalJoin = root.join("medicalProfile", JoinType.LEFT);
                    }
                    addQualificationFilter(request.getQualification(), medicalJoin, criteriaBuilder, predicates);
                }

                // Lọc theo lịch làm việc
                if (request.getAvailableDate() != null) {
                    scheduleJoin = root.join("doctorSchedules", JoinType.INNER);
                    addAvailabilityFilter(request, scheduleJoin, criteriaBuilder, predicates);
                }


                query.distinct(true);

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

            } catch (Exception e) {
                log.error("Error building search specification", e);
                throw new CustomException(ErrorCode.DATABASE_ERROR,
                        "Failed to build search query: " + e.getMessage());
            }
        };
    }
    private boolean needsMedicalProfileJoin(DoctorSearchRequest request) {
        return request.getSpecialtyId() != null ||
                StringUtils.hasText(request.getSpecialtyName()) ||
                request.getIsApproved() != null ||
                request.getMinExperience() != null ||
                request.getMaxExperience() != null ||
                request.getMinConsultationFee() != null ||
                request.getMaxConsultationFee() != null ||
                StringUtils.hasText(request.getQualification());
    }

    private void addDoctorRoleFilter(Root<User> root, CriteriaBuilder criteriaBuilder,
                                     List<Predicate> predicates) {
        Join<User, UserRole> userRoleJoin = root.join("userRoles", JoinType.INNER);
        predicates.add(criteriaBuilder.equal(userRoleJoin.get("role").get("name"), "DOCTOR"));
    }

    private void addDoctorNameFilter(String doctorName, Join<User, UserProfile> userProfileJoin,
                                     CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        String searchTerm = doctorName.trim().toLowerCase();
        List<Predicate> namePredicates = new ArrayList<>();

        // Tìm theo firstName
        namePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(userProfileJoin.get("firstName")),
                "%" + searchTerm + "%"
        ));

        // Tìm theo lastName
        namePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(userProfileJoin.get("lastName")),
                "%" + searchTerm + "%"
        ));

        // Tìm theo fullName (firstName + " " + lastName)
        Expression<String> fullNameExpression = criteriaBuilder.concat(
                criteriaBuilder.concat(
                        criteriaBuilder.lower(userProfileJoin.get("firstName")),
                        criteriaBuilder.literal(" ")
                ),
                criteriaBuilder.lower(userProfileJoin.get("lastName"))
        );
        namePredicates.add(criteriaBuilder.like(fullNameExpression, "%" + searchTerm + "%"));

        // Tìm theo từng từ nếu có nhiều từ
        String[] searchWords = searchTerm.split("\\s+");
        if (searchWords.length > 1) {
            for (String word : searchWords) {
                if (word.length() > 1) {
                    namePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(userProfileJoin.get("firstName")),
                            "%" + word + "%"
                    ));
                    namePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(userProfileJoin.get("lastName")),
                            "%" + word + "%"
                    ));
                }
            }
        }

        predicates.add(criteriaBuilder.or(namePredicates.toArray(new Predicate[0])));
    }

    private void addSpecialtyFilter(DoctorSearchRequest request, Join<MedicalProfile, Specialty> specialtyJoin,
                                    CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        List<Predicate> specialtyPredicates = new ArrayList<>();

        if (request.getSpecialtyId() != null) {
            specialtyPredicates.add(criteriaBuilder.equal(
                    specialtyJoin.get("id"), request.getSpecialtyId()
            ));
        }

        if (StringUtils.hasText(request.getSpecialtyName())) {
            specialtyPredicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(specialtyJoin.get("name")),
                    "%" + request.getSpecialtyName().toLowerCase() + "%"
            ));
        }

        if (!specialtyPredicates.isEmpty()) {
            predicates.add(criteriaBuilder.or(specialtyPredicates.toArray(new Predicate[0])));
        }
    }

    private void addApprovalStatusFilter(Boolean isApproved, Join<User, MedicalProfile> medicalJoin,
                                         CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        predicates.add(criteriaBuilder.equal(medicalJoin.get("isDoctorApproved"), isApproved));
    }

    private void addExperienceFilter(DoctorSearchRequest request, Join<User, MedicalProfile> medicalJoin,
                                     CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        if (request.getMinExperience() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    medicalJoin.get("yearsOfExperience"), request.getMinExperience()
            ));
        }

        if (request.getMaxExperience() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    medicalJoin.get("yearsOfExperience"), request.getMaxExperience()
            ));
        }
    }

    private void addConsultationFeeFilter(DoctorSearchRequest request, Join<User, MedicalProfile> medicalJoin,
                                          CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        if (request.getMinConsultationFee() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    medicalJoin.get("consultationFee"), request.getMinConsultationFee()
            ));
        }

        if (request.getMaxConsultationFee() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    medicalJoin.get("consultationFee"), request.getMaxConsultationFee()
            ));
        }
    }

    private void addQualificationFilter(String qualification, Join<User, MedicalProfile> medicalJoin,
                                        CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        predicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(medicalJoin.get("qualification")),
                "%" + qualification.toLowerCase() + "%"
        ));
    }


    private void addAvailabilityFilter(DoctorSearchRequest request, Join<User, DoctorSchedule> scheduleJoin,
                                       CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        LocalDate availableDate = request.getAvailableDate();
        DayOfWeek dayOfWeek = availableDate.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue(); // Monday = 1, Sunday = 7

        List<Predicate> availabilityPredicates = new ArrayList<>();

        // Lọc theo ngày trong tuần
        availabilityPredicates.add(criteriaBuilder.equal(
                scheduleJoin.get("dayOfWeek"), dayOfWeekValue
        ));

        // Lọc lịch đang active
        availabilityPredicates.add(criteriaBuilder.isTrue(scheduleJoin.get("isActive")));

        // Lọc theo thời gian ưa thích nếu có
        if (request.getPreferredStartTime() != null) {
            availabilityPredicates.add(criteriaBuilder.lessThanOrEqualTo(
                    scheduleJoin.get("startTime"), request.getPreferredStartTime()
            ));
        }

        if (request.getPreferredEndTime() != null) {
            availabilityPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    scheduleJoin.get("endTime"), request.getPreferredEndTime()
            ));
        }

        predicates.add(criteriaBuilder.and(availabilityPredicates.toArray(new Predicate[0])));
    }
}
