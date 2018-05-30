package org.zalando.nakadi_mock;

import java.net.URL;

/**
 * A mock of Nakadi. This interface allows to configure the mock, and to retrieve an URL at which it can be reached over HTTP.
 * A typical use looks like this:
 * <pre>
 * NakadiMock mock = NakadiMock.make();
 *
 * </pre>
 */
public interface NakadiMock
{
    void start();

    interface EventType {
        String getName();
        void setSubmissionCallback(EventSubmissionCallback<?> callback);
    }

    EventType eventType(String name);
    void stop();
    URL getRootUrl();

    static NakadiMock make() {
        return new NakadiMockImpl();
    }
}
