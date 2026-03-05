package org.project.appointment_project.ui.viewmodel.admin;

import java.util.UUID;

import org.project.appointment_project.common.util.SecurityUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zkplus.spring.SpringUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashboardViewModel {

    public static final String SESSION_CURRENT_USER_ID = "currentUserId";

    private String currentContent;

    @Init
    public void init() {
        this.currentContent = "/user/doctor-list.zul";
        saveCurrentUserToSession();
    }

    private void saveCurrentUserToSession() {
        try {
            SecurityUtils securityUtils = SpringUtil.getApplicationContext().getBean(SecurityUtils.class);
            securityUtils.getCurrentUserIdSafe().ifPresentOrElse(
                    userId -> {
                        Sessions.getCurrent().setAttribute(SESSION_CURRENT_USER_ID, userId);
                        log.info("Đã lưu currentUserId vào ZK session: {}", userId);
                    },
                    () -> log.warn("Không lấy được currentUserId khi khởi tạo DashboardViewModel")
            );
        } catch (Exception e) {
            log.warn("saveCurrentUserToSession thất bại: {}", e.getMessage());
        }
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