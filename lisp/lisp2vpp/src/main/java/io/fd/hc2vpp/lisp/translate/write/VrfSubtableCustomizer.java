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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.lisp.translate.write.trait.SubtableWriter;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VrfSubtableCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<VrfSubtable>, SubtableWriter {

    private static final Logger LOG = LoggerFactory.getLogger(VrfSubtableCustomizer.class);

    public VrfSubtableCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                       @Nonnull final VrfSubtable dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // TODO - HC2VPP-73 - remove after resolving ODL Boron issues
        checkNotNull(dataAfter.getTableId(), "Table id must be present");
        LOG.debug("Writing Id[{}]/Data[{}]", id, dataAfter);

        try {
            addDelSubtableMapping(getFutureJVpp(), true, extractVni(id), dataAfter.getTableId().intValue(), false, LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        LOG.debug("{} successfully written", id);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VrfSubtable> id,
                                        @Nonnull final VrfSubtable dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // TODO - HC2VPP-73 - remove after resolving ODL Boron issues
        checkNotNull(dataBefore.getTableId(), "Table id must be present");
        LOG.debug("Removing Id[{}]/Data[{}]", id, dataBefore);

        try {
            addDelSubtableMapping(getFutureJVpp(), false, extractVni(id), dataBefore.getTableId().intValue(), false,
                    LOG);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }

        LOG.debug("{} successfully removed", id);
    }

}
