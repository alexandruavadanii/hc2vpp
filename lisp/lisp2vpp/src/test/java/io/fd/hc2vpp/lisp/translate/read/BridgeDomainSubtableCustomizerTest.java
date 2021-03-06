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

package io.fd.hc2vpp.lisp.translate.read;


import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.translate.read.trait.SubtableReaderTestCase;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.VppCallbackException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BridgeDomainSubtableCustomizerTest
        extends SubtableReaderTestCase<BridgeDomainSubtable, BridgeDomainSubtableBuilder> {

    private InstanceIdentifier<BridgeDomainSubtable> validId;
    private NamingContext bridgeDomainContext;

    public BridgeDomainSubtableCustomizerTest() {
        super(BridgeDomainSubtable.class, VniTableBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        bridgeDomainContext = new NamingContext("br", "br-domain-context");
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(expectedVni))
                .child(BridgeDomainSubtable.class);

        defineMapping(mappingContext, "br-domain", expectedTableId, "br-domain-context");
    }

    @Test
    public void testReadCurrentSuccessfull() throws ReadFailedException {
        doReturnValidNonEmptyDataOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyOneEidTableMapDumpCalled(L2);

        final BridgeDomainSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertEquals("br-domain", subtable.getBridgeDomainRef());
    }


    @Test
    public void testReadCurrentEmptyDump() throws ReadFailedException {
        doReturnEmptyDataOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyOneEidTableMapDumpCalled(L2);

        final BridgeDomainSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertNull(subtable.getBridgeDomainRef());
    }

    @Test
    public void testReadCurrentFailed() {
        doThrowOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        try {
            customizer.readCurrentAttributes(validId, builder, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertNull(builder.getBridgeDomainRef());
            verifyOneEidTableMapDumpNotCalled();

            return;
        }

        fail("Test should throw ReadFailedException");
    }

    @Override
    @Test
    public void testGetBuilder() {
        final BridgeDomainSubtableBuilder builder = customizer.getBuilder(validId);

        assertNotNull(builder);
        assertNull(builder.getLocalMappings());
        assertNull(builder.getRemoteMappings());
        assertNull(builder.getBridgeDomainRef());
    }

    @Override
    protected ReaderCustomizer<BridgeDomainSubtable, BridgeDomainSubtableBuilder> initCustomizer() {
        return new BridgeDomainSubtableCustomizer(api, bridgeDomainContext);
    }
}
