package com.jet.align.ai.agent;

import com.jet.align.ai.agent.dto.AgentResponse;
import com.jet.align.ai.agent.dto.ChatHistoryResponse;
import com.jet.align.user.User;

public interface AgentService {

    AgentResponse chat(String userMessage, User user);

    ChatHistoryResponse getHistory(User user);
}
