package com.github.warada_v2.shared.services;

import com.github.warada_v2.shared.entities.Tag;
import java.util.List;

public interface TagService {
  List<Tag> getAllTags();

  Tag findById(Long id);

  List<Tag> resolveOrCreateTags(List<String> rawTagNames);

  void incrementComicCount(List<Tag> tags);
}
