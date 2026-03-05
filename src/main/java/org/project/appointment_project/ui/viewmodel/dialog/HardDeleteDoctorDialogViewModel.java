package org.project.appointment_project.ui.viewmodel.dialog;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
public class HardDeleteDoctorDialogViewModel {

    @Getter
    private String doctorName = "";

    @Getter
    private UUID doctorId;

    @Init
    public void init(@ExecutionArgParam("doctorName") String doctorName,
                     @ExecutionArgParam("doctorId") UUID doctorId) {
        this.doctorName = doctorName != null ? doctorName : "";
        this.doctorId = doctorId;
    }

    @Command
    public void hardDelete(@BindingParam("view") Component view) {
        view.detach();
        BindUtils.postGlobalCommand(null, null, "onConfirmHardDelete",
                Map.of("doctorId", doctorId));
    }

    @Command
    public void cancel(@BindingParam("view") Component view) {
        view.detach();
    }
}

