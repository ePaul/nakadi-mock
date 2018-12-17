package org.zalando.nakadi_mock;

import java.util.List;

/**
 * A callback interface, which is used by NakadiMock to figure out what to do
 * with a batch of events.
 *
 * @param <Event> the type of the event class. This needs to be frozen as a
 *            concrete type (not a type argument) into any implementation class,
 *            as we are using this to parse the actual events. Unfortunately
 *            this means you can't use a lambda function to implement this
 *            interface.
 */
public interface EventSubmissionCallback<Event> {
    /**
     * Process a batch of events and return an answer.
     */
    NakadiSubmissionAnswer processBatch(List<Event> batch);
}