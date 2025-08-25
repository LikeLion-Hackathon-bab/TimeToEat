package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostParsingService {
    public PostDetailsDto getPostDetails(Post post) {
        return new PostDetailsDto(
                post.getLocation(),
                post.getMeetingAt()
        );
    }
}
