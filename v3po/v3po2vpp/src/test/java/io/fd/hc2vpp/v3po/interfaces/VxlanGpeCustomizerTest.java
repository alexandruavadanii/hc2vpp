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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import io.fd.vpp.jvpp.core.dto.VxlanGpeAddDelTunnel;
import io.fd.vpp.jvpp.core.dto.VxlanGpeAddDelTunnelReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanGpeCustomizerTest extends WriterCustomizerTest implements AddressTranslator {

    private static final byte ADD_VXLAN_GPE = 1;
    private static final byte DEL_VXLAN_GPE = 0;

    @Mock
    private DisabledInterfacesManager interfaceDisableContext;

    private VxlanGpeCustomizer customizer;
    private String ifaceName;
    private InstanceIdentifier<VxlanGpe> id;

    private static VxlanGpe generateVxlanGpe(long vni) {
        final VxlanGpeBuilder builder = new VxlanGpeBuilder();
        builder.setLocal(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.10")));
        builder.setRemote(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.11")));
        builder.setVni(new VxlanGpeVni(Long.valueOf(vni)));
        builder.setNextProtocol(VxlanGpeNextProtocol.forValue(1));
        builder.setEncapVrfId(Long.valueOf(123));
        builder.setDecapVrfId(Long.valueOf(456));
        return builder.build();
    }

    private static VxlanGpe generateVxlanGpe() {
        return generateVxlanGpe(Long.valueOf(11));
    }

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VxlanGpeTunnel.class);
        NamingContext namingContext = new NamingContext("generateInterfaceName", "test-instance");
        customizer = new VxlanGpeCustomizer(api, namingContext, interfaceDisableContext);

        ifaceName = "elth0";
        id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(ifaceName))
            .augmentation(VppInterfaceAugmentation.class).child(VxlanGpe.class);
    }

    private void whenVxlanGpeAddDelTunnelThenSuccess() {
        when(api.vxlanGpeAddDelTunnel(any(VxlanGpeAddDelTunnel.class)))
            .thenReturn(future(new VxlanGpeAddDelTunnelReply()));
    }

    private void whenVxlanGpeAddDelTunnelThenFailure() {
        doReturn(failedFuture()).when(api).vxlanGpeAddDelTunnel(any(VxlanGpeAddDelTunnel.class));
    }

    private VxlanGpeAddDelTunnel verifyVxlanGpeAddDelTunnelWasInvoked(final VxlanGpe vxlanGpe)
        throws VppBaseCallException {
        ArgumentCaptor<VxlanGpeAddDelTunnel> argumentCaptor = ArgumentCaptor.forClass(VxlanGpeAddDelTunnel.class);
        verify(api).vxlanGpeAddDelTunnel(argumentCaptor.capture());
        final VxlanGpeAddDelTunnel actual = argumentCaptor.getValue();
        assertEquals(0, actual.isIpv6);
        assertArrayEquals(ipAddressToArray(vxlanGpe.getLocal()), actual.local);
        assertArrayEquals(ipAddressToArray(vxlanGpe.getRemote()), actual.remote);
        assertEquals(vxlanGpe.getVni().getValue().intValue(), actual.vni);
        assertEquals(vxlanGpe.getNextProtocol().getIntValue(), actual.protocol);
        assertEquals(vxlanGpe.getEncapVrfId().intValue(), actual.encapVrfId);
        assertEquals(vxlanGpe.getDecapVrfId().intValue(), actual.decapVrfId);
        return actual;
    }

    private void verifyVxlanGpeAddWasInvoked(final VxlanGpe vxlanGpe) throws VppBaseCallException {
        final VxlanGpeAddDelTunnel actual = verifyVxlanGpeAddDelTunnelWasInvoked(vxlanGpe);
        assertEquals(ADD_VXLAN_GPE, actual.isAdd);
    }

    private void verifyVxlanGpeDeleteWasInvoked(final VxlanGpe vxlanGpe) throws VppBaseCallException {
        final VxlanGpeAddDelTunnel actual = verifyVxlanGpeAddDelTunnelWasInvoked(vxlanGpe);
        assertEquals(DEL_VXLAN_GPE, actual.isAdd);
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenSuccess();
        noMappingDefined(mappingContext, ifaceName, "test-instance");

        customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeAddWasInvoked(vxlanGpe);
        verify(mappingContext).put(eq(mappingIid(ifaceName, "test-instance")), eq(
            mapping(ifaceName, 0).get()));
    }

    @Test
    public void testWriteCurrentAttributesMappingAlreadyPresent() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();
        final int ifaceId = 0;

        whenVxlanGpeAddDelTunnelThenSuccess();
        defineMapping(mappingContext, ifaceName, ifaceId, "test-instance");

        customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeAddWasInvoked(vxlanGpe);

        // Remove the first mapping before putting in the new one
        verify(mappingContext).delete(eq(mappingIid(ifaceName, "test-instance")));
        verify(mappingContext).put(eq(mappingIid(ifaceName, "test-instance")),
            eq(mapping(ifaceName, ifaceId).get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyVxlanGpeAddWasInvoked(vxlanGpe);
            // Mapping not stored due to failure
            verify(mappingContext, times(0))
                .put(eq(mappingIid(ifaceName, "test-instance")), eq(
                    mapping(ifaceName, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(id, generateVxlanGpe(10), generateVxlanGpe(11), writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenSuccess();
        defineMapping(mappingContext, ifaceName, 1, "test-instance");

        customizer.deleteCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeDeleteWasInvoked(vxlanGpe);
        verify(mappingContext).delete(eq(mappingIid(ifaceName, "test-instance")));
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenFailure();
        defineMapping(mappingContext, ifaceName, 1, "test-instance");

        try {
            customizer.deleteCurrentAttributes(id, vxlanGpe, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyVxlanGpeDeleteWasInvoked(vxlanGpe);
            verify(mappingContext, times(0)).delete(eq(mappingIid(ifaceName, "test-instance")));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}
