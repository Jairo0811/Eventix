package com.jairomatias.eventix.category.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.category.dto.CategoryForm;
import com.jairomatias.eventix.category.dto.CategoryListItem;
import com.jairomatias.eventix.category.dto.CategoryOption;
import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.category.repository.EventCategoryRepository;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

@Service
public class DefaultEventCategoryService
        implements EventCategoryService {

    private final EventCategoryRepository categoryRepository;

    public DefaultEventCategoryService(
            EventCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<CategoryListItem> findAll(
            String term,
            Boolean active,
            Pageable pageable) {

        String normalizedTerm = term == null ? "" : term.trim();
        return categoryRepository.search(
                        normalizedTerm,
                        active,
                        pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<CategoryOption> findActiveOptions() {
        return categoryRepository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(category -> new CategoryOption(
                        category.getId(),
                        category.getName(),
                        category.getSystemKey()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public CategoryForm getForm(Long id) {
        EventCategory category = findEntity(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long create(CategoryForm form) {
        String name = form.getName().trim();
        validateUniqueName(name, null);

        EventCategory category = new EventCategory(
                name,
                normalizeNullable(form.getDescription()));

        return categoryRepository.save(category).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void update(Long id, CategoryForm form) {
        EventCategory category = findEntity(id);
        String name = form.getName().trim();
        validateUniqueName(name, id);

        category.setName(name);
        category.setDescription(
                normalizeNullable(form.getDescription()));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void activate(Long id) {
        findEntity(id).activate();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(Long id) {
        findEntity(id).deactivate();
    }

    private EventCategory findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La categoría solicitada no existe."));
    }

    private void validateUniqueName(String name, Long excludedId) {
        boolean duplicated = excludedId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                name,
                                excludedId);

        if (duplicated) {
            throw new DuplicateResourceException(
                    "name",
                    "Ya existe una categoría con ese nombre.");
        }
    }

    private CategoryListItem toListItem(EventCategory category) {
        return new CategoryListItem(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt());
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
