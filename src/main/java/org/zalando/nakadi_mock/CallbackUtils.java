package org.zalando.nakadi_mock;


import java.util.ArrayList;
import java.util.List;

public class CallbackUtils {

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
    public abstract static class CollectingCallback<E> implements EventSubmissionCallback<E> {
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
     * A callback which accepts all events and ignores them.
     * You usually don't need to specify this, as this is the default for all event types.
     */
    public static final EventSubmissionCallback<Object> IGNORING_CALLBACK = new EventSubmissionCallback<Object>() {
        @Override
        public NakadiSubmissionAnswer processBatch(List<Object> batch) {
            return NakadiSubmissionAnswer.ok();
        }
    };

}
