package com.freightfox.transporter.service.optimization;

import com.freightfox.transporter.domain.entity.Lane;
import com.freightfox.transporter.domain.entity.LaneQuote;
import com.freightfox.transporter.domain.entity.Transporter;
import com.freightfox.transporter.exception.OptimizationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransporterAssignmentOptimizerTest {

    private final TransporterAssignmentOptimizer optimizer =
            new TransporterAssignmentOptimizer();

    @Test
    void findsMinimumCostUsingMaximumAllowedTransporters() {

        Lane lane1 = new Lane(1L, "A", "B");
        Lane lane2 = new Lane(2L, "B", "C");
        Lane lane3 = new Lane(3L, "C", "D");

        Transporter t1 = new Transporter(1L, "T1");
        Transporter t2 = new Transporter(2L, "T2");
        Transporter t3 = new Transporter(3L, "T3");

        List<LaneQuote> quotes = List.of(
                new LaneQuote(lane1, t1, new BigDecimal("5")),
                new LaneQuote(lane1, t2, new BigDecimal("2")),

                new LaneQuote(lane2, t1, new BigDecimal("1")),
                new LaneQuote(lane2, t3, new BigDecimal("5")),

                new LaneQuote(lane3, t2, new BigDecimal("5")),
                new LaneQuote(lane3, t3, new BigDecimal("1"))
        );

        var result = optimizer.optimize(
                List.of(lane1, lane2, lane3),
                quotes,
                2
        );

        assertEquals(
                0,
                result.totalCost().compareTo(new BigDecimal("7"))
        );

        assertEquals(
                List.of(1L, 3L),
                result.selectedTransporters()
        );

        assertEquals(
                List.of(
                        new TransporterAssignmentOptimizer.LaneAssignment(1L, 1L),
                        new TransporterAssignmentOptimizer.LaneAssignment(2L, 1L),
                        new TransporterAssignmentOptimizer.LaneAssignment(3L, 3L)
                ),
                result.assignments()
        );
    }

    @Test
    void prioritizesUsingMoreTransportersWhenLimitAllowsIt() {

        Lane lane1 = new Lane(1L, "A", "B");
        Lane lane2 = new Lane(2L, "B", "C");

        Transporter t1 = new Transporter(1L, "T1");
        Transporter t2 = new Transporter(2L, "T2");

        List<LaneQuote> quotes = List.of(
                new LaneQuote(lane1, t1, new BigDecimal("1")),
                new LaneQuote(lane2, t1, new BigDecimal("1")),
                new LaneQuote(lane1, t2, new BigDecimal("100"))
        );

        var result = optimizer.optimize(
                List.of(lane1, lane2),
                quotes,
                2
        );

        assertEquals(
                List.of(1L, 2L),
                result.selectedTransporters()
        );

        assertEquals(
                0,
                result.totalCost().compareTo(new BigDecimal("101"))
        );
    }

    @Test
    void rejectsLaneWithoutAnyQuotes() {

        Lane lane1 = new Lane(1L, "A", "B");
        Lane lane2 = new Lane(2L, "B", "C");

        Transporter t1 = new Transporter(1L, "T1");

        List<LaneQuote> quotes = List.of(
                new LaneQuote(lane1, t1, new BigDecimal("100"))
        );

        assertThrows(
                OptimizationException.class,
                () -> optimizer.optimize(
                        List.of(lane1, lane2),
                        quotes,
                        2
                )
        );
    }

    @Test
    void rejectsZeroTransporterLimit() {

        Lane lane = new Lane(1L, "A", "B");
        Transporter transporter = new Transporter(1L, "T1");

        List<LaneQuote> quotes = List.of(
                new LaneQuote(lane, transporter, new BigDecimal("100"))
        );

        assertThrows(
                OptimizationException.class,
                () -> optimizer.optimize(
                        List.of(lane),
                        quotes,
                        0
                )
        );
    }

    @Test
    void rejectsMoreThanSixteenActiveTransporters() {

        Lane lane = new Lane(1L, "A", "B");

        List<LaneQuote> quotes = new ArrayList<>();

        for (long id = 1; id <= 17; id++) {

            Transporter transporter =
                    new Transporter(id, "T" + id);

            quotes.add(new LaneQuote(
                    lane,
                    transporter,
                    new BigDecimal("100")
            ));
        }

        assertThrows(
                OptimizationException.class,
                () -> optimizer.optimize(
                        List.of(lane),
                        quotes,
                        3
                )
        );
    }
}