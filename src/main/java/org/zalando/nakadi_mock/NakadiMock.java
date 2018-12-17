package org.zalando.nakadi_mock;

import com.jayway.jsonpath.TypeRef;

import java.net.URL;

/**
 * A mock of Nakadi. This interface allows to configure the mock, and to
 * retrieve an URL at which it can be reached over HTTP. A typical use looks
 * like this:
 *
 * <pre>
 * NakadiMock mock = NakadiMock.make();
 * mock.start();
 * ...
 * mock.stop();
 *
 * </pre>
 */
public interface NakadiMock {
    void start();

    interface EventType {
        String getName();

        /**
         * Sets the callback for this event type. The type used for parsing is
         * extracted from the callback.
         *
         * @param callback the callback.
         *            <p>
         *            For this method to work, the type parameter needs to be
         *            frozen into the class implementing the callback (e.g. by
         *            using {@code new EventSubmissionCallback<MyClass>(){...}}.
         *            If you have only a generic class or a lambda function, use
         *            one of the other methods with same name, which take a type
         *            token in addition.
         *            </p>
         */
        <T> void setSubmissionCallback(EventSubmissionCallback<T> callback);

        /**
         * Sets the callback for this event type. The type used for parsing is
         * passed separately.
         *
         * @param type a type reference for the event type. You can use e.g.
         *            <code>{@code new TypeRef<Map<String, String>>}(){}}</code>
         *            to get such a type reference.
         * @param callback the callback function, which takes a batch of events
         *            and decides how to answer.
         */
        <T> void setSubmissionCallback(TypeRef<T> type, EventSubmissionCallback<T> callback);

        /**
         * Sets the callback for this event type. The type used for parsing is
         * passed separately.
         *
         * @param type a class object for the event type. Use this only if you
         *            have a class without type parameters.
         * @param callback the callback function, which takes a batch of events
         *            and decides how to answer.
         */
        <T> void setSubmissionCallback(Class<T> type, EventSubmissionCallback<T> callback);
    }

    EventType eventType(String name);

    void stop();

    URL getRootUrl();

    static NakadiMock make() {
        return new NakadiMockImpl();
    }
}
