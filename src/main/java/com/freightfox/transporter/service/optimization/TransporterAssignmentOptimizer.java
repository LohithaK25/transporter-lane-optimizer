package com.freightfox.transporter.service.optimization;

import com.freightfox.transporter.domain.entity.Lane;
import com.freightfox.transporter.domain.entity.LaneQuote;
import com.freightfox.transporter.exception.OptimizationException;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransporterAssignmentOptimizer {

    private static final int MAX_SUPPORTED_TRANSPORTERS = 16;

    public Result optimize(
            List<Lane> inputLanes,
            List<LaneQuote> inputQuotes,
            int maxTransporters
    ) {

        if (maxTransporters <= 0) {
            throw new OptimizationException(
                    "maxTransporters must be greater than zero."
            );
        }

        if (inputLanes == null || inputLanes.isEmpty()) {
            throw new OptimizationException(
                    "No lane data is available."
            );
        }

        if (inputQuotes == null || inputQuotes.isEmpty()) {
            throw new OptimizationException(
                    "No transporter quotes are available."
            );
        }

        List<Lane> lanes = inputLanes.stream()
                .sorted(Comparator.comparing(Lane::getId))
                .toList();

        List<Long> transporterIds = inputQuotes.stream()
                .map(quote -> quote.getTransporter().getId())
                .distinct()
                .sorted()
                .toList();

        if (transporterIds.size() > MAX_SUPPORTED_TRANSPORTERS) {
            throw new OptimizationException(
                    "Exact optimization supports at most "
                            + MAX_SUPPORTED_TRANSPORTERS
                            + " transporters with quotes. Received: "
                            + transporterIds.size()
            );
        }

        Map<Long, Integer> transporterIndexes = new HashMap<>();

        for (int i = 0; i < transporterIds.size(); i++) {
            transporterIndexes.put(transporterIds.get(i), i);
        }

        Map<Long, List<QuoteOption>> quotesByLane = new HashMap<>();

        for (LaneQuote quote : inputQuotes) {

            Long laneId = quote.getLane().getId();
            Long transporterId = quote.getTransporter().getId();

            int transporterIndex = transporterIndexes.get(transporterId);

            quotesByLane.computeIfAbsent(
                    laneId,
                    ignored -> new ArrayList<>()
            ).add(new QuoteOption(
                    transporterIndex,
                    transporterId,
                    quote.getQuote()
            ));
        }

        quotesByLane.values().forEach(options ->
                options.sort(
                        Comparator.comparing(QuoteOption::transporterId)
                )
        );

        int stateCount = 1 << transporterIds.size();

        State[] current = new State[stateCount];

        current[0] = new State(BigDecimal.ZERO, null);

        // Process every lane exactly once.
        for (Lane lane : lanes) {

            List<QuoteOption> options =
                    quotesByLane.get(lane.getId());

            if (options == null || options.isEmpty()) {
                throw new OptimizationException(
                        "No transporter quote available for lane "
                                + lane.getId()
                );
            }

            State[] next = new State[stateCount];

            for (int mask = 0; mask < stateCount; mask++) {

                State previousState = current[mask];

                if (previousState == null) {
                    continue;
                }

                for (QuoteOption option : options) {

                    int transporterBit =
                            1 << option.transporterIndex();

                    int newMask = mask | transporterBit;

                    BigDecimal newCost =
                            previousState.cost().add(option.quote());

                    State existing = next[newMask];

                    if (existing == null
                            || newCost.compareTo(existing.cost()) < 0) {

                        AssignmentNode path = new AssignmentNode(
                                previousState.path(),
                                new LaneAssignment(
                                        lane.getId(),
                                        option.transporterId()
                                )
                        );

                        next[newMask] = new State(newCost, path);
                    }
                }
            }

            current = next;
        }

        // First maximize distinct transporters used.
        // Then minimize the cost for that transporter count.
        int bestMask = -1;
        int bestCount = -1;
        State bestState = null;

        int allowedCount = Math.min(
                maxTransporters,
                transporterIds.size()
        );

        for (int mask = 1; mask < stateCount; mask++) {

            State candidate = current[mask];

            if (candidate == null) {
                continue;
            }

            int usedCount = Integer.bitCount(mask);

            if (usedCount > allowedCount) {
                continue;
            }

            if (usedCount > bestCount
                    || (usedCount == bestCount
                    && (bestState == null
                    || candidate.cost().compareTo(
                            bestState.cost()
                    ) < 0))) {

                bestCount = usedCount;
                bestMask = mask;
                bestState = candidate;
            }
        }

        if (bestState == null) {
            throw new OptimizationException(
                    "No feasible transporter assignment exists "
                            + "within the specified limit."
            );
        }

        List<LaneAssignment> assignments = new ArrayList<>();

        AssignmentNode node = bestState.path();

        while (node != null) {
            assignments.add(node.assignment());
            node = node.previous();
        }

        Collections.reverse(assignments);

        List<Long> selectedTransporters = new ArrayList<>();

        for (int i = 0; i < transporterIds.size(); i++) {

            if ((bestMask & (1 << i)) != 0) {
                selectedTransporters.add(transporterIds.get(i));
            }
        }

        return new Result(
                bestState.cost(),
                List.copyOf(assignments),
                List.copyOf(selectedTransporters)
        );
    }

    public record LaneAssignment(
            Long laneId,
            Long transporterId
    ) {
    }

    public record Result(
            BigDecimal totalCost,
            List<LaneAssignment> assignments,
            List<Long> selectedTransporters
    ) {
    }

    private record QuoteOption(
            int transporterIndex,
            Long transporterId,
            BigDecimal quote
    ) {
    }

    private record State(
            BigDecimal cost,
            AssignmentNode path
    ) {
    }

    private record AssignmentNode(
            AssignmentNode previous,
            LaneAssignment assignment
    ) {
    }
}