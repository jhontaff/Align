package com.jet.align.ai.memory.impl;

import com.jet.align.ai.memory.UserMemory;
import com.jet.align.ai.memory.UserMemoryRepository;
import com.jet.align.ai.memory.UserMemoryService;
import com.jet.align.ai.memory.dto.MemoryResponse;
import com.jet.align.common.exception.ResourceNotFoundException;
import com.jet.align.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserMemoryServiceImpl implements UserMemoryService {

    private final UserMemoryRepository userMemoryRepository;

    private static final String MEMORY_NOT_FOUND_MESSAGE = "Memory not found with id: ";

    public UserMemoryServiceImpl(UserMemoryRepository userMemoryRepository) {
        this.userMemoryRepository = userMemoryRepository;
    }

    @Override
    @Transactional
    public MemoryResponse remember(User user, String content) {
        UserMemory userMemory = new UserMemory();
        userMemory.setUser(user);
        userMemory.setContent(content);

        return toResponse(userMemoryRepository.save(userMemory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryResponse> list(User user) {
        return userMemoryRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemoryResponse update(User user, UUID id, String content) {
        UserMemory userMemory = userMemoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(MEMORY_NOT_FOUND_MESSAGE + id));

        userMemory.setContent(content);
        return toResponse(userMemory);
    }

    @Override
    @Transactional
    public void forget(User user, UUID id) {
        UserMemory userMemory = userMemoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException(MEMORY_NOT_FOUND_MESSAGE + id));

        userMemoryRepository.delete(userMemory);
    }

    private MemoryResponse toResponse(UserMemory memory) {
        return new MemoryResponse(memory.getId(), memory.getContent(), memory.getCreatedAt());
    }

}
