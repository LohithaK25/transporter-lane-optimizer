package com.freightfox.transporter.domain.repository;

import com.freightfox.transporter.domain.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository
        extends JpaRepository<Transporter, Long> {
}