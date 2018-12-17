package org.zalando.nakadi_mock;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.nakadi_mock.EventSubmissionCallback.CollectingCallback;
import org.zalando.nakadi_mock.NakadiSubmissionAnswer.BatchItemResponse;
import org.zalando.nakadi_mock.NakadiSubmissionAnswer.BatchItemResponse.PublishingProcessStep;
import org.zalando.nakadi_mock.NakadiSubmissionAnswer.BatchItemResponse.PublishingStatus;

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
        CollectingCallback<Map<String, String>> collector = new CollectingCallback<Map<String, String>>() {};
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

    @Test
    public void testSubmissionWithBatchItemReponsePartiallySubmitted() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(ExampleEvent.class,
                batch -> NakadiSubmissionAnswer.partialSubmitted(
                        batch.stream()
                             .map(event -> event.bla != null
                                    ? new BatchItemResponse("123", PublishingStatus.SUBMITTED, null, null)
                                    : new BatchItemResponse("124", PublishingStatus.FAILED,
                                            PublishingProcessStep.VALIDATING, "Missing key 'bla'"))
                            .collect(Collectors.toList())));
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        HttpURLConnection connection = submitEventsAndReturnConnection(submissionUrl(eventType), events);
        int status = connection.getResponseCode();
        assertThat(status, is(207));
        //String text = readFully(connection.getInputStream());
        DocumentContext document = JsonPath.parse(connection.getInputStream());
       // LOG.error("document: {}", document);
        assertThat(document.read("$"), hasSize(2));

        assertThat(document.read("$[0].eid"), is("123"));
        assertThat(document.read("$[0].publishing_status"), is("submitted"));

        assertThat(document.read("$[1].eid"), is("124"));
        assertThat(document.read("$[1].publishing_status"), is("failed"));
        assertThat(document.read("$[1].step"), is("validating"));
        assertThat(document.read("$[1].detail"), is("Missing key 'bla'"));
    }


    @Test
    public void testSubmissionWithBatchItemReponseValidationFailed() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(ExampleEvent.class,
                batch -> {
                    BatchItemResponse r1 = new BatchItemResponse("123", PublishingStatus.SUBMITTED, null, null);
                    BatchItemResponse r2 = new BatchItemResponse("124", PublishingStatus.FAILED,
                            PublishingProcessStep.VALIDATING, "Validation failed");
                    return NakadiSubmissionAnswer.partialValidation(Arrays.asList(r1, r2));
                });
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        HttpURLConnection connection = submitEventsAndReturnConnection(submissionUrl(eventType), events);
        int status = connection.getResponseCode();
        assertThat(status, is(422));

        DocumentContext document = JsonPath.parse(connection.getErrorStream());
        assertThat(document.read("$"), hasSize(2));

        assertThat(document.read("$[0].eid"), is("123"));
        assertThat(document.read("$[0].publishing_status"), is("submitted"));

        assertThat(document.read("$[1].eid"), is("124"));
        assertThat(document.read("$[1].publishing_status"), is("failed"));
        assertThat(document.read("$[1].step"), is("validating"));
        assertThat(document.read("$[1].detail"), is("Validation failed"));
    }

    @Test
    public void testSubmissionForbidden() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(new TypeRef<Map<String, String>>() {},
                batch -> NakadiSubmissionAnswer.accessForbidden());
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        HttpURLConnection connection = submitEventsAndReturnConnection(submissionUrl(eventType), events);
        int status = connection.getResponseCode();
        assertThat(status, is(403));
        mock.stop();
    }

    @Test
    public void testSubmissionNotAuthenticated() throws IOException {
        String eventType = "example-event";
        mock.eventType(eventType).setSubmissionCallback(new TypeRef<Map<String, String>>() {},
                batch -> NakadiSubmissionAnswer.notAuthenticated());
        mock.start();

        String events = "[{'bla':'blub'}, {'egal':'wie'}]".replace('\'', '"');
        HttpURLConnection connection = submitEventsAndReturnConnection(submissionUrl(eventType), events);
        int status = connection.getResponseCode();
        assertThat(status, is(401));

        mock.stop();
    }

    private static class ExampleEvent {
        public String bla;
        public String egal;
    }

    private String readFully(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder(stream.available());
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        char[] buffer = new char[500];
        int read;
        while (0 < (read = reader.read(buffer))) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        stream.close();
        return builder.toString();
    }

    private URL submissionUrl(String eventType) {
        try {
            return new URL(mock.getRootUrl(), "event-types/" + eventType + "/events");
        } catch (MalformedURLException e) {
            throw new RuntimeException("This should not happen", e);
        }
    }

    private void postDataToUrl(String events, URL eventSubmissionUrl) throws IOException {
        URLConnection connection = submitEventsAndReturnConnection(eventSubmissionUrl, events);
        connection.getInputStream().close();
    }

    private HttpURLConnection submitEventsAndReturnConnection(URL eventSubmissionUrl, String events)
            throws IOException, UnsupportedEncodingException {
        HttpURLConnection connection = (HttpURLConnection)eventSubmissionUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.connect();
        PrintStream out = new PrintStream(connection.getOutputStream(), true, "UTF-8");
        out.print(events);
        out.close();
        return connection;
    }
}
