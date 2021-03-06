/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.lisp.translate.read.trait;


import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L2;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L3;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.SubtableDumpParamsBuilder;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.OneEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.OneEidTableMapDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

/**
 * Provides common logic for reading Eid subtables
 */
public interface SubtableReader extends JvppReplyConsumer {

    SubtableDumpParams L2_PARAMS = new SubtableDumpParamsBuilder().setL2(L2).build();
    SubtableDumpParams L3_PARAMS = new SubtableDumpParamsBuilder().setL2(L3).build();

    default EntityDumpExecutor<OneEidTableMapDetailsReplyDump, SubtableDumpParams> createExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            final OneEidTableMapDump request = new OneEidTableMapDump();
            request.isL2 = checkNotNull(params, "Cannot bind null params").isL2();
            return getReplyForRead(vppApi.oneEidTableMapDump(request).toCompletableFuture(), identifier);
        };
    }
}
