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
package io.servicetalk.http.api;

import io.servicetalk.client.api.GroupKey;
import io.servicetalk.concurrent.api.Completable;
import io.servicetalk.http.api.BlockingAggregatedHttpClient.BlockingAggregatedReservedHttpConnection;

import static io.servicetalk.concurrent.internal.Await.awaitIndefinitelyNonNull;
import static java.util.Objects.requireNonNull;

final class AggregatedHttpClientGroupToBlockingAggregatedHttpClientGroup<UnresolvedAddress>
        extends BlockingAggregatedHttpClientGroup<UnresolvedAddress> {
    private final AggregatedHttpClientGroup<UnresolvedAddress> clientGroup;

    AggregatedHttpClientGroupToBlockingAggregatedHttpClientGroup(
            AggregatedHttpClientGroup<UnresolvedAddress> clientGroup) {
        this.clientGroup = requireNonNull(clientGroup);
    }

    @Override
    public AggregatedHttpResponse<HttpPayloadChunk> request(final GroupKey<UnresolvedAddress> key,
                                                            final AggregatedHttpRequest<HttpPayloadChunk> request)
            throws Exception {
        // It is assumed that users will always apply timeouts at the HttpService layer (e.g. via filter). So we don't
        // apply any explicit timeout here and just wait forever.
        return awaitIndefinitelyNonNull(clientGroup.request(key, request));
    }

    @Override
    public BlockingAggregatedReservedHttpConnection reserveConnection(final GroupKey<UnresolvedAddress> key,
                                                                  final AggregatedHttpRequest<HttpPayloadChunk> request)
            throws Exception {
        return awaitIndefinitelyNonNull(clientGroup.reserveConnection(key, request))
                .asBlockingAggregatedReservedConnection();
    }

    @Override
    public void close() throws Exception {
        BlockingUtils.close(clientGroup);
    }

    @Override
    AggregatedHttpClientGroup<UnresolvedAddress> asAggregatedClientGroupInternal() {
        return clientGroup;
    }

    Completable onClose() {
        return clientGroup.onClose();
    }
}
