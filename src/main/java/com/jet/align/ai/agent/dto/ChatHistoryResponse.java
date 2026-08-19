package com.jet.align.ai.agent.dto;

import java.util.List;

public record ChatHistoryResponse(
        List<ChatTurn> turns
) {
}
