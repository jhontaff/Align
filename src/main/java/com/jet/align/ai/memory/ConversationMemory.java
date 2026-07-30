package com.jet.align.ai.memory;

import com.jet.align.ai.llm.Message;
import com.jet.align.user.User;

import java.util.List;

public interface ConversationMemory {

    List<Message> loadHistory(User user);

    void append(User user, List<Message> newMessages);
}
