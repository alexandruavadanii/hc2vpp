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

package io.fd.honeycomb.v3po.translate.v3po.vppclassifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.booleanToByte;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.dto.ClassifyAddDelTableReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for classify table create/delete. <br> Sends {@code classify_add_del_table} message to
 * VPP.<br> Equivalent to invoking {@code vppctl classify table} command.
 */
public class ClassifyTableWriter extends FutureJVppCustomizer
    implements ListWriterCustomizer<ClassifyTable, ClassifyTableKey> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifyTableWriter.class);
    private final NamingContext classifyTableContext;

    public ClassifyTableWriter(@Nonnull final FutureJVpp futureJvpp,
                               @Nonnull final NamingContext classifyTableContext) {
        super(futureJvpp);
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<List<ClassifyTable>> extract(@Nonnull final InstanceIdentifier<ClassifyTable> currentId,
                                                 @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppClassifier) parentData).getClassifyTable());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifyTable> id,
                                       @Nonnull final ClassifyTable dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Creating classify table: iid={} dataAfter={}", id, dataAfter);
        try {
            final int newTableIndex =
                classifyAddDelTable(true, id, dataAfter, ~0 /* value not present */, writeContext.getMappingContext());

            // Add classify table name <-> vpp index mapping to the naming context:
            classifyTableContext.addName(newTableIndex, dataAfter.getName(), writeContext.getMappingContext());
            LOG.debug("Successfully created classify table(id={]): iid={} dataAfter={}", newTableIndex, id, dataAfter);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifyTable> id,
                                        @Nonnull final ClassifyTable dataBefore, @Nonnull final ClassifyTable dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.warn("ClassifyTable update is not supported, ignoring configuration {}", dataAfter);
        // TODO if only leaves were updated (but not child/aug nodes), we should throw exception to deny config change
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifyTable> id,
                                        @Nonnull final ClassifyTable dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing classify table: iid={} dataBefore={}", id, dataBefore);
        final String tableName = dataBefore.getName();
        checkState(classifyTableContext.containsIndex(tableName, writeContext.getMappingContext()),
            "Removing classify table {}, but index could not be found in the classify table context", tableName);

        final int tableIndex = classifyTableContext.getIndex(tableName, writeContext.getMappingContext());
        try {
            classifyAddDelTable(false, id, dataBefore, tableIndex, writeContext.getMappingContext());

            // Remove deleted interface from interface context:
            classifyTableContext.removeName(dataBefore.getName(), writeContext.getMappingContext());
            LOG.debug("Successfully removed classify table(id={]): iid={} dataAfter={}", tableIndex, id, dataBefore);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private int classifyAddDelTable(final boolean isAdd, @Nonnull final InstanceIdentifier<ClassifyTable> id,
                                    @Nonnull final ClassifyTable table, final int tableId, final MappingContext ctx)
        throws VppBaseCallException, WriteTimeoutException {
        final CompletionStage<ClassifyAddDelTableReply> createClassifyTableReplyCompletionStage =
            getFutureJVpp().classifyAddDelTable(getClassifyAddDelTableRequest(isAdd, tableId, table, ctx));

        final ClassifyAddDelTableReply reply =
            TranslateUtils.getReplyForWrite(createClassifyTableReplyCompletionStage.toCompletableFuture(), id);
        return reply.newTableIndex;

    }

    private ClassifyAddDelTable getClassifyAddDelTableRequest(final boolean isAdd, final int tableIndex,
                                                              @Nonnull final ClassifyTable table,
                                                              @Nonnull final MappingContext ctx) {
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.isAdd = booleanToByte(isAdd);
        request.tableIndex = tableIndex;

        // mandatory, all u32 values are permitted:
        request.nbuckets = table.getNbuckets().intValue();
        request.memorySize = table.getMemorySize().intValue();
        request.skipNVectors = table.getSkipNVectors().intValue();

        // mandatory
        // TODO implement node name to index conversion after https://jira.fd.io/browse/VPP-203 is fixed
        request.missNextIndex = table.getMissNextIndex().getPacketHandlingAction().getIntValue();

        final String nextTable = table.getNextTable();
        if (nextTable != null) {
            request.nextTableIndex = classifyTableContext.getIndex(nextTable, ctx);
        } else {
            request.nextTableIndex = ~0; // value not specified
        }
        request.mask = DatatypeConverter.parseHexBinary(table.getMask().getValue().replace(":", ""));
        checkArgument(request.mask.length % 16 == 0, "Number of mask bytes must be multiple of 16.");
        request.matchNVectors = request.mask.length / 16;

        return request;
    }
}