package com.jairomatias.eventix.settlement.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.settlement.dto.SettlementActionForm;
import com.jairomatias.eventix.settlement.dto.SettlementCreateForm;
import com.jairomatias.eventix.settlement.dto.SettlementDetailsView;
import com.jairomatias.eventix.settlement.dto.SettlementListItem;
import com.jairomatias.eventix.settlement.dto.SettlementOrganizerOption;
import com.jairomatias.eventix.settlement.entity.SettlementStatus;

public interface OrganizerSettlementService {

    Page<SettlementListItem> findAll(
            SettlementStatus status,
            String authenticatedLogin,
            Pageable pageable);

    SettlementDetailsView findById(Long id, String authenticatedLogin);

    SettlementCreateForm getCreateForm();

    List<SettlementOrganizerOption> findOrganizerOptions();

    Long create(SettlementCreateForm form);

    void startProcessing(Long id, SettlementActionForm form);

    void markPaid(Long id, SettlementActionForm form);

    void markFailed(Long id, SettlementActionForm form);

    void cancel(Long id, SettlementActionForm form);
}
