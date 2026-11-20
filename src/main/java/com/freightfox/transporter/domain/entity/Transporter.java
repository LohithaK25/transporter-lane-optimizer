package com.freightfox.transporter.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transporters")
public class Transporter {

    @Id
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    protected Transporter() {
    }

    public Transporter(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}