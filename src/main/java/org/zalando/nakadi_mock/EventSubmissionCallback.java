package org.zalando.nakadi_mock;

import java.util.List;

public interface EventSubmissionCallback<Event> {
    NakadiSubmissionAnswer processBatch(List<Event> batch);
}