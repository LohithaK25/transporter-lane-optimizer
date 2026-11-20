package com.freightfox.transporter.domain.repository;

import com.freightfox.transporter.domain.entity.Lane;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaneRepository extends JpaRepository<Lane, Long> {
}