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

package io.fd.hc2vpp.vpp.classifier.read.acl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterface;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp._interface.acl.rev170315.VppInterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ingress ACLs enabled on given interface.
 */
public class AclCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<Ingress, IngressBuilder>, AclReader, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AclCustomizer.class);
    private final NamingContext interfaceContext;
    private final VppClassifierContextManager classifyTableContext;

    public AclCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext,
                         @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(jvpp);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Ingress readValue) {
        ((AclBuilder) parentBuilder).setIngress(readValue);
    }

    @Nonnull
    @Override
    public IngressBuilder getBuilder(@Nonnull final InstanceIdentifier<Ingress> id) {
        return new IngressBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Ingress> id,
                                      @Nonnull final IngressBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for interface ACL: {}", id);
        final InterfaceKey interfaceKey = id.firstKeyOf(Interface.class);
        checkArgument(interfaceKey != null, "No parent interface key found");

        final ClassifyTableByInterface request = new ClassifyTableByInterface();
        request.swIfIndex = interfaceContext.getIndex(interfaceKey.getName(), ctx.getMappingContext());

        final ClassifyTableByInterfaceReply reply =
                getReplyForRead(getFutureJVpp().classifyTableByInterface(request).toCompletableFuture(), id);

        builder.setL2Acl(readL2Acl(reply.l2TableId, classifyTableContext, ctx.getMappingContext()));
        builder.setIp4Acl(readIp4Acl(reply.ip4TableId, classifyTableContext, ctx.getMappingContext()));
        builder.setIp6Acl(readIp6Acl(reply.ip6TableId, classifyTableContext, ctx.getMappingContext()));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Attributes for ACL {} successfully read: {}", id, builder.build());
        }
    }

    @Override
    public Initialized<Ingress> init(
            @Nonnull final InstanceIdentifier<Ingress> id, @Nonnull final Ingress readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new IngressBuilder()
                        .setL2Acl(readValue.getL2Acl())
                        .setIp4Acl(readValue.getIp4Acl())
                        .setIp6Acl(readValue.getIp6Acl())
                        .build());
    }

    private InstanceIdentifier<Ingress> getCfgId(
            final InstanceIdentifier<Ingress> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAclAugmentation.class)
                .child(Acl.class)
                .child(Ingress.class);
    }
}
