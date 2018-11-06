/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.concurrent.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Subscriber;

import java.util.concurrent.ExecutionException;

import static io.servicetalk.concurrent.api.DeliberateException.DELIBERATE_EXCEPTION;
import static io.servicetalk.concurrent.api.Executors.from;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;

public class SubscribeThrowsTest {

    @Rule
    public final ExpectedException expectedException = none();

    @Test
    public void publisherSubscriberThrows() throws Exception {
        Publisher<String> p = new Publisher<String>() {
            @Override
            protected void handleSubscribe(final Subscriber<? super String> subscriber) {
                throw DELIBERATE_EXCEPTION;
            }
        };
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        p.toFuture().get();
    }

    @Test
    public void publisherExecutorThrows() throws Exception {
        Publisher<String> p = Publisher.just("Hello")
                .publishAndSubscribeOn(from(task -> {
                    throw DELIBERATE_EXCEPTION;
                }));
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        p.toFuture().get();
    }

    @Test
    public void singleSubscriberThrows() throws Exception {
        Single<String> s = new Single<String>() {
            @Override
            protected void handleSubscribe(final Subscriber subscriber) {
                throw DELIBERATE_EXCEPTION;
            }
        };
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        s.toFuture().get();
    }

    @Test
    public void singleExecutorThrows() throws Exception {
        Single<String> s = Single.success("Hello")
                .publishAndSubscribeOn(from(task -> {
                    throw DELIBERATE_EXCEPTION;
                }));
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        s.toFuture().get();
    }

    @Test
    public void completableSubscriberThrows() throws Exception {
        Completable c = new Completable() {
            @Override
            protected void handleSubscribe(final Subscriber subscriber) {
                throw DELIBERATE_EXCEPTION;
            }
        };
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        c.toFuture().get();
    }

    @Test
    public void completableExecutorThrows() throws Exception {
        Completable c = Completable.completed()
                .publishAndSubscribeOn(from(task -> {
                    throw DELIBERATE_EXCEPTION;
                }));
        expectedException.expect(instanceOf(ExecutionException.class));
        expectedException.expectCause(is(DELIBERATE_EXCEPTION));
        c.toFuture().get();
    }
}
