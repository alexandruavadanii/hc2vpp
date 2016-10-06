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

package io.fd.honeycomb.translate.v3po.vppclassifier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.OpaqueIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifySessionWriterTest extends WriterCustomizerTest {

    private static final int TABLE_INDEX = 123;
    private static final String TABLE_NAME = "table123";

    @Mock
    private VppClassifierContextManager classfierContext;

    private ClassifySessionWriter customizer;
    private static final int SESSION_INDEX = 456;

    @Override
    public void setUp() throws Exception {
        customizer = new ClassifySessionWriter(api, classfierContext);

        when(classfierContext.containsTable(TABLE_NAME, mappingContext)).thenReturn(true);
        when(classfierContext.getTableIndex(TABLE_NAME, mappingContext)).thenReturn(TABLE_INDEX);

        final ClassifyTable table = mock(ClassifyTable.class);
        when(table.getClassifierNode()).thenReturn(new VppNodeName("ip4-classifier"));
        when(writeContext.readAfter(any())).thenReturn(Optional.of(table));
        when(writeContext.readBefore(any())).thenReturn(Optional.of(table));
    }

    private static ClassifySession generateClassifySession(final long opaqueIndex, final String match) {
        final ClassifySessionBuilder builder = new ClassifySessionBuilder();
        builder.setOpaqueIndex(new OpaqueIndex(opaqueIndex));
        builder.setHitNext(new VppNode(PacketHandlingAction.Deny));
        builder.setAdvance(123);
        builder.setMatch(new HexString(match));
        return builder.build();
    }

    private static InstanceIdentifier<ClassifySession> getClassifySessionId(final String tableName,
                                                                            final String match) {
        return InstanceIdentifier.create(VppClassifier.class)
                .child(ClassifyTable.class, new ClassifyTableKey(tableName))
                .child(ClassifySession.class, new ClassifySessionKey(new HexString(match)));
    }

    private void whenClassifyAddDelSessionThenSuccess() {
        doReturn(future(new ClassifyAddDelSessionReply())).when(api)
                .classifyAddDelSession(any(ClassifyAddDelSession.class));
    }

    private void whenClassifyAddDelSessionThenFailure() {
        doReturn(failedFuture()).when(api).classifyAddDelSession(any(ClassifyAddDelSession.class));
    }

    private static ClassifyAddDelSession generateClassifyAddDelSession(final byte isAdd, final int tableIndex,
                                                                       final int sessionIndex) {
        final ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = isAdd;
        request.tableIndex = tableIndex;
        request.opaqueIndex = sessionIndex;
        request.hitNextIndex = 0;
        request.advance = 123;
        request.match =
                new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                        (byte) 0x05, (byte) 0x06, 0x00, 0x00, 0x00, 0x00};
        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final String match = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
        final ClassifySession classifySession = generateClassifySession(SESSION_INDEX, match);
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, match);

        whenClassifyAddDelSessionThenSuccess();

        customizer.writeCurrentAttributes(id, classifySession, writeContext);

        verify(api).classifyAddDelSession(generateClassifyAddDelSession((byte) 1, TABLE_INDEX, SESSION_INDEX));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final String match = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
        final ClassifySession classifySession = generateClassifySession(SESSION_INDEX, match);
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, match);

        whenClassifyAddDelSessionThenFailure();

        try {
            customizer.writeCurrentAttributes(id, classifySession, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).classifyAddDelSession(generateClassifyAddDelSession((byte) 1, TABLE_INDEX, SESSION_INDEX));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(null, null, null, writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        final String match = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
        final ClassifySession classifySession = generateClassifySession(SESSION_INDEX, match);
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, match);

        whenClassifyAddDelSessionThenSuccess();

        customizer.deleteCurrentAttributes(id, classifySession, writeContext);

        verify(api).classifyAddDelSession(generateClassifyAddDelSession((byte) 0, TABLE_INDEX, SESSION_INDEX));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final String match = "00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00";
        final ClassifySession classifySession = generateClassifySession(SESSION_INDEX, match);
        final InstanceIdentifier<ClassifySession> id = getClassifySessionId(TABLE_NAME, match);

        whenClassifyAddDelSessionThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, classifySession, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).classifyAddDelSession(generateClassifyAddDelSession((byte) 0, TABLE_INDEX, SESSION_INDEX));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, classifySession, writeContext);
    }
}