package com.jet.align.notification.dto;

public record SubscribeRequest(String endpoint, Keys keys) {
    public record Keys(String p256dh, String auth) {}
}
