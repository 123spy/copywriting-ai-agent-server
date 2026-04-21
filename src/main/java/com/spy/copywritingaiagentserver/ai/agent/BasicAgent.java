package com.spy.copywritingaiagentserver.ai.agent;

import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

@Data
public class BasicAgent {

    private final ChatClient chatClient;

    // Bean
    public BasicAgent(ChatModel chatModel, ToolCallback[] allTools) {
        chatClient = ChatClient
                .builder(chatModel)
                .defaultToolCallbacks(allTools)
                .build();
    }

}
