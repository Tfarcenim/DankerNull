package tfar.dankstorage.network;

import net.minecraft.resources.ResourceLocation;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.network.client.*;
import tfar.dankstorage.network.server.*;
import tfar.dankstorage.platform.Services;

import java.util.Locale;

public class DankPacketHandler {

    public static void registerPackets() {

        Services.PLATFORM.registerServerPacket(C2SScrollSlotPacket.TYPE, C2SScrollSlotPacket.STREAM_CODEC);
        Services.PLATFORM.registerServerPacket(C2SLockSlotPacket.TYPE, C2SLockSlotPacket.STREAM_CODEC);
        Services.PLATFORM.registerServerPacket(C2SButtonPacket.TYPE, C2SButtonPacket.STREAM_CODEC);
        Services.PLATFORM.registerServerPacket(C2SSetFrequencyPacket.TYPE, C2SSetFrequencyPacket.STREAM_CODEC);
        Services.PLATFORM.registerServerPacket(C2SRequestContentsPacket.TYPE, C2SRequestContentsPacket.STREAM_CODEC);
        Services.PLATFORM.registerServerPacket(C2SOpenMenuPacket.TYPE,C2SOpenMenuPacket.STREAM_CODEC);

        ///////server to client

        Services.PLATFORM.registerClientPacket(S2CSendExtendedSlotChangePacket.TYPE, S2CSendExtendedSlotChangePacket.STREAM_CODEC);
        Services.PLATFORM.registerClientPacket(S2CSendGhostSlotPacket.TYPE, S2CSendGhostSlotPacket.STREAM_CODEC);
        //Services.PLATFORM.registerClientPacket(S2CSyncSelectedDankItemPacket.TYPE, S2CSyncSelectedDankItemPacket.STREAM_CODEC);
        Services.PLATFORM.registerClientPacket(S2CInitialSyncContainerPacket.TYPE, S2CInitialSyncContainerPacket.STREAM_CODEC);
        Services.PLATFORM.registerClientPacket(S2CContentsForDisplayPacket.TYPE, S2CContentsForDisplayPacket.STREAM_CODEC);
        Services.PLATFORM.registerClientPacket(AdvancedContainerSetDataPayload.TYPE, AdvancedContainerSetDataPayload.STREAM_CODEC);
    }

    public static ResourceLocation packet(Class<?> clazz) {
        return DankStorage.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
