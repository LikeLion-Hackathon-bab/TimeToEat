package com.example.timetoeat.domain.post.application.port.out;

import java.util.List;

public interface LoadPreferenceFood {
    List<String> loadFoodPreferences(List<String> imageUrls);
}
