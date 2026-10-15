package com.bsp.supportassistant.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A Spring Data repository must name an entity type, so this class exists for TicketProcedures.
// Nothing reads or saves it. The package does the work.
@Entity
@Table(name = "SUPPORT_TICKETS")
public class TicketEntity {

    @Id
    private Long id;

    protected TicketEntity() {
    }
}
