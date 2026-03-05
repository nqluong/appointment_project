package org.project.appointment_project.ui.viewmodel.dialog;

import lombok.Getter;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;

public class ResultDialogVM {

    @Getter
    private String headerSclass;

    @Getter
    private String iconSclass;

    @Getter
    private String headerTitle;

    @Getter
    private String message;

    @Init
    public void init(@ExecutionArgParam("success") Boolean success,
                     @ExecutionArgParam("message") String message) {
        boolean ok = Boolean.TRUE.equals(success);
        this.message = message != null ? message : "";
        this.headerSclass = ok ? "confirm-header confirm-header-info"
                : "confirm-header confirm-header-error";
        this.iconSclass = ok ? "fa-solid fa-circle-check confirm-icon-info"
                : "fa-solid fa-circle-xmark confirm-icon-error";
        this.headerTitle = ok ? "Thành công" : "Thất bại";
    }

    @Command
    public void close(@ContextParam(ContextType.VIEW) Component view) {
        view.detach();
    }
}
