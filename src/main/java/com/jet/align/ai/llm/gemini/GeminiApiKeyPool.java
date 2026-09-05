package com.jet.align.ai.llm.gemini;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
class GeminiApiKeyPool {

    private final List<String> keys;
    private final AtomicInteger cursor = new AtomicInteger(0);

    GeminiApiKeyPool(GeminiProperties properties) {
        List<String> apiKeys = properties.apiKeys();
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalStateException(
                    "align.gemini.api-keys no puede estar vacío: configurá al menos una key.");
        }
        this.keys = apiKeys;
    }

    String next() {
        int index = cursor.getAndUpdate(i -> (i + 1) % keys.size());
        System.out.println("[SMOKE TEST] GeminiApiKeyPool.next() -> index " + index + " / " + keys.size());
        return keys.get(index);
    }

    int size() {
        return keys.size();
    }
}
