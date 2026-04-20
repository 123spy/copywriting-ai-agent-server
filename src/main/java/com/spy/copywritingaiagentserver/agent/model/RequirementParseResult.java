package com.spy.copywritingaiagentserver.agent.model;

import lombok.Data;

import java.util.List;

@Data
public class RequirementParseResult {

    private String platform;

    private String topic;

    private String targetAudience;

    private String tone;

    private List<String> sellingPoints;

    private String contentGoal;

    private String constraints;
}
