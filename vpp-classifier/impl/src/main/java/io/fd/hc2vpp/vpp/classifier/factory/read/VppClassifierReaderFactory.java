/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.vpp.classifier.factory.read;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.read.ClassifySessionReader;
import io.fd.hc2vpp.vpp.classifier.read.ClassifyTableReader;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.VppClassifierStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.VppClassifierState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.rev170327.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class VppClassifierReaderFactory implements ReaderFactory {

    private final FutureJVppCore jvpp;
    private final VppClassifierContextManager classifyCtx;

    @Inject
    public VppClassifierReaderFactory(final FutureJVppCore jvpp,
                                      @Named("classify-table-context") final VppClassifierContextManager classifyCtx) {
        this.jvpp = jvpp;
        this.classifyCtx = classifyCtx;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // VppClassifierState
        final InstanceIdentifier<VppClassifierState> vppStateId = InstanceIdentifier.create(VppClassifierState.class);
        registry.addStructuralReader(vppStateId, VppClassifierStateBuilder.class);
        //  ClassifyTable
        final InstanceIdentifier<ClassifyTable> classTblId = vppStateId.child(ClassifyTable.class);
        registry.add(new GenericInitListReader<>(classTblId, new ClassifyTableReader(jvpp, classifyCtx)));
        //   ClassifySession
        final InstanceIdentifier<ClassifySession> classSesId = classTblId.child(ClassifySession.class);
        registry.add(new GenericListReader<>(classSesId, new ClassifySessionReader(jvpp, classifyCtx)));
    }
}
