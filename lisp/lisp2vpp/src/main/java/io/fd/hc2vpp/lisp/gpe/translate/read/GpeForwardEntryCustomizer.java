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

package io.fd.hc2vpp.lisp.gpe.translate.read;

import static io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeEntryIdentifier.fromDumpDetail;
import static io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPair.fromDumpDetail;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;
import static java.lang.String.format;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeEntryIdentifier;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeEntryMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPair;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntriesGet;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntriesGetReply;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDetails;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDump;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryVnisGet;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryVnisGetReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.GpeFwdEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificator;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.Gpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.locator.pair.LocatorPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapReplyAction;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeForwardEntryCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<GpeEntry, GpeEntryKey, GpeEntryBuilder>, JvppReplyConsumer,
        EidTranslator {

    private final DumpCacheManager<GpeFwdEntriesGetReply, Integer> entryDumpManager;
    private final DumpCacheManager<GpeFwdEntryPathDetailsReplyDump, Integer> entryDumpCacheManager;
    private final DumpCacheManager<GpeFwdEntryVnisGetReply, Void> activeVnisDumpManager;
    private final GpeEntryMappingContext gpeEntryMappingContext;
    private final GpeLocatorPairMappingContext gpeLocatorsMappingContext;
    private final GpeStateCheckService gpeStateCheckService;

    public GpeForwardEntryCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                     @Nonnull final GpeStateCheckService gpeStateCheckService,
                                     @Nonnull final GpeEntryMappingContext gpeEntryMappingContext,
                                     @Nonnull final GpeLocatorPairMappingContext gpeLocatorsMappingContext) {
        super(futureJVppCore);
        this.gpeStateCheckService = gpeStateCheckService;
        this.gpeEntryMappingContext = gpeEntryMappingContext;
        this.gpeLocatorsMappingContext = gpeLocatorsMappingContext;
        this.entryDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<GpeFwdEntriesGetReply, Integer>()
                .acceptOnly(GpeFwdEntriesGetReply.class)
                .withExecutor((identifier, vni) -> {
                    GpeFwdEntriesGet request = new GpeFwdEntriesGet();
                    request.vni = vni;
                    return getReplyForRead(getFutureJVpp().gpeFwdEntriesGet(request).toCompletableFuture(), identifier);
                }).build();
        entryDumpCacheManager =
                new DumpCacheManager.DumpCacheManagerBuilder<GpeFwdEntryPathDetailsReplyDump, Integer>()
                        .acceptOnly(GpeFwdEntryPathDetailsReplyDump.class)
                        .withExecutor((identifier, fwdEntryIndex) -> {
                            GpeFwdEntryPathDump request = new GpeFwdEntryPathDump();
                            request.fwdEntryIndex = fwdEntryIndex;
                            return getReplyForRead(getFutureJVpp().gpeFwdEntryPathDump(request).toCompletableFuture(),
                                    identifier);
                        }).build();
        activeVnisDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<GpeFwdEntryVnisGetReply, Void>()
                .acceptOnly(GpeFwdEntryVnisGetReply.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        getFutureJVpp().gpeFwdEntryVnisGet(new GpeFwdEntryVnisGet()).toCompletableFuture(),
                        identifier))
                .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                                  @Nonnull final GpeEntry readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(InstanceIdentifier.create(Gpe.class)
                .child(GpeFeatureData.class)
                .child(GpeEntryTable.class)
                .child(GpeEntry.class, id.firstKeyOf(GpeEntry.class)), readValue);
    }

    @Nonnull
    @Override
    public List<GpeEntryKey> getAllIds(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                       @Nonnull final ReadContext context)
            throws ReadFailedException {

        if (!gpeStateCheckService.isGpeEnabled(context)) {
            return Collections.emptyList();
        }

        return activeVnis(id, context.getModificationCache())
                .flatMap(vni -> getKeysForVni(id, vni, context).stream())
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<GpeEntry> readData) {
        ((GpeEntryTableBuilder) builder).setGpeEntry(readData);
    }

    @Nonnull
    @Override
    public GpeEntryBuilder getBuilder(@Nonnull final InstanceIdentifier<GpeEntry> id) {
        return new GpeEntryBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<GpeEntry> id,
                                      @Nonnull final GpeEntryBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        if (!gpeStateCheckService.isGpeEnabled(ctx)) {
            return;
        }

        final String entryId = id.firstKeyOf(GpeEntry.class).getId();

        final GpeEntryIdentificator identificator =
                gpeEntryMappingContext.getIdentificatorById(entryId, ctx.getMappingContext());

        // reads configured vni's, then reads entries for them and filter out current one
        final java.util.Optional<GpeFwdEntry> entryCandicate = activeVnis(id, ctx.getModificationCache())
                .flatMap(vni -> getEntriesForVni(id, vni, ctx).stream())
                .filter(entry -> fromDumpDetail(entry).isSame(identificator))
                .findAny();

        if (entryCandicate.isPresent()) {
            final GpeFwdEntry gpeFwdEntry = entryCandicate.get();

            final int entryVni = gpeFwdEntry.vni;
            builder.setId(entryId)
                    .setDpTable((long) gpeFwdEntry.dpTable)
                    .setLocalEid(getArrayAsGpeLocalEid(MappingsDumpParams.EidType.valueOf(gpeFwdEntry.eidType),
                            gpeFwdEntry.leid, gpeFwdEntry.leidPrefixLen, entryVni))
                    .setRemoteEid(getArrayAsGpeRemoteEid(MappingsDumpParams.EidType.valueOf(gpeFwdEntry.eidType),
                            gpeFwdEntry.reid, gpeFwdEntry.reidPrefixLen, entryVni))
                    .setVni((long) entryVni);

            final Optional<GpeFwdEntryPathDetailsReplyDump> locatorsDump =
                    entryDumpCacheManager.getDump(id, ctx.getModificationCache(), gpeFwdEntry.fwdEntryIndex);

            // if any locators exist,it is a positive mapping
            if (locatorsDump.isPresent() && locatorsDump.get().gpeFwdEntryPathDetails != null &&
                    !locatorsDump.get().gpeFwdEntryPathDetails.isEmpty()) {
                final List<LocatorPairs> pairs =
                        java.util.Optional.ofNullable(locatorsDump.get().gpeFwdEntryPathDetails)
                                .orElse(Collections.emptyList())
                                .stream()
                                .map(entry -> {
                                    final GpeLocatorPair gpePair = fromDumpDetail(entry);
                                    final LocatorPairMapping mapping = gpeLocatorsMappingContext
                                            .getMapping(entryId, gpePair, ctx.getMappingContext());
                                    return buildLocatorPair(entry, gpePair, mapping);
                                }).collect(Collectors.toList());
                builder.setLocatorPairs(pairs);
            } else {
                // negative otherwise
                builder.setAction(MapReplyAction.forValue(gpeFwdEntry.action));
            }
        }
    }

    private List<GpeFwdEntry> getEntriesForVni(final InstanceIdentifier<GpeEntry> id, final int vni,
                                               final ReadContext context) {
        final Optional<GpeFwdEntriesGetReply> dump = getEntiesDump(id, vni, context);
        if (dump.isPresent()) {
            return Arrays.stream(java.util.Optional.ofNullable(dump.get().entries).orElse(new GpeFwdEntry[]{}))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<GpeEntryKey> getKeysForVni(final InstanceIdentifier<GpeEntry> id, final int vni,
                                            final ReadContext context) {

        final Optional<GpeFwdEntriesGetReply> dump = getEntiesDump(id, vni, context);
        if (dump.isPresent()) {
            return Arrays.stream(java.util.Optional.ofNullable(dump.get().entries).orElse(new GpeFwdEntry[]{}))
                    .map(GpeEntryIdentifier::fromDumpDetail)
                    .map(identifier -> gpeEntryMappingContext
                            .getIdByEntryIdentifier(identifier, context.getMappingContext()))
                    .map(GpeEntryKey::new)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Optional<GpeFwdEntriesGetReply> getEntiesDump(final InstanceIdentifier<GpeEntry> id, final int vni,
                                                          final ReadContext context) {
        final Optional<GpeFwdEntriesGetReply> dump;
        try {
            dump = entryDumpManager.getDump(id, context.getModificationCache(), vni);
        } catch (ReadFailedException e) {
            throw new IllegalStateException(format("Unable to read Gpe entries for vni %s", vni), e);
        }
        return dump;
    }

    private LocatorPairs buildLocatorPair(final GpeFwdEntryPathDetails entry, final GpeLocatorPair gpePair,
                                          final LocatorPairMapping mapping) {
        return new LocatorPairsBuilder()
                .setId(mapping.getId())
                .setLocatorPair(new LocatorPairBuilder()
                        .setLocalLocator(gpePair.getLocalAddress())
                        .setRemoteLocator(gpePair.getRemoteAddress())
                        .setWeight((short) entry.lclLoc.weight)
                        .build())
                .build();
    }

    private Stream<Integer> activeVnis(final InstanceIdentifier<GpeEntry> id,
                                       final ModificationCache cache) throws ReadFailedException {
        final int[] vnis = activeVnisDumpManager.getDump(id, cache, NO_PARAMS).or(() -> {
            final GpeFwdEntryVnisGetReply reply = new GpeFwdEntryVnisGetReply();
            reply.vnis = new int[0];
            return reply;
        }).vnis;
        return Arrays.stream(vnis).boxed();
    }
}