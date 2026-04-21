package com.github.warada_v2.display.services;

import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.models.PageRequest;
import java.util.List;

public interface ComicService {
    List<Comic> getPageableComics(PageRequest pageRequest);
    Comic findById(Long id);
}
