package com.example.timetoeat.domain.plans.meeting;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MeetingServiceTest {
    private final MeetingRepository repository = mock(MeetingRepository.class);
    private final MeetingService service = new MeetingService(repository, null, null);

    @Test void repeatedConfirmationCanOnlyAcknowledgeTheSameRestaurant() {
        var stored = mock(ConfirmedMeeting.class);
        when(stored.getRestaurant()).thenReturn("초밥집");
        when(repository.findById(12L)).thenReturn(Optional.of(stored));
        assertDoesNotThrow(() -> service.record(12, "초밥집"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.record(12, "다른 식당")).getStatusCode().value());
    }

    @Test void anonymousViewerCannotListAnotherPersonsMeals() {
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.list(null, null)).getStatusCode().value());
        verifyNoInteractions(repository);
    }
}
