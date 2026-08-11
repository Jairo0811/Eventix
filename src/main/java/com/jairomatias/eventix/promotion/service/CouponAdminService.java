package com.jairomatias.eventix.promotion.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.promotion.dto.CouponEventOption;
import com.jairomatias.eventix.promotion.dto.CouponForm;
import com.jairomatias.eventix.promotion.dto.CouponListItem;

public interface CouponAdminService {

    Page<CouponListItem> findAll(
            String term,
            Boolean active,
            Pageable pageable);

    List<CouponEventOption> findEventOptions();

    CouponForm getForm(Long id);

    Long create(CouponForm form);

    void update(Long id, CouponForm form);

    void activate(Long id);

    void deactivate(Long id);
}
