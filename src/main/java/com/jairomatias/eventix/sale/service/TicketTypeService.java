package com.jairomatias.eventix.sale.service;

import java.util.List;

import com.jairomatias.eventix.sale.dto.TicketTypeForm;
import com.jairomatias.eventix.sale.dto.TicketTypeView;

public interface TicketTypeService {

    List<TicketTypeView> findByEvent(
            Long eventId,
            String authenticatedLogin);

    TicketTypeForm getCreateForm(
            Long eventId,
            String authenticatedLogin);

    TicketTypeForm getUpdateForm(
            Long id,
            String authenticatedLogin);

    Long create(
            Long eventId,
            TicketTypeForm form,
            String authenticatedLogin);

    void update(
            Long id,
            TicketTypeForm form,
            String authenticatedLogin);
}
