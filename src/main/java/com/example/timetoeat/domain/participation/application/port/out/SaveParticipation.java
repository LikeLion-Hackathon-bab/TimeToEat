package com.example.timetoeat.domain.participation.application.port.out;

import com.example.timetoeat.domain.participation.domain.Participation;

public interface SaveParticipation {
    void save(Participation participation);
}
