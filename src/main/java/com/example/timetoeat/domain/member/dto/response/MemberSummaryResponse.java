package com.example.timetoeat.domain.member.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSummaryResponse {

    private int friends;
    private int meetingsDone;
    private int meetingsPlanned;

    @Builder
    private MemberSummaryResponse(int friends, int meetingsDone, int meetingsPlanned) {
        this.friends = friends;
        this.meetingsDone = meetingsDone;
        this.meetingsPlanned = meetingsPlanned;
    }

    public static MemberSummaryResponse of(int friends, int meetingDone, int meetingPlanned) {
        return MemberSummaryResponse.builder()
                .friends(friends).meetingsDone(meetingDone).meetingsPlanned(meetingPlanned).build();
    }
}
