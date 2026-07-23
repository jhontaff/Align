package com.jet.align.ai.tool;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(
                Tool::name, Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException(
                            "Duplicate tool name: " + first.name());
                }));
    }

    public Optional<Tool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<Tool> getAll() {
        return tools.values();
    }
}