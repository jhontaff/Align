package com.jet.align.ai.agent;

import com.jet.align.user.User;

public interface AgentService {

    AgentResponse chat(String userMessage, User user);
}
