package org.zalando.nakadi_mock;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.TypeRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.nakadi_mock.CallbackUtils.CollectingCallback;

public class NakadiMockTest {
    private static Logger LOG = LoggerFactory.getLogger(NakadiMockTest.class);

    private NakadiMock mock;

    @Before
    public void setUp() {
        mock = NakadiMock.make();
    }

    @After
    public void tearDown() {
        mock.stop();
    }

    @Test
    public void testStopOnly() {
        mock.stop();
    }

    @Test
    public void testStartStop() {
        mock.start();
        mock.stop();
    }

    @Test
    public void testSubmissionToUndefinedEventGives404() {
        mock.start();
        String eventType = "example";
        URL url = submissionUrl(eventType);
        try {
            postDataToUrl("[]", url);
            fail();
        } catch (IOException e) {
            assertThat(e, instanceOf(FileNotFoundException.class));
        }
        mock.stop();
    }

    @Test
    public void testSubmissionWithoutCallback() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType);
        mock.start();
        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        postDataToUrl(events, submissionUrl(eventType));
        mock.stop();
    }

    @Test
    public void testSubmissionWithCollectingCallback() throws IOException {
        CollectingCallback<Map<String, String>> collector = new CollectingCallback<Map<String, String>>() {
        };
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(collector);
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        postDataToUrl(events, submissionUrl(eventType));

        mock.stop();
        List<Map<String, String>> submittedEvents = collector.getSubmittedEvents();
        LOG.info("events: {}", submittedEvents);
        assertThat(submittedEvents, contains(hasEntry("bla", "blub"), hasEntry("egal", "wie")));

        LOG.info("Batches: {}", collector.getSubmittedBatches());
    }

    @Test
    public void testSubmissionWithCustomCallbackLambda() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(new TypeRef<Map<String, String>>() {},
                batch -> NakadiSubmissionAnswer.ok());
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        postDataToUrl(events, submissionUrl(eventType));

        mock.stop();
    }

    @Test
    public void testSubmissionWithCustomCallbackInnerClass() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(new EventSubmissionCallback<Map<String, String>>() {
            @Override
            public NakadiSubmissionAnswer processBatch(List<Map<String, String>> batch) {
                assertThat(batch.get(0).get("bla"), is(equalTo("blub")));
                assertThat(batch.get(1).get("egal"), is(equalTo("wie")));
                return NakadiSubmissionAnswer.ok();
            }
        });
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        postDataToUrl(events, submissionUrl(eventType));

        mock.stop();
    }

    @Test
    public void testSubmissionWithCustomCallbackLambdaExampleEvent() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(ExampleEvent.class,
                batch -> {
                    assertThat(batch.get(0).bla, is(equalTo("blub")));
                    assertThat(batch.get(1).egal, is(equalTo("wie")));
                    return NakadiSubmissionAnswer.ok();
                });
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        postDataToUrl(events, submissionUrl(eventType));

        mock.stop();
    }

    private static class ExampleEvent {
        public String bla;
        public String egal;
    }


    private URL submissionUrl(String eventType) {
        try {
            return new URL(mock.getRootUrl(), "event-types/" + eventType + "/events");
        } catch (MalformedURLException e) {
            throw new RuntimeException("This should not happen", e);
        }
    }

    private void postDataToUrl(String events, URL eventSubmissionUrl) throws IOException {
        URLConnection connection = eventSubmissionUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.connect();
        PrintStream out = new PrintStream(connection.getOutputStream(), true, "UTF-8");
        out.print(events);
        out.close();
        connection.getInputStream().close();
    }
}
