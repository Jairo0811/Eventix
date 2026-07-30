package com.jairomatias.eventix.category.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.category.dto.CategoryForm;
import com.jairomatias.eventix.category.dto.CategoryListItem;
import com.jairomatias.eventix.category.dto.CategoryOption;

public interface EventCategoryService {

    Page<CategoryListItem> findAll(
            String term,
            Boolean active,
            Pageable pageable);

    List<CategoryOption> findActiveOptions();

    CategoryForm getForm(Long id);

    Long create(CategoryForm form);

    void update(Long id, CategoryForm form);

    void activate(Long id);

    void deactivate(Long id);
}
