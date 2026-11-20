package com.freightfox.transporter.domain.repository;

import com.freightfox.transporter.domain.entity.LaneQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LaneQuoteRepository
        extends JpaRepository<LaneQuote, Long> {

    @Query("""
        SELECT q
        FROM LaneQuote q
        JOIN FETCH q.lane
        JOIN FETCH q.transporter
        ORDER BY q.lane.id, q.transporter.id
        """)
    List<LaneQuote> findAllForOptimization();
}