package com.example.timetoeat.domain.posting.core.gateway.service.out.participation;

import com.example.timetoeat.domain.posting.domain.model.participation.Participation;

public interface SaveParticipationPort {
    void save(Participation participation);
}
