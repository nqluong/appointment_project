package org.project.appointment_project.ui.viewmodel.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.util.SecurityUtils;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.service.DoctorService;
import org.project.appointment_project.user.service.SpecialtyService;
import org.project.appointment_project.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

@Slf4j
public class DoctorListViewModel {

    private DoctorService doctorService;
    private SpecialtyService specialtyService;
    private UserService userService;
    private SecurityUtils securityUtils;

    @Getter
    private List<DoctorResponse> doctors = new ArrayList<>();
    @Getter
    private List<SpecialtyResponse> specialties = new ArrayList<>();
    @Getter
    private ListModelList<String> specialtyModel = new ListModelList<>();
    @Getter
    @Setter
    private String searchKeyword = "";
    @Getter
    @Setter
    private String selectedSpecialty = "";
    @Getter
    private int currentPage = 0;
    @Getter
    private int totalPages = 0;
    @Getter
    private long totalElements = 0;

    private Timer debounceTimer;

    private static final int PAGE_SIZE = 10;
    private static final int DEBOUNCE_DELAY_MS = 500;

    @Init
    public void init() {
        this.doctorService = SpringUtil.getApplicationContext().getBean(DoctorService.class);
        this.specialtyService = SpringUtil.getApplicationContext().getBean(SpecialtyService.class);
        this.userService = SpringUtil.getApplicationContext().getBean(UserService.class);
        this.securityUtils = SpringUtil.getApplicationContext().getBean(SecurityUtils.class);
        loadSpecialties();
        loadDoctors();
    }

    private void loadSpecialties() {
        try {
            specialties = specialtyService.getAllActiveSpecialties();
            List<String> names = new ArrayList<>();
            names.add("Tat ca");
            specialties.forEach(s -> names.add(s.getName()));
            specialtyModel = new ListModelList<>(names);
            specialtyModel.addToSelection("Tat ca");
        } catch (Exception e) {
            specialties = new ArrayList<>();
            specialtyModel = new ListModelList<>(List.of("Tat ca"));
            specialtyModel.addToSelection("Tat ca");
        }
    }

