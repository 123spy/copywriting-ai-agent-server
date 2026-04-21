package com.spy.copywritingaiagentserver.ai.model;

import lombok.Data;

import java.util.List;

@Data
public class ContentPlanResult {

    private String contentAngle;      // 内容角度，例如：早餐替代 + 低负担减脂
    private String hookStrategy;      // 开头钩子策略
    private List<String> corePoints;  // 核心内容点
    private String structureAdvice;   // 内容结构建议
    private String ctaStrategy;       // CTA策略
    private String visualStyle;       // 视觉风格建议
}