package com.example.timetoeat.domain.posting.application.port.out.save;

import com.example.timetoeat.domain.posting.domain.model.Post;

public interface SavePostPort {
    void save(Post post);
}
