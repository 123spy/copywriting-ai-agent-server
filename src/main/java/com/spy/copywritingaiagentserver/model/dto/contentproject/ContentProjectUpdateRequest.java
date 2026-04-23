package com.spy.copywritingaiagentserver.model.dto.contentproject;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContentProjectUpdateRequest implements Serializable {

    private Long id;

    private String projectName;

    private String platform;

    private String topic;

    private String audience;

    private String tone;

    private String normalizedStyle;

    private String productInfo;

    private String requirement;

    private static final long serialVersionUID = 1L;
}
