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

package io.fd.hc2vpp.l3.write.factory;


import static io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory.L2_ID;
import static io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory.SUB_IFC_ID;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.write.ipv4.subinterface.SubInterfaceIpv4AddressCustomizer;
import io.fd.hc2vpp.l3.write.ipv4.subinterface.SubInterfaceIpv4NeighbourCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.rewrite.attributes.Rewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv4WriterFactory implements WriterFactory{

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        final InstanceIdentifier<Rewrite> rewriteId = L2_ID.child(Rewrite.class);

        //   Ipv4(handled after L2 and L2/rewrite is done) =
        final InstanceIdentifier<Address> ipv4SubifcAddressId = SUB_IFC_ID.child(Ipv4.class).child(Address.class);
        registry.addAfter(new GenericListWriter<>(ipv4SubifcAddressId,
                        new SubInterfaceIpv4AddressCustomizer(jvpp, ifcNamingContext)),
                ImmutableSet.of(rewriteId, SUB_IFC_ID.child(Routing.class)));
        final InstanceIdentifier<Neighbor> ipv4NeighborId = SUB_IFC_ID.child(Ipv4.class).child(Neighbor.class);
        registry.addAfter(new GenericListWriter<>(ipv4NeighborId,
                new SubInterfaceIpv4NeighbourCustomizer(jvpp, ifcNamingContext)), rewriteId);

    }
}
