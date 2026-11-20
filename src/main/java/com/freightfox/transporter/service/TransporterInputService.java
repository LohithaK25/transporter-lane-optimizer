package com.freightfox.transporter.service;

import com.freightfox.transporter.api.dto.request.LaneInput;
import com.freightfox.transporter.api.dto.request.LaneQuoteInput;
import com.freightfox.transporter.api.dto.request.SubmitInputRequest;
import com.freightfox.transporter.api.dto.request.TransporterInput;
import com.freightfox.transporter.domain.entity.Lane;
import com.freightfox.transporter.domain.entity.LaneQuote;
import com.freightfox.transporter.domain.entity.Transporter;
import com.freightfox.transporter.domain.repository.LaneQuoteRepository;
import com.freightfox.transporter.domain.repository.LaneRepository;
import com.freightfox.transporter.domain.repository.TransporterRepository;
import com.freightfox.transporter.exception.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransporterInputService {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;

    public TransporterInputService(
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository laneQuoteRepository
    ) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.laneQuoteRepository = laneQuoteRepository;
    }

    @Transactional
    public void replaceInputData(SubmitInputRequest request) {

        validateUniqueLaneIds(request.lanes());
        validateUniqueTransporterIds(request.transporters());

        Set<Long> laneIds = request.lanes()
                .stream()
                .map(LaneInput::id)
                .collect(Collectors.toSet());

        validateQuotes(request.transporters(), laneIds);

        // Remove the previous dataset in foreign-key-safe order.
        laneQuoteRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();

        // Persist lanes.
        List<Lane> lanes = request.lanes()
                .stream()
                .map(input -> new Lane(
                        input.id(),
                        input.origin(),
                        input.destination()
                ))
                .toList();

        Map<Long, Lane> lanesById = laneRepository.saveAll(lanes)
                .stream()
                .collect(Collectors.toMap(
                        Lane::getId,
                        Function.identity()
                ));

        // Persist transporters.
        List<Transporter> transporters = request.transporters()
                .stream()
                .map(input -> new Transporter(
                        input.id(),
                        input.name()
                ))
                .toList();

        Map<Long, Transporter> transportersById =
                transporterRepository.saveAll(transporters)
                        .stream()
                        .collect(Collectors.toMap(
                                Transporter::getId,
                                Function.identity()
                        ));

        // Construct and persist lane quotes.
        List<LaneQuote> quotes = request.transporters()
                .stream()
                .flatMap(transporterInput ->
                        transporterInput.laneQuotes()
                                .stream()
                                .map(quoteInput -> new LaneQuote(
                                        lanesById.get(quoteInput.laneId()),
                                        transportersById.get(transporterInput.id()),
                                        quoteInput.quote()
                                ))
                )
                .toList();

        laneQuoteRepository.saveAll(quotes);
    }

    private void validateUniqueLaneIds(List<LaneInput> lanes) {

        Set<Long> ids = new HashSet<>();

        for (LaneInput lane : lanes) {
            if (!ids.add(lane.id())) {
                throw new InvalidInputException(
                        "Duplicate lane ID: " + lane.id()
                );
            }
        }
    }

    private void validateUniqueTransporterIds(
            List<TransporterInput> transporters
    ) {

        Set<Long> ids = new HashSet<>();

        for (TransporterInput transporter : transporters) {
            if (!ids.add(transporter.id())) {
                throw new InvalidInputException(
                        "Duplicate transporter ID: " + transporter.id()
                );
            }
        }
    }

    private void validateQuotes(
            List<TransporterInput> transporters,
            Set<Long> laneIds
    ) {

        for (TransporterInput transporter : transporters) {

            Set<Long> quotedLaneIds = new HashSet<>();

            for (LaneQuoteInput quote : transporter.laneQuotes()) {

                if (!laneIds.contains(quote.laneId())) {
                    throw new InvalidInputException(
                            "Transporter " + transporter.id()
                                    + " references unknown lane ID: "
                                    + quote.laneId()
                    );
                }

                if (!quotedLaneIds.add(quote.laneId())) {
                    throw new InvalidInputException(
                            "Duplicate quote for lane " + quote.laneId()
                                    + " by transporter " + transporter.id()
                    );
                }
            }
        }
    }
}