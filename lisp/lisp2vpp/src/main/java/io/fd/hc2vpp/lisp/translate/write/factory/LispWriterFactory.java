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

package io.fd.hc2vpp.lisp.translate.write.factory;


import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.INTERFACE_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCAL_MAPPING_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.REMOTE_MAPPING_CONTEXT;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.lisp.context.util.AdjacenciesMappingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.write.LispCustomizer;
import io.fd.hc2vpp.lisp.translate.write.PitrCfgCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Initialize writers for {@link Lisp}
 */
public final class LispWriterFactory extends AbstractLispWriterFactoryBase implements WriterFactory {

    private final NamingContext bridgeDomainContext;
    private final AdjacenciesMappingContext adjacenciesMappingContext;

    @Inject
    public LispWriterFactory(final FutureJVppCore vppApi,
                             @Named(INTERFACE_CONTEXT) final NamingContext interfaceContext,
                             @Named(LOCATOR_SET_CONTEXT) final NamingContext locatorSetContext,
                             @Named("bridge-domain-context") final NamingContext bridgeDomainContext,
                             @Named(LOCAL_MAPPING_CONTEXT) final EidMappingContext localMappingContext,
                             @Named(REMOTE_MAPPING_CONTEXT) final EidMappingContext remoteMappingContext,
                             @Named(ADJACENCIES_IDENTIFICATION_CONTEXT) final AdjacenciesMappingContext adjacenciesMappingContext) {
        super(InstanceIdentifier.create(Lisp.class), vppApi, interfaceContext, locatorSetContext, localMappingContext,
                remoteMappingContext);
        this.bridgeDomainContext = checkNotNull(bridgeDomainContext, "Bridge domain context cannot be null");
        this.adjacenciesMappingContext =
                checkNotNull(adjacenciesMappingContext, "Adjacencies mapping context cannot be null");
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.add(new GenericWriter<>(lispInstanceIdentifier, new LispCustomizer(vppApi)));

        VniTableWriterFactory.newInstance(lispInstanceIdentifier, vppApi, localMappingContext, remoteMappingContext,
                bridgeDomainContext, adjacenciesMappingContext)
                .init(registry);
        LocatorSetsWriterFactory.newInstance(lispInstanceIdentifier, vppApi, interfaceContext, locatorSetContext)
                .init(registry);
        MapResolversWriterFactory.newInstance(lispInstanceIdentifier, vppApi).init(registry);

        registry.add(new GenericWriter<>(lispInstanceIdentifier.child(LispFeatureData.class).child(PitrCfg.class),
                new PitrCfgCustomizer(vppApi)));
    }
}