package il.ac.bgu.cs.bp.samplebpjsproject;


import il.ac.bgu.cs.bp.bpjs.model.BEvent;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.SyncStatement;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionResult;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionStrategy;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.ComposableEventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSets;
import org.mozilla.javascript.Context;

import java.util.*;
import java.util.stream.Collectors;

/**
 * New Event Selection Strategy.
 *
 * 1. Hot SyncStatements go first, and turn cold when selected once.
 * 2. If selected hot SyncStatements isn't requesting any events, it's turn is given to a "GetSensorData" event from external queue (if one exists).
 * 3. "Update" Events go next. When an "Update" event is selected all other SyncStatements are turned hot.
 * 4. The rest of the events are joined by the events from the external queue and all are sent to selection together.
 *
 * Whenever a "Subscribe" event is selected, the external event queue is cleared.
 * This prevents "GetSensorData" events that are missing newly subscribed ports from being selected.
 */
public class UpdateMakeAllHotSelectionStrategy implements EventSelectionStrategy {

    public Set<BEvent> selectableEvents(BProgramSyncSnapshot bProgramSyncSnapshot) {

        Set<SyncStatement> statements = bProgramSyncSnapshot.getStatements();
        List<BEvent> externalEvents = bProgramSyncSnapshot.getExternalEvents();

        // Create EventSet of blocked SyncStatements
        EventSet blocked = new ComposableEventSet.AnyOf(
                statements
                        .stream()
                        .filter(Objects::nonNull)
                        .map(SyncStatement::getBlock)
                        .filter((r) -> r != EventSets.none)
                        .collect(Collectors.toSet()));

        // Create set of hot events
        Set<BThreadSyncSnapshot> hotBThreads = bProgramSyncSnapshot.getBThreadSnapshots()
                .stream()
                .filter(bThread -> Objects.nonNull(bThread.getSyncStatement()) && bThread.getSyncStatement().isHot())
                .collect(Collectors.toSet());

        // If there are any hot b-threads, select one and make it cold.
        // we need to select the winning SyncStatement here so we can turn it cold.
        // before we send it's requested events for selection.
        if (!hotBThreads.isEmpty()){
            for (BThreadSyncSnapshot chosenBThread : hotBThreads) {
                // Make b-thread's SyncStatement cold.
                chosenBThread.setSyncStatement(chosenBThread.getSyncStatement().hot(false));
                Set<BEvent> requestedEvents = getRequestedAndNotBlocked(chosenBThread.getSyncStatement(), blocked);

                if (!requestedEvents.isEmpty()) {
                    return requestedEvents;
                }


                // If chosen hot b-thread has no requested events, it might be waiting for
                // data from an event in the external event set.
                // So if external event set has a "GetSensorsData" event, let it through in the b-threads turn.

                // tl:dr hot thread with no requested events lets "GetSensorsData" event use its turn.
                Optional<BEvent> getSensorsDataEvent = externalEvents
                        .stream()
                        .filter(e -> e.name.equals("GetSensorsData"))
                        .findFirst();

                if (getSensorsDataEvent.isPresent()) {
                    requestedEvents.add(getSensorsDataEvent.get());
                    return requestedEvents;
                }

                // No "GetSensorsData" event found, try the next hot b-thread...
            }
            // No hot thread was selected.
            // keep going...
        }

        // If reached this, there are no hot b-threads
        Optional<SyncStatement> update = statements
                .stream()
                .filter(s -> s
                        .getRequest()
                        .stream()
                        .filter(Objects::nonNull)
                        .anyMatch((e) -> e.name.equals("Update") & !getRequestedAndNotBlocked(s, blocked).isEmpty())).findFirst();

        try {
            Context.enter();
            if (update.isPresent()){

                // Make all b-threads hot
                bProgramSyncSnapshot.getBThreadSnapshots().forEach(t -> t.setSyncStatement(t.getSyncStatement().hot(true)));

                // Return "Update" event first,
                // On the next sync point, the hot b-threads will be selected.
                SyncStatement updateStatement = update.get();

                return getRequestedAndNotBlocked(updateStatement, blocked);
            }

            // If reached this, there is no "Update" Event and there are no hot b-threads.
            // Mix all the rest BEvents and return them
            Set<BEvent> rest = statements
                    .stream()
                    .filter(Objects::nonNull)
                    .flatMap(s -> getRequestedAndNotBlocked(s, blocked).stream())
                    .collect(Collectors.toSet());

            Set<BEvent> filteredExtEvents = externalEvents
                    .stream()
                    .filter((e) -> !blocked.contains(e))
                    .collect(Collectors.toSet());
            rest.addAll(filteredExtEvents);
            return rest;

        } finally {
            Context.exit();
        }

    }

    @Override
    public Optional<EventSelectionResult> select(BProgramSyncSnapshot bProgramSyncSnapshot, Set<BEvent> set) {
        if (set.isEmpty()){
            return Optional.empty();
        }
        BEvent selectedEvent = set.iterator().next();
        if (selectedEvent.name.equals("Subscribe")){
            bProgramSyncSnapshot.getExternalEvents().clear();
        }
        return Optional.of(new EventSelectionResult(selectedEvent));
    }

    private Set<BEvent> getRequestedAndNotBlocked(SyncStatement stmt, EventSet blocked) {
        Set<BEvent> var3;
        try {
            Context.enter();
            var3 = stmt
                    .getRequest()
                    .stream()
                    .filter((req) -> !blocked.contains(req))
                    .collect(Collectors.toSet());
        } finally {
            Context.exit();
        }

        return var3;
    }
}
