package com.github.warada_v2.api.services;

import com.github.warada_v2.shared.entities.Tag;
import java.util.List;

public interface TagService {
    List<Tag> getAllTags();
    Tag findById(Long id);
}
