# Nakadi Mock

A mock implementation of a part of the [Nakadi](https://nakadi.io/index.html) API for testing purposes.

## Overview

Nakadi is an event-bus used for Micro-service communication in Zalando.
This library is intended to use when testing Java applications (or libraries) which interact with Nakadi.
Instead of running a complete Nakadi implementation (with database, Kafka, etc.), you can just use `NakadiMock.start()`.


## Mocking Event Submission


### Just ignore events

In the simplest case, you just want your application to not fail, and do not actually care about any events.

```java
NakadiMock mock = NakadiMock.make();
mock.eventType("my-event");
mock.start();
URL nakadiUrl = mock.getRootUrl();

// configure application to use that URL
// do whatever tests you need

mock.stop();
```

The default configuration for any event type is to accept all submissions, and ignore the events.


### Get submitted events

In the next more complicated case, you just want to know which events arrived at Nakadi, without caring about any error handling. For this, you can use our predefined "collecting callback", which also accepts all events (as long as they can be parsed as the correct type), and stores them for later retrieval.

```java
NakadiMock mock = NakadiMock.make();
EventSubmissionCallback.CollectingCallback<MyEventObject> collector =
   new EventSubmissionCallback.CollectingCallback<MyEventObject>() {}; 
mock.eventType("my-event")
    .setSubmissionCallback(collector);
mock.start();
URL nakadiUrl = mock.getRootUrl();

// configure application to use that URL
// get application to send events

mock.stop();

List<MyEventObject> submittedEvents = collector.getSubmittedEvents();
assertThat(submittedEvents, hasSize(5));

// You can also get the individual batches:
List<List<MyEventObject>> submittedBatches = collector.getSubmittedBatches();
```

You need to create an (usually anonymous) subclass of the CollectingCallback which fixes the type argument, as we are using that to determine which type to use for parsing.

### Test failure cases

How does your application react if the token is expired, or if the application is not allowed to submit events?

Use custom callbacks to check that. The callback interface EventSubmissionCallback has one method, which receives a list of events, and returns a NakadiSubmissionAnswer. When returning `.notAuthenticated()` or `.accessForbidden()`, NakadiMock will return a 401 or 403 to your application.

```java
NakadiMock mock = NakadiMock.make();
mock.eventType("my-event")
    .setSubmissionCallback(Object.class, batch -> NakadiSubmissionAnswer.notAuthenticated());
mock.start();
URL nakadiUrl = mock.getRootUrl();

// configure application to use that URL
// do whatever tests you need

mock.stop();
```
If you use a lambda function (as we did here) or method reference (or any class which is generic itself) to implement the EventSubmissionCallback, we can't get the actual type argument out of it, and it therefore has to be provided as a separate type token argument, so we know how to parse the events.

You can use a class object (like we did here) (if your event type has no type parameters on its own), or a `TypeRef` (from Jayway JsonPath), which also works with parameterized types like `Map<String, String>`.

### Partial failures

Nakadi can also return validation failures, if an event doesn't match the defined schema. In this case it returns a list of batch item responses, which for each event tell what happened. Same happens if something goes wrong when actually storing the events to the partitions – then e.g. one event is already subitted and another one can't.

You can simulate that by returning `NakadiSubmissionAnswer.partialValidation(...)` or `.partialSubmitted(...)`, each of which takes a list of `BatchItemResponse` objects, which contain the details.

```
NakadiMock mock = NakadiMock.make();
mock.eventType("my-event")
    .setSubmissionCallback(MyEventObject.class,
                batch -> NakadiSubmissionAnswer.partialSubmitted(
                        batch.stream()
                             .map(event -> event.bla != null
                                    ? new BatchItemResponse("123", PublishingStatus.SUBMITTED, null, null)
                                    : new BatchItemResponse("124", PublishingStatus.FAILED,
                                            PublishingProcessStep.VALIDATING, "Missing key 'bla'"))
                            .collect(Collectors.toList())));
mock.start();
URL nakadiUrl = mock.getRootUrl();

// configure application to use that URL
// make your application send events
// check that your application reacts correctly

```


## More Planned features:

### Event consumption

We will only support the subscription API, not the low-level consumption API (which is deprecated anyways).


* creating subscriptions (or retrieving an existing one)
* reading from a subscription
* committing cursors


### Event type management

Managing of event types is of lower priority, as that is a more
occasional task, not one done regularly (and needs less testing).
