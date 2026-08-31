package com.jet.align.notification;

import com.jet.align.user.User;

public interface NotificationService {
    void notify(User user, String title, String body);
}
