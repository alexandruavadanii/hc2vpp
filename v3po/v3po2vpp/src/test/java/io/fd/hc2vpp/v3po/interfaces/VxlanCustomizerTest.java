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

package io.fd.hc2vpp.v3po.interfaces;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.VppInvocationException;
import io.fd.vpp.jvpp.core.dto.VxlanAddDelTunnel;
import io.fd.vpp.jvpp.core.dto.VxlanAddDelTunnelReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.L2Input;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VxlanVni;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanCustomizerTest extends WriterCustomizerTest implements AddressTranslator {

    private static final byte ADD_VXLAN = 1;
    private static final byte DEL_VXLAN = 0;

    @Mock
    private DisabledInterfacesManager disableContext;

    private VxlanCustomizer customizer;
    private String ifaceName;
    private InstanceIdentifier<Vxlan> id;

    private static Vxlan generateVxlan(long vni) {
        final VxlanBuilder builder = new VxlanBuilder();
        builder.setSrc(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.10")));
        builder.setDst(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.11")));
        builder.setEncapVrfId(new VniReference(123L));
        builder.setVni(new VxlanVni(Long.valueOf(vni)));
        builder.setDecapNext(L2Input.class);
        return builder.build();
    }

    private static Vxlan generateVxlan() {
        return generateVxlan(Long.valueOf(11));
    }

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VxlanTunnel.class);

        customizer =
                new VxlanCustomizer(api, new NamingContext("generateInterfaceNAme", "test-instance"), disableContext);

        ifaceName = "eth0";
        id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(ifaceName))
                .augmentation(VppInterfaceAugmentation.class).child(Vxlan.class);
    }

    private void whenVxlanAddDelTunnelThenSuccess() {
        when(api.vxlanAddDelTunnel(any(VxlanAddDelTunnel.class))).thenReturn(future(new VxlanAddDelTunnelReply()));
    }

    private void whenVxlanAddDelTunnelThenFailure() {
        doReturn(failedFuture()).when(api).vxlanAddDelTunnel(any(VxlanAddDelTunnel.class));
    }

    private VxlanAddDelTunnel verifyVxlanAddDelTunnelWasInvoked(final Vxlan vxlan) throws VppInvocationException {
        ArgumentCaptor<VxlanAddDelTunnel> argumentCaptor = ArgumentCaptor.forClass(VxlanAddDelTunnel.class);
        verify(api).vxlanAddDelTunnel(argumentCaptor.capture());
        final VxlanAddDelTunnel actual = argumentCaptor.getValue();
        assertEquals(0, actual.isIpv6);
        assertEquals(1, actual.decapNextIndex);

        assertArrayEquals(ipAddressToArray(vxlan.getSrc()), actual.srcAddress);
        assertArrayEquals(ipAddressToArray(vxlan.getDst()), actual.dstAddress);
        assertEquals(vxlan.getEncapVrfId().getValue().intValue(), actual.encapVrfId);
        assertEquals(vxlan.getVni().getValue().intValue(), actual.vni);
        return actual;
    }

    private void verifyVxlanAddWasInvoked(final Vxlan vxlan) throws VppInvocationException {
        final VxlanAddDelTunnel actual = verifyVxlanAddDelTunnelWasInvoked(vxlan);
        assertEquals(ADD_VXLAN, actual.isAdd);
    }

    private void verifyVxlanDeleteWasInvoked(final Vxlan vxlan) throws VppInvocationException {
        final VxlanAddDelTunnel actual = verifyVxlanAddDelTunnelWasInvoked(vxlan);
        assertEquals(DEL_VXLAN, actual.isAdd);
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenSuccess();
        noMappingDefined(mappingContext, ifaceName, "test-instance");

        customizer.writeCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanAddWasInvoked(vxlan);
        verify(mappingContext).put(eq(mappingIid(ifaceName, "test-instance")), eq(mapping(ifaceName, 0).get()));
    }

    @Test
    public void testWriteCurrentAttributesWithExistingVxlanPlaceholder() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenSuccess();
        noMappingDefined(mappingContext, ifaceName, "test-instance");
        doReturn(true).when(disableContext).isInterfaceDisabled(0, mappingContext);

        customizer.writeCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanAddWasInvoked(vxlan);
        verify(mappingContext).put(eq(mappingIid(ifaceName, "test-instance")), eq(mapping(ifaceName, 0).get()));
        verify(disableContext).removeDisabledInterface(0, mappingContext);
    }

    @Test
    public void testWriteCurrentAttributesMappingAlreadyPresent() throws Exception {
        final Vxlan vxlan = generateVxlan();
        final int ifaceId = 0;

        whenVxlanAddDelTunnelThenSuccess();
        defineMapping(mappingContext, ifaceName, ifaceId, "test-instance");

        customizer.writeCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanAddWasInvoked(vxlan);

        // Remove the first mapping before putting in the new one
        verify(mappingContext).delete(eq(mappingIid(ifaceName, "test-instance")));
        verify(mappingContext)
        .put(eq(mappingIid(ifaceName, "test-instance")), eq(mapping(ifaceName, ifaceId).get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, vxlan, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyVxlanAddWasInvoked(vxlan);
            // Mapping not stored due to failure
            verify(mappingContext, times(0))
            .put(eq(mappingIid(ifaceName, "test-instance")), eq(mapping(ifaceName, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(id, generateVxlan(10), generateVxlan(11), writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenSuccess();
        defineMapping(mappingContext, ifaceName, 1, "test-instance");

        customizer.deleteCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanDeleteWasInvoked(vxlan);
        verify(mappingContext).delete(eq(mappingIid(ifaceName, "test-instance")));
        verify(disableContext).disableInterface(1, mappingContext);
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenFailure();
        defineMapping(mappingContext, ifaceName, 1, "test-instance");

        try {
            customizer.deleteCurrentAttributes(id, vxlan, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyVxlanDeleteWasInvoked(vxlan);
            verify(mappingContext, times(0)).delete(eq(mappingIid(ifaceName, "test-instance")));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}
