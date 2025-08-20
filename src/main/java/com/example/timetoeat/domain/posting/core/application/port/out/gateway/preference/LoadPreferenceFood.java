package com.example.timetoeat.domain.posting.core.application.port.out.gateway.preference;

import java.util.List;

public interface LoadPreferenceFood {
    List<String> loadFoodPreferences(List<String> imageUrls);
}
