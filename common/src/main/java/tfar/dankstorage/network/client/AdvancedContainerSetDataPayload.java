/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package tfar.dankstorage.network.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.client.CommonClient;
//shamelessly stolen from neoforge
/**
 * A custom payload that updates the full dataslot value instead of just the short value
 *
 * @param containerId The containerId for the container.
 * @param dataId      The ID of the dataslot.
 * @param value       The value of the dataslot.
 */
public record AdvancedContainerSetDataPayload(byte containerId, short dataId, int value) implements S2CModPacket {

    public static final Type<AdvancedContainerSetDataPayload> TYPE = new Type<>(DankStorage.id("advanced_container_set_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedContainerSetDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            AdvancedContainerSetDataPayload::containerId,
            ByteBufCodecs.SHORT,
            AdvancedContainerSetDataPayload::dataId,
            ByteBufCodecs.VAR_INT,
            AdvancedContainerSetDataPayload::value,
            AdvancedContainerSetDataPayload::new);
    @Override
    public Type<AdvancedContainerSetDataPayload> type() {
        return TYPE;
    }

    @Override
    public void handleClient() {
        Player player = CommonClient.getLocalPlayer();
        if (player.containerMenu != null && player.containerMenu.containerId == containerId) {
            player.containerMenu.setData(dataId, value);
        }
    }
}
