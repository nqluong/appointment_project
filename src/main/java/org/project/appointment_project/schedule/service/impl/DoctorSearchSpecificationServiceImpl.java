package org.project.appointment_project.schedule.service.impl;

import jakarta.persistence.criteria.*;
import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;
import org.project.appointment_project.schedule.model.DoctorSchedule;
import org.project.appointment_project.schedule.service.DoctorSearchSpecificationService;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;
import org.project.appointment_project.user.model.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorSearchSpecificationServiceImpl implements DoctorSearchSpecificationService {
    @Override
    public Specification<User> buildDoctorSearchSpecification(DoctorSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter for users with DOCTOR role
            Join<User, UserRole> userRoleJoin = root.join("userRoles", JoinType.INNER);
            predicates.add(criteriaBuilder.equal(userRoleJoin.get("role").get("name"), "DOCTOR"));

            // Join bảng medical profile
            Join<User, MedicalProfile> medicalProfileJoin = root.join("medicalProfile", JoinType.LEFT);

            // Specialty filter
            if (request.getSpecialtyId() != null) {
                predicates.add(criteriaBuilder.equal(
                        medicalProfileJoin.get("specialty").get("id"),
                        request.getSpecialtyId()
                ));
            }

            // Doctor name filter (search in first name and last name)
            if (StringUtils.hasText(request.getDoctorName())) {
                addDoctorNamePredicate(request.getDoctorName(), root, criteriaBuilder, predicates);
            }

            // Approval status filter
            if (request.getIsApproved() != null) {
                predicates.add(criteriaBuilder.equal(
                        medicalProfileJoin.get("isDoctorApproved"),
                        request.getIsApproved()
                ));
            }

            // Experience filter
            if (request.getMinExperience() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        medicalProfileJoin.get("yearsOfExperience"),
                        request.getMinExperience()
                ));
            }

            if (request.getMaxExperience() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        medicalProfileJoin.get("yearsOfExperience"),
                        request.getMaxExperience()
                ));
            }

            // Qualification filter
            if (StringUtils.hasText(request.getQualification())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(medicalProfileJoin.get("qualification")),
                        "%" + request.getQualification().toLowerCase() + "%"
                ));
            }

            // Available date filter
            if (request.getAvailableDate() != null) {
                addAvailabilityPredicate(request, root, criteriaBuilder, predicates);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addDoctorNamePredicate(String doctorName, Root<User> root,
                                        CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {

        String searchTerm = doctorName.trim().toLowerCase();

        // Join với UserProfile để lấy firstName và lastName
        Join<User, UserProfile> userProfileJoin = root.join("userProfile", JoinType.LEFT);

        List<Predicate> namePredicates = new ArrayList<>();

        // Tìm kiếm theo firstName
        namePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(userProfileJoin.get("firstName")),
                "%" + searchTerm + "%"
        ));

        // Tìm kiếm theo lastName
        namePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(userProfileJoin.get("lastName")),
                "%" + searchTerm + "%"
        ));

        // Tìm kiếm theo fullName (concat firstName + " " + lastName)
        Expression<String> fullNameExpression = criteriaBuilder.concat(
                criteriaBuilder.concat(
                        criteriaBuilder.lower(userProfileJoin.get("firstName")),
                        criteriaBuilder.literal(" ")
                ),
                criteriaBuilder.lower(userProfileJoin.get("lastName"))
        );

        namePredicates.add(criteriaBuilder.like(fullNameExpression, "%" + searchTerm + "%"));

        // Nếu search term có nhiều từ, tìm kiếm theo từng từ
        String[] searchWords = searchTerm.split("\\s+");
        if (searchWords.length > 1) {
            for (String word : searchWords) {
                if (word.length() > 1) { // Bỏ qua từ quá ngắn
                    // Tìm trong firstName
                    namePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(userProfileJoin.get("firstName")),
                            "%" + word + "%"
                    ));

                    // Tìm trong lastName
                    namePredicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(userProfileJoin.get("lastName")),
                            "%" + word + "%"
                    ));
                }
            }
        }

        // Combine all name predicates with OR
        predicates.add(criteriaBuilder.or(namePredicates.toArray(new Predicate[0])));
    }

    private void addAvailabilityPredicate(DoctorSearchRequest request, Root<User> root,
                                          CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {

        LocalDate availableDate = request.getAvailableDate();
        DayOfWeek dayOfWeek = availableDate.getDayOfWeek();
        int dayOfWeekValue = dayOfWeek.getValue(); // Monday = 1, Sunday = 7

        // Join with doctor schedules
        Join<User, DoctorSchedule> scheduleJoin = root.join("doctorSchedules", JoinType.INNER);

        List<Predicate> availabilityPredicates = new ArrayList<>();

        // Schedule for the specific day of week
        availabilityPredicates.add(criteriaBuilder.equal(
                scheduleJoin.get("dayOfWeek"), dayOfWeekValue
        ));

        // Schedule is active
        availabilityPredicates.add(criteriaBuilder.isTrue(scheduleJoin.get("isActive")));

        // Time range filters if specified
        if (request.getPreferredStartTime() != null) {
            availabilityPredicates.add(criteriaBuilder.lessThanOrEqualTo(
                    scheduleJoin.get("startTime"),
                    request.getPreferredStartTime()
            ));
        }

        if (request.getPreferredEndTime() != null) {
            availabilityPredicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    scheduleJoin.get("endTime"),
                    request.getPreferredEndTime()
            ));
        }

        predicates.add(criteriaBuilder.and(availabilityPredicates.toArray(new Predicate[0])));
    }
}
