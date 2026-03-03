package org.project.appointment_project.ui.viewmodel.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.service.DoctorService;
import org.project.appointment_project.user.service.SpecialtyService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Window;

import lombok.Getter;
import lombok.Setter;

public class DoctorListViewModel {

    private DoctorService doctorService;
    private SpecialtyService specialtyService;

    @Getter private List<DoctorResponse> doctors = new ArrayList<>();
    @Getter private List<SpecialtyResponse> specialties = new ArrayList<>();
    @Getter private ListModelList<String> specialtyModel = new ListModelList<>();

    @Getter private String searchKeyword = "";
    @Getter @Setter private String selectedSpecialty = "";

    @Getter private int currentPage = 0;
    private final int pageSize = 10;
    @Getter private int totalPages = 0;
    @Getter private long totalElements = 0;


    @Init
    public void init() {
        this.doctorService = SpringUtil.getApplicationContext().getBean(DoctorService.class);
        this.specialtyService = SpringUtil.getApplicationContext().getBean(SpecialtyService.class);
        loadSpecialties();
        loadDoctors();
    }

    private void loadSpecialties() {
        try {
            specialties = specialtyService.getAllActiveSpecialties();
            // Filter listbox
            List<String> names = new ArrayList<>();
            names.add("Tất cả");
            for (SpecialtyResponse s : specialties) names.add(s.getName());
            specialtyModel = new ListModelList<>(names);
            specialtyModel.addToSelection("Tất cả");
        } catch (Exception e) {
            specialties = new ArrayList<>();
            specialtyModel = new ListModelList<>(List.of("Tất cả"));
            specialtyModel.addToSelection("Tất cả");
        }
    }

    private void loadDoctors() {
        try {
            Pageable pageable = PageRequest.of(currentPage, pageSize);
            PageResponse<DoctorResponse> response;
            boolean hasFilter = selectedSpecialty != null
                    && !selectedSpecialty.isBlank()
                    && !selectedSpecialty.equals("Tất cả");
            response = hasFilter
                    ? doctorService.getDoctorsWithFilters(selectedSpecialty, pageable)
                    : doctorService.getAllDoctors(pageable);

            if (searchKeyword != null && !searchKeyword.isBlank()) {
                String kw = searchKeyword.toLowerCase();
                doctors = response.getContent().stream()
                        .filter(d -> (d.getFullName() != null && d.getFullName().toLowerCase().contains(kw))
                                || (d.getSpecialtyName() != null && d.getSpecialtyName().toLowerCase().contains(kw)))
                        .toList();
            } else {
                doctors = response.getContent() != null ? response.getContent() : new ArrayList<>();
            }
            totalPages = response.getTotalPages();
            totalElements = response.getTotalElements();
        } catch (Exception e) {
            doctors = new ArrayList<>();
            totalPages = 0;
            totalElements = 0;
        }
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void search() {
        if (!specialtyModel.getSelection().isEmpty()) {
            String sel = specialtyModel.getSelection().iterator().next();
            selectedSpecialty = "Tất cả".equals(sel) ? "" : sel;
        } else {
            selectedSpecialty = "";
        }
        currentPage = 0;
        loadDoctors();
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void prevPage() {
        if (currentPage > 0) { currentPage--; loadDoctors(); }
    }

    @Command
    @NotifyChange({"doctors", "currentPage", "totalPages", "totalElements", "pageInfo"})
    public void nextPage() {
        if (currentPage < totalPages - 1) { currentPage++; loadDoctors(); }
    }

    @Command
    public void openAddDoctor() {
        Window win = (Window) Executions.createComponents(
                "/user/add-doctor-dialog.zul",
                null,
                null);
        win.doModal();
    }

    @Command
    public void viewDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        Map<String, Object> args = new HashMap<>();
        args.put("doctorId", doctor.getId());
        Window win = (Window) Executions.createComponents(
                "/user/doctor-detail-dialog.zul",
                null,
                args);
        win.doModal();
    }

    @GlobalCommand("onDoctorAdded")
    @NotifyChange({"doctors", "totalPages", "totalElements", "pageInfo"})
    public void onDoctorAdded() {
        currentPage = 0;
        loadDoctors();
    }

    @Command
    public void editDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        Map<String, Object> args = new HashMap<>();
        args.put("userId", doctor.getId());
        Window win = (Window) Executions.createComponents(
                "/user/edit-profile-dialog.zul",
                null,
                args);
        win.doModal();
    }

    @Command
    public void deleteDoctor(@BindingParam("doctor") DoctorResponse doctor) {
        // TODO: xác nhận xóa
    }

    public boolean isFirstPage() { return currentPage == 0; }
    public boolean isLastPage()  { return currentPage >= totalPages - 1; }
    public int getDisplayPage()  { return currentPage + 1; }
    public String getPageInfo()  { return "Trang " + getDisplayPage() + " / " + totalPages; }

    public String getExperienceLabel(DoctorResponse doctor) {
        return doctor.getYearsOfExperience() != null ? doctor.getYearsOfExperience() + " năm" : "";
    }



    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