    private void loadDoctors() {
        try {
            Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);

            boolean hasKeyword = searchKeyword != null && !searchKeyword.isBlank();
            boolean hasFilter = selectedSpecialty != null
                    && !selectedSpecialty.isBlank()
                    && !selectedSpecialty.equals("Tat ca");

            PageResponse<DoctorResponse> response;
            if (hasKeyword) {
                // Tim kiem theo tu khoa (ho ten hoac chuyen khoa) qua DB
                response = doctorService.searchDoctors(searchKeyword.trim(), pageable);
                // Neu dong thoi co loc chuyen khoa, loc them o client
                if (hasFilter) {
                    String filterLower = selectedSpecialty.toLowerCase();
                    doctors = response.getContent().stream()
                            .filter(d -> d.getSpecialtyName() != null
                                    && d.getSpecialtyName().toLowerCase().contains(filterLower))
                            .toList();
                } else {
                    doctors = response.getContent() != null ? response.getContent() : new ArrayList<>();
                }
            } else if (hasFilter) {
                response = doctorService.getDoctorsWithFilters(selectedSpecialty, pageable);
                doctors = response.getContent() != null ? response.getContent() : new ArrayList<>();
            } else {
                response = doctorService.getAllDoctors(pageable);
                doctors = response.getContent() != null ? response.getContent() : new ArrayList<>();
            }

            totalPages = response.getTotalPages();
            totalElements = response.getTotalElements();
        } catch (Exception e) {
            log.error("Loi tai danh sach bac si: {}", e.getMessage(), e);
            doctors = new ArrayList<>();
            totalPages = 0;
            totalElements = 0;
        }
    }

    @Command
    public void onSearchKeywordChange() {
        if (debounceTimer != null) {
            debounceTimer.stop();
            debounceTimer.detach();
            debounceTimer = null;
        }

        debounceTimer = new Timer();
        debounceTimer.setDelay(DEBOUNCE_DELAY_MS);
        debounceTimer.setRepeats(false);
        debounceTimer.addEventListener(Events.ON_TIMER, event -> {
            currentPage = 0;
            loadDoctors();
            BindUtils.postNotifyChange(DoctorListViewModel.this, "doctors");
            BindUtils.postNotifyChange(DoctorListViewModel.this, "currentPage");
            BindUtils.postNotifyChange(DoctorListViewModel.this, "totalPages");
            BindUtils.postNotifyChange(DoctorListViewModel.this, "totalElements");
            BindUtils.postNotifyChange(DoctorListViewModel.this, "pageInfo");
            debounceTimer.detach();
            debounceTimer = null;
        });

        debounceTimer.setPage(Executions.getCurrent().getDesktop().getFirstPage());
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void search() {
        selectedSpecialty = specialtyModel.getSelection().isEmpty() ? ""
                : "Tất cả".equals(specialtyModel.getSelection().iterator().next()) ? ""
                : specialtyModel.getSelection().iterator().next();
        currentPage = 0;
        loadDoctors();
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadDoctors();
        }
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadDoctors();
        }
    }

    @Command
    public void openAddDoctor() {
        openDialog("/user/add-doctor-dialog.zul", null);
    }

    @Command
    public void viewDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        openDialog("/user/doctor-detail-dialog.zul", Map.of("doctorId", doctor.getId()));
    }

    @Command
    public void editDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        openDialog("/user/edit-profile-dialog.zul", Map.of("userId", doctor.getId()));
    }

    @Command
    public void deleteDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        openDialog("/user/delete-doctor-dialog.zul",
                Map.of("doctorId", doctor.getId(),
                        "doctorName", doctor.getFullName() != null ? doctor.getFullName() : ""));
    }

    @GlobalCommand("onDoctorAdded")
    @NotifyChange({"doctors", "totalPages", "totalElements", "pageInfo"})
    public void onDoctorAdded() {
        currentPage = 0;
        loadDoctors();
    }

    @GlobalCommand("onProfileUpdated")
    @NotifyChange({"doctors", "totalPages", "totalElements", "pageInfo"})
    public void onProfileUpdated() {
        loadDoctors();
    }

    @GlobalCommand("onConfirmSoftDelete")
    @NotifyChange({"doctors", "totalPages", "totalElements", "pageInfo"})
    public void onConfirmSoftDelete(@BindingParam("doctorId") UUID doctorId) {
        executeDeletion(doctorId, false);
    }

    @GlobalCommand("onOpenHardDelete")
    public void onOpenHardDelete(@BindingParam("doctorId") UUID doctorId,
                                 @BindingParam("doctorName") String doctorName) {
        openDialog("/user/hard-delete-doctor-dialog.zul",
                Map.of("doctorId", doctorId,
                        "doctorName", doctorName != null ? doctorName : ""));
    }

    @GlobalCommand("onConfirmHardDelete")
    @NotifyChange({"doctors", "totalPages", "totalElements", "pageInfo"})
    public void onConfirmHardDelete(@BindingParam("doctorId") UUID doctorId) {
        executeDeletion(doctorId, true);
    }

    private void executeDeletion(UUID doctorId, boolean hardDelete) {
        try {
            UUID deletedBy = resolveCurrentUserId();
            userService.deleteUser(doctorId, deletedBy, "Xóa bởi admin", hardDelete);
            currentPage = 0;
            loadDoctors();
            BindUtils.postGlobalCommand(null, null, "onDoctorDeleted", null);
            String msg = hardDelete ? "Đã xóa vĩnh viễn bác sĩ." : "Đã vô hiệu hóa bác sĩ .";
            openDialog("/user/result-dialog.zul", Map.of("success", true, "message", msg));
        } catch (Exception e) {
            log.error("Xoa bac si {} that bai: {}", doctorId, e.getMessage(), e);
            openDialog("/user/result-dialog.zul",
                    Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Đã có lỗi xảy ra."));
        }
    }

    private void openDialog(String zulPath, Map<String, Object> args) {
        Window win = (Window) Executions.createComponents(zulPath, null,
                args != null ? new HashMap<>(args) : null);
        win.doModal();
    }

    private UUID resolveCurrentUserId() {
        Object sessionId = Sessions.getCurrent().getAttribute(DashboardViewModel.SESSION_CURRENT_USER_ID);
        if (sessionId instanceof UUID uid) return uid;
        Optional<UUID> fromSecurity = securityUtils.getCurrentUserIdSafe();
        if (fromSecurity.isPresent()) return fromSecurity.get();
        log.warn("resolveCurrentUserId: khong co session/token, dung DEV fallback UUID");
        return UUID.fromString("1aada4f9-9ca3-4c55-b7b8-47db6352d26b");
    }

    public boolean isFirstPage() { return currentPage == 0; }
    public boolean isLastPage() { return currentPage >= totalPages - 1; }
    public int getDisplayPage() { return currentPage + 1; }
    public String getPageInfo() { return "Trang " + getDisplayPage() + " / " + totalPages; }

    public String getExperienceLabel(DoctorResponse doctor) {
        return doctor.getYearsOfExperience() != null ? doctor.getYearsOfExperience() + " năm" : "";
    }

    public String getRowSclass(DoctorResponse doctor) {
        if (!doctor.isActive()) return "doctor-row doctor-row-inactive";
        if (!doctor.isApproved()) return "doctor-row doctor-row-unapproved";
        return "doctor-row";
    }

    public String getStatusBadgeSclass(DoctorResponse doctor) {
        if (!doctor.isActive()) return "doctor-badge doctor-badge-inactive";
        if (!doctor.isApproved()) return "doctor-badge doctor-badge-unapproved";
        return "doctor-badge doctor-badge-active";
    }

    public String getStatusLabel(DoctorResponse doctor) {
        if (!doctor.isActive()) return "Vô hiệu";
        if (!doctor.isApproved()) return "Chờ duyệt";
        return "Hoạt động";
    }
}


