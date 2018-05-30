# Nakadi Mock

A mock implementation of a part of the [Nakadi](https://nakadi.io/index.html) API for testing purposes.

## Overview

Nakadi is an event-bus used for Micro-service communication in Zalando.
This library is intended to use when testing Java applications (or libraries) which interact with Nakadi.
Instead of running a complete Nakadi implementation (with database, Kafka, etc.), you can just use `NakadiMock.start()`.

## Planned features:

### Event Submission

Simple:

* Setup: define event type, get submission URL
* Test: Have the tested code submit events via HTTP (all are simply accepted as-is)
* Assert: access the submitted events to check if they are what was expected

With callback:

* Setup: define event type, register callback, get submission URL
* Test:
    * Have the tested code submit events via HTTP
    * get callbacks for the events
    * have the callback decide whether to accept all/some/none of the events
    * Your tested code gets a HTTP response back
* Assert: (?)

### Event consumption

We will only support the subscription API, not the low-level consumption API (which is deprecated anyways).


* creating subscriptions (or retrieving an existing one)
* reading from a subscription
* committing cursors


### Event type management

Managing of event types is of lower priority, as that is a more
occassional task, not one done regularly (and needs less testing).
