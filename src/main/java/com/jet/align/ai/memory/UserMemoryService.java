package com.jet.align.ai.memory;

import com.jet.align.ai.memory.dto.MemoryResponse;
import com.jet.align.user.User;
import java.util.List;
import java.util.UUID;

public interface UserMemoryService {
    MemoryResponse remember(User user, String content);
    List<MemoryResponse> list(User user);
    MemoryResponse update(User user, UUID id, String content);
    void forget(User user, UUID id);
}
