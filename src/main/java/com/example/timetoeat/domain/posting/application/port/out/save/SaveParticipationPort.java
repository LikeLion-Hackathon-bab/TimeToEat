package com.example.timetoeat.domain.posting.application.port.out.save;

import com.example.timetoeat.domain.posting.domain.model.Participation;

public interface SaveParticipationPort {
    void save(Participation participation);
}
