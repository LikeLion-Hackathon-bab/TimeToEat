package com.example.timetoeat.domain.posting.application.port.out.save;

import com.example.timetoeat.domain.posting.domain.model.Notification;

public interface SaveNotificationPort {
    void save(Notification notification);
}
