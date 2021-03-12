package io.github.rbajek.rasa.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rbajek.rasa.sdk.action.Action;
import io.github.rbajek.rasa.sdk.dto.ActionRequest;
import io.github.rbajek.rasa.sdk.dto.ActionResponse;
import io.github.rbajek.rasa.sdk.dto.event.AbstractEvent;
import io.github.rbajek.rasa.sdk.exception.RasaException;
import io.github.rbajek.rasa.sdk.util.StringUtils;

/**
 * Action executor
 *
 * @author Rafał Bajek
 */
public class ActionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionExecutor.class);

    private Map<String, Action> actions = new HashMap<>();

    public void registerAction(Action action) {
        if(isEmpty(action.name())) {
            throw new RasaException("An action must implement a name");
        }
        for (String nameOfOne: action.name()){
            if (this.actions.get(nameOfOne) != null && this.actions.get(nameOfOne) != action){
                throw new RuntimeException("Duplicate actions mapping for "+nameOfOne);
            }
            this.actions.put(nameOfOne, action);

        }
        LOGGER.info("Registered action for '{}'.", action.name());
    }

    private boolean isEmpty(String[] name) {
        for (String nameOfOne: name){
            if (StringUtils.isNullOrEmpty(nameOfOne)){
                return true;
            }
        }
        return false;
    }

    private void validateEvents(List<AbstractEvent> events, String actionName) {
        Iterator<AbstractEvent> eventsIterator = events.iterator();
        while (eventsIterator.hasNext()) {
            AbstractEvent event = eventsIterator.next();
            if(StringUtils.isNullOrEmpty(event.getEvent())) {
                LOGGER.error("Your action '{}' returned an event without the 'event' property. Event will be ignored! Event: {}", actionName, event);
                eventsIterator.remove();
            }
        }
    }

    public ActionResponse run(ActionRequest actionRequest) {
        // Check for version of Rasa.
        VersionChecker.checkVersionCompatibility(actionRequest.getVersion());

        if(StringUtils.isNotNullOrEmpty(actionRequest.getNextAction())) {
            LOGGER.debug("Received request to run '{}'", actionRequest.getNextAction());
            Action action = actions.get(actionRequest.getNextAction());
            if(action == null) {
                throw new RasaException("No registered Action found for name '"+actionRequest.getNextAction()+"'.");
            }

            CollectingDispatcher dispatcher = new CollectingDispatcher();
            List<AbstractEvent> events = action.run(dispatcher, actionRequest.getTracker(), actionRequest.getDomain());
            if(events == null) {
                // make sure the action did not just return "null"...
                events = Collections.emptyList();
            }
            validateEvents(events, actionRequest.getNextAction());
            LOGGER.debug("Finished running '{}'", actionRequest.getNextAction());
            ActionResponse actionResponse = new ActionResponse();
            actionResponse.setEvents(events);
            // Rasa API require list of key-value pair objects
            actionResponse.setResponses(Arrays.asList(dispatcher.getMessages()));

            return actionResponse;
        }
        LOGGER.warn("Received an action call without an action.");
        return null;
    }

    public List<String> getRegisteredActionNames() {
        return new ArrayList<>(this.actions.keySet());
    }
}
