package com.spy.copywritingaiagentserver.model;

import lombok.Data;

/**
 * 项目状态枚举
 */

public enum ProjectStatus {
    // 0:INIT/1:RUNNING/2:SUCCESS/3:FAILED
    INIT(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3);

    private int code;

    ProjectStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public ProjectStatus getStatusByCode(int code) {
        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
