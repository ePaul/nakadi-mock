package org.zalando.nakadi_mock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class NakadiSubmissionAnswer {

    private static NakadiSubmissionAnswer okAnswer = new ConstantNakadiSubmissionAnswer(200, null, null);
    private static NakadiSubmissionAnswer noAuthAnswer = new ConstantNakadiSubmissionAnswer(401,
            "application/problem+json", null /* TODO: add problem */);
    private static NakadiSubmissionAnswer forbiddenAnswer = new ConstantNakadiSubmissionAnswer(403,
            "application/problem+json", null /* TODO: add problem */);

    private static class ConstantNakadiSubmissionAnswer extends NakadiSubmissionAnswer {
        final String body;

        ConstantNakadiSubmissionAnswer(int status, String contentType, String body) {
            super(status, contentType);
            this.body = body;
        }

        @Override
        String getBody() {
            return body;
        }

    }

    private static class BatchResponseAnswer extends NakadiSubmissionAnswer {
        private List<NakadiSubmissionAnswer.BatchItemResponse> items;

        public BatchResponseAnswer(int status, List<NakadiSubmissionAnswer.BatchItemResponse> items) {
            super(status, "application/json");
            this.items = items;
        }

        @Override
        String getBody() {
            return items.stream().map(item -> item.toJSon()).collect(Collectors.joining(", ", "[", "]"));
        }
    }

    public static class BatchItemResponse {
        enum PublishingStatus {
            SUBMITTED, FAILED, ABORTED
        }

        enum PublishingProcessStep {
            NONE, VALIDATING, PARTITIONING, ENRICHING, PUBLISHING
        }

        final String eid;
        final PublishingStatus status;
        final PublishingProcessStep step;
        final String detail;

        public BatchItemResponse(String eid, PublishingStatus status, PublishingProcessStep step, String detail) {
            this.eid = eid;
            this.status = status;
            this.step = step;
            this.detail = detail;
        }

        public String toJSon() {
            Map<String, String> items = new LinkedHashMap<>();
            items.put("eid", eid);
            items.put("publishing_status", status.name().toLowerCase());
            if (step != null) {
                items.put("step", step.name().toLowerCase());
            }
            items.put("detail", detail);
            return
                items.entrySet()
                     .stream()
                     .filter(entry -> entry.getValue() != null)
                     .map(entry -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"")
                     .collect(Collectors.joining(", ", "{", "}"));
        }
    }

    public static NakadiSubmissionAnswer ok() {
        return okAnswer;
    }

    public static NakadiSubmissionAnswer partialSubmitted(List<NakadiSubmissionAnswer.BatchItemResponse> items) {
        return new BatchResponseAnswer(207, items);
    }

    public static NakadiSubmissionAnswer partialValidation(List<NakadiSubmissionAnswer.BatchItemResponse> items) {
        return new BatchResponseAnswer(422, items);
    }

    public static NakadiSubmissionAnswer notAuthenticated() {
        return noAuthAnswer;
    }

    public static NakadiSubmissionAnswer accessForbidden() {
        return forbiddenAnswer;
    }

    private NakadiSubmissionAnswer(int status, String contentType) {
        this.status = status;
        this.contentType = contentType;
    }

    final int status;
    final String contentType;

    abstract String getBody();

}