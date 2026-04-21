package com.spy.copywritingaiagentserver.workflow.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStateReader {

    @SuppressWarnings("unchecked")
    public <T> T get(OverAllState state, String key, Class<T> clazz) {
        Object value = state.value(key).orElse(null);
        return value == null ? null : (T) value;
    }

    public String getString(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        return value == null ? null : String.valueOf(value);
    }

    public Boolean getBoolean(OverAllState state, String key) {
        Object value = state.value(key).orElse(Boolean.FALSE);
        return (Boolean) value;
    }
}