package org.zalando.nakadi_mock;


import java.util.ArrayList;
import java.util.List;

public class CallbackUtils {

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

    public static final EventSubmissionCallback<Object> IGNORING_CALLBACK = new EventSubmissionCallback<Object>() {
        @Override
        public NakadiSubmissionAnswer processBatch(List<Object> batch) {
            return NakadiSubmissionAnswer.ok();
        }
    };

}
