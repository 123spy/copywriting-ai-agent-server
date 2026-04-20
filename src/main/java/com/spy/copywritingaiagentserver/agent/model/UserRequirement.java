package com.spy.copywritingaiagentserver.agent.model;

import lombok.Data;

@Data
public class UserRequirement {

    /**
     * 平台
     */
    private String platform;

    /**
     * 主题
     */
    private String topic;

    /**
     * 目标人群
     */
    private String audience;

    /**
     * 风格
     */
    private String tone;

    /**
     * 产品信息
     */
    private String productInfo;
}
