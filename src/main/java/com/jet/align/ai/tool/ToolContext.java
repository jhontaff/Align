package com.jet.align.ai.tool;

import com.jet.align.user.User;

import java.util.Map;

public record ToolContext(

        User user,
        Map<String, Object> arguments
) {
}
