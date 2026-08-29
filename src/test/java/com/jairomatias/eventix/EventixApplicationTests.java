package com.jairomatias.eventix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jairomatias.eventix.category.repository.EventCategoryRepository;

@SpringBootTest
@ActiveProfiles("test")
class EventixApplicationTests {

    @Autowired
    private EventCategoryRepository categoryRepository;

    @Test
    void applicationContextLoads() {
    }

    @Test
    void flywaySeedsCurrentEventCategories() {
        org.assertj.core.api.Assertions
                .assertThat(categoryRepository.findAllByActiveTrueOrderByNameAsc())
                .extracting(category -> category.getName())
                .containsExactlyInAnyOrder(
                        "Conferencia",
                        "Concierto",
                        "Deportivo",
                        "Taller",
                        "Promoción escolar");
    }
}
