package com.example.timetoeat.domain.meeting.application.port.in;

import com.example.timetoeat.domain.meeting.dto.request.AnnouncementReq;
import com.example.timetoeat.domain.user.domain.User;

public interface AnnouncementUseCase {
    void create(User user, AnnouncementReq req);
    void cancel(User user, Long announcementId );
    void close(User user, Long announcementId);
    void autoExpire();
}
