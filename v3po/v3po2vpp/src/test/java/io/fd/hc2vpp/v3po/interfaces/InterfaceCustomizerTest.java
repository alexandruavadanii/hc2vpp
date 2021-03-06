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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetFlags;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetFlagsReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {
    private static final String IFACE_CTX_NAME = "interface-ctx";
    private static final String IF_NAME = "eth1";
    private static final int IF_INDEX = 1;

    private static final InstanceIdentifier<Interface> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME));

    private static final String LOCAL0_IFC_NAME = "local0";
    private static final InstanceIdentifier<Interface> LOCAL0_IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(LOCAL0_IFC_NAME));
    private InterfaceCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new InterfaceCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        final boolean enabled = true;
        when(api.swInterfaceSetFlags(any())).thenReturn(future(new SwInterfaceSetFlagsReply()));
        customizer.writeCurrentAttributes(IID, iface(enabled), writeContext);
        verify(api).swInterfaceSetFlags(expectedRequest(enabled));
    }

    @Test
    public void testWriteFailed() {
        final boolean enabled = false;
        when(api.swInterfaceSetFlags(any())).thenReturn(failedFuture());
        try {
            customizer.writeCurrentAttributes(IID, iface(enabled), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceSetFlags(expectedRequest(enabled));
            return;
        }
        fail("WriteFailedException expected");
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        when(api.swInterfaceSetFlags(any())).thenReturn(future(new SwInterfaceSetFlagsReply()));
        customizer.updateCurrentAttributes(IID, iface(false), iface(true), writeContext);
        verify(api).swInterfaceSetFlags(expectedRequest(true));
    }

    @Test
    public void testUpdateFailed() {
        when(api.swInterfaceSetFlags(any())).thenReturn(failedFuture());
        try {
            customizer.updateCurrentAttributes(IID, iface(false), iface(true), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceSetFlags(expectedRequest(true));
            return;
        }
        fail("WriteFailedException expected");
    }

    @Test
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, mock(Interface.class), writeContext);
        verifyZeroInteractions(api);
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteLocal0() throws WriteFailedException {
        final Interface ifc = mock(Interface.class);
        when(ifc.getName()).thenReturn(LOCAL0_IFC_NAME);
        customizer.deleteCurrentAttributes(LOCAL0_IID, ifc, writeContext);
    }

    private Interface iface(final boolean enabled) {
        return new InterfaceBuilder().setName(IF_NAME).setEnabled(enabled).build();
    }

    private SwInterfaceSetFlags expectedRequest(final boolean enabled) {
        final SwInterfaceSetFlags request = new SwInterfaceSetFlags();
        request.adminUpDown = booleanToByte(enabled);
        request.swIfIndex = IF_INDEX;
        return request;
    }
}