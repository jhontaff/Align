package com.jet.align.ai.model;

import java.util.Map;

public record ToolCall(

        String name,

        Map<String, Object> arguments

) {
}
