package org.project.appointment_project.ui.viewmodel.admin;

import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

public class DashboardViewModel {

    private String currentContent;

    @Init
    public void init() {
        this.currentContent = "/user/doctor-list.zul";
    }

    public String getCurrentContent() {
        return currentContent;
    }

    @Command
    @NotifyChange("currentContent")
    public void navigate(@BindingParam("page") String pageUrl) {
        this.currentContent = pageUrl;
    }
}