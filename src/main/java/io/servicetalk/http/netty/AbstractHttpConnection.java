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
package io.servicetalk.http.netty;

import io.servicetalk.concurrent.api.Completable;
import io.servicetalk.concurrent.api.Publisher;
import io.servicetalk.concurrent.api.Single;
import io.servicetalk.http.api.HttpConnection;
import io.servicetalk.http.api.HttpPayloadChunk;
import io.servicetalk.http.api.HttpRequest;
import io.servicetalk.http.api.HttpResponse;
import io.servicetalk.http.api.HttpResponseMetaData;
import io.servicetalk.http.api.HttpResponses;
import io.servicetalk.http.api.LastHttpPayloadChunk;
import io.servicetalk.transport.api.ConnectionContext;
import io.servicetalk.transport.api.ExecutionContext;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.servicetalk.concurrent.api.Publisher.error;
import static io.servicetalk.concurrent.api.Publisher.just;
import static io.servicetalk.http.netty.HeaderUtils.addRequestTransferEncodingIfNecessary;
import static io.servicetalk.http.netty.SpliceFlatStreamToMetaSingle.flatten;
import static java.util.Objects.requireNonNull;

abstract class AbstractHttpConnection<CC extends ConnectionContext> extends HttpConnection {

    // TODO consolidate with http server logic
    private static final Predicate<HttpPayloadChunk> LAST_CHUNK_PREDICATE = p -> p instanceof LastHttpPayloadChunk;
    private static final Supplier<HttpPayloadChunk> LAST_CHUNK_SUPPLIER = () -> EmptyLastHttpPayloadChunk.INSTANCE;

    protected final CC connection;
    protected final ReadOnlyHttpClientConfig config;
    protected final ExecutionContext executionContext;

    protected AbstractHttpConnection(CC connection,
                                     ReadOnlyHttpClientConfig config,
                                     ExecutionContext executionContext) {
        this.connection = requireNonNull(connection);
        this.config = requireNonNull(config);
        this.executionContext = requireNonNull(executionContext);
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connection;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Publisher<T> getSettingStream(final SettingKey<T> settingKey) {
        if (settingKey == SettingKey.MAX_CONCURRENCY) {
            return (Publisher<T>) just(config.getMaxPipelinedRequests());
        }
        return error(new IllegalArgumentException("Unknown setting: " + settingKey));
    }

    @Override
    public Single<HttpResponse<HttpPayloadChunk>> request(HttpRequest<HttpPayloadChunk> request) {
        addRequestTransferEncodingIfNecessary(request); // See https://tools.ietf.org/html/rfc7230#section-3.3.3
        return new SpliceFlatStreamToMetaSingle<HttpResponse<HttpPayloadChunk>, HttpResponseMetaData, HttpPayloadChunk>(
                writeAndRead(flatten(executionContext.getExecutor(), request, AbstractHttpConnection::unpack)),
                AbstractHttpConnection::newResponse);
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    protected abstract Publisher<Object> writeAndRead(Publisher<Object> stream);

    private static Publisher<HttpPayloadChunk> unpack(HttpRequest<HttpPayloadChunk> request) {
        return request.getPayloadBody().liftSynchronous(new EnsureLastItemBeforeCompleteOperator<>(
                LAST_CHUNK_PREDICATE, LAST_CHUNK_SUPPLIER));
    }

    private static HttpResponse<HttpPayloadChunk> newResponse(HttpResponseMetaData meta,
                                                              Publisher<HttpPayloadChunk> pub) {
        return HttpResponses.newResponse(meta.getVersion(), meta.getStatus(), pub, meta.getHeaders());
    }

    @Override
    public Completable onClose() {
        return connection.onClose();
    }

    @Override
    public Completable closeAsync() {
        return connection.closeAsync();
    }
}
