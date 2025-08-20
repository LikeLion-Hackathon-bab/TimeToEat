package com.example.timetoeat.domain.posting.core.application.port.out.gateway.participation;

import com.example.timetoeat.domain.posting.core.domain.model.participation.Participation;

public interface SaveParticipation {
    void save(Participation participation);
}
