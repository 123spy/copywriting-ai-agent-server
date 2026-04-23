package com.spy.copywritingaiagentserver.model.dto.contentproject;

import com.spy.copywritingaiagentserver.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class ContentProjectQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String projectName;

    private String platform;

    private String topic;

    private String audience;

    private String tone;

    private String normalizedStyle;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
