package com.example.timetoeat.domain.posting.application.port.out.participation;

import com.example.timetoeat.domain.posting.domain.model.participation.Participation;

public interface SaveParticipationPort {
    void save(Participation participation);
}
