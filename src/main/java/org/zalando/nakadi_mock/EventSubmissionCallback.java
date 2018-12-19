package org.zalando.nakadi_mock;

import java.time.Instant;
import java.util.ArrayList;
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
     * A helper class which accepts all events of correct type, and just
     * collects them into two lists (one of all events, one of all batches).
     *
     * @author paulo
     *
     * @param <E> the class of the event type. As this needs to be a concrete
     *            type, you need to create a subclass of CollectingCallback with
     *            the correct type parameter. The easiest way of doing this is
     *            by using {@code new CollectingCallback<...>(){}}.
     */
    abstract class CollectingCallback<E> implements EventSubmissionCallback<E> {
        private List<List<E>> submittedBatches = new ArrayList<>();
        private List<E> submittedEvents = new ArrayList<>();

        @Override
        public NakadiSubmissionAnswer processBatch(List<E> batch) {
            submittedBatches.add(batch);
            submittedEvents.addAll(batch);
            return NakadiSubmissionAnswer.ok();
        }

        public List<List<E>> getSubmittedBatches() {
            return submittedBatches;
        }

        public List<E> getSubmittedEvents() {
            return submittedEvents;
        }
    }

    /**
     * A callback which accepts all events and ignores them. You usually don't
     * need to specify this, as this is the default for all event types.
     */
    EventSubmissionCallback<Object> IGNORING_CALLBACK = new EventSubmissionCallback<Object>() {
        @Override
        public NakadiSubmissionAnswer processBatch(List<Object> batch) {
            return NakadiSubmissionAnswer.ok();
        }
    };

    /**
     * Process a batch of events and return an answer.
     */
    NakadiSubmissionAnswer processBatch(List<Event> batch);

    /**
     * A helper class which can be used to parse the received event as a data
     * change event, with {@code <Data>} as the embedded data type. You can use
     * this as the type parameter for the EventSubmissionCallback.
     */
    public class DataChangeEvent<Data> {
        private Data data;
        private String dataType;
        private String dataOp;
        private MetaData metaData;

        public Data getData() {
            return data;
        }

        public String getDataType() {
            return dataType;
        }

        public String getDataOp() {
            return dataOp;
        }

        public MetaData getMetaData() {
            return metaData;
        }
    }

    /**
     * A helper class which can be used to get the meta data out of the submitted events.
     * This is part of the DataChangeEvent class, but needs to be integrated when you capture business events.
     */
    public static class MetaData {
        private String eid;
        private String eventType;
        private Instant occurredAt;
        private String flowId;

        public String getEid() {
            return eid;
        }

        public String getEventType() {
            return eventType;
        }

        public Instant getOccurredAt() {
            return occurredAt;
        }

        public String getFlowId() {
            return flowId;
        }

    }
}