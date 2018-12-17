package org.zalando.nakadi_mock;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.ListenerInfo;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.AllowedMethodsHandler;
import io.undertow.server.handlers.RequestBufferingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatch;

class NakadiMockImpl implements NakadiMock {
    private static HttpString CONTENT_TYPE_HEADER = new HttpString("Content-Type");

    Configuration jsonPathConfig = Configuration.builder().jsonProvider(new GsonJsonProvider())
            .mappingProvider(new GsonMappingProvider()).build();

    private class EventTypeImpl implements EventType {
        private final String name;
        private EventSubmissionCallback<?> callback = CallbackUtils.IGNORING_CALLBACK;

        private EventTypeImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setSubmissionCallback(EventSubmissionCallback<?> callback) {
            this.callback = callback;
        }

        private <T> NakadiSubmissionAnswer parseAndPassToCallback(EventSubmissionCallback<T> callback,
                DocumentContext document) {
            List<T> events = document.read("$.[*]", TypeUtils.getTypeRefFromCallback(callback));
            NakadiSubmissionAnswer answer = callback.processBatch(events);
            return answer;
        }

        private void handleSubmission(HttpServerExchange exchange, String requestContent) {
            DocumentContext document = JsonPath.parse(requestContent, jsonPathConfig);

            NakadiSubmissionAnswer answer = parseAndPassToCallback(callback, document);
            exchange.setStatusCode(answer.status);
            String responseContentType = answer.contentType;
            exchange.getResponseHeaders().put(CONTENT_TYPE_HEADER, responseContentType);

            String responseContent = answer.getBody();
            if (responseContent != null) {
                exchange.getResponseSender().send(responseContent);
            } else {
                exchange.endExchange();
            }
        }
    }

    private Map<String, EventTypeImpl> eventTypes = new HashMap<>();

    @Override
    public EventType eventType(String name) {
        EventTypeImpl type = new EventTypeImpl(name);
        eventTypes.put(name, type);
        return type;
    }

    private Undertow server;

    private void handleSubmission(HttpServerExchange exchange) throws Exception {
        PathTemplateMatch match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String eventTypeName = match.getParameters().get("type");
        EventTypeImpl type = eventTypes.get(eventTypeName);
        if (type != null) {
            new RequestBufferingHandler(ex -> ex.getRequestReceiver().receiveFullString(type::handleSubmission), 10)
            .handleRequest(exchange);
        } else {
            ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
        }
    }

    @Override
    public void start() {

        HttpHandler handler = Handlers.pathTemplate().add("/event-types/{type}/events",
                new AllowedMethodsHandler(this::handleSubmission, new HttpString("POST")));
        server = Undertow.builder()//
                .addHttpListener(0, "localhost") //
                .setHandler(handler) //
                .build();
        server.start();

    }

    @Override
    public URL getRootUrl() {
        ListenerInfo listenerInfo = server.getListenerInfo().get(0);
        InetSocketAddress address = (InetSocketAddress) listenerInfo.getAddress();
        try {
            return new URL(listenerInfo.getProtcol(), address.getHostString(), address.getPort(), "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("this should not happen!", e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
