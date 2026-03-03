package org.project.appointment_project.ui.viewmodel;

import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

import lombok.Getter;

@Getter
public class IndexViewModel {

    private String message;

    @Init
    public void init() {
        message = "Chào mừng đến với Hệ thống Đặt lịch khám chữa bệnh!";
    }

    @Command
    @NotifyChange("message")
    public void refresh() {
        message = "Đã làm mới lúc: " + new java.util.Date();
    }
}
