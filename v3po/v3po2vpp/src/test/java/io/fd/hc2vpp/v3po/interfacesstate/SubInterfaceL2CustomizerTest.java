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

package io.fd.hc2vpp.v3po.interfacesstate;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.l2.state.attributes.L2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.l2.state.attributes.L2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceL2CustomizerTest extends ReaderCustomizerTest<L2, L2Builder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String BD_CTX_NAME = "bd-test-instance";
    private NamingContext interfaceContext;
    private NamingContext bridgeDomainContext;

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUB_IF_NAME = "local0.1";
    private static final long SUB_IF_ID = 1;
    private static final int SUB_IF_INDEX = 11;
    private InstanceIdentifier<L2> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceStateAugmentation.class)
                    .child(SubInterfaces.class).child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID))
                    .child(L2.class);

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public SubInterfaceL2CustomizerTest() {
        super(L2.class, SubInterfaceBuilder.class);
    }

    @Override
    protected void setUp() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        bridgeDomainContext = new NamingContext("generatedBDName", BD_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_NAME, SUB_IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<L2, L2Builder> initCustomizer() {
        return new SubInterfaceL2Customizer(api, interfaceContext, bridgeDomainContext, dumpCacheManager);
    }
}
