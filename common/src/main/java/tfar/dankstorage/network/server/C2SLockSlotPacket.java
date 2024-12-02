package tfar.dankstorage.network.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import tfar.dankstorage.inventory.DankInventory;
import tfar.dankstorage.menu.DankMenu;
import tfar.dankstorage.network.DankPacketHandler;
import tfar.dankstorage.platform.Services;

public record C2SLockSlotPacket(int slot) implements C2SModPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SLockSlotPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, C2SLockSlotPacket::slot, C2SLockSlotPacket::new);


    public static final CustomPacketPayload.Type<C2SLockSlotPacket> TYPE = new CustomPacketPayload.Type<>(
            DankPacketHandler.packet(C2SLockSlotPacket.class));



    public static void send(int slot) {
        Services.PLATFORM.sendToServer(new C2SLockSlotPacket(slot));
    }

    public void handleServer(ServerPlayer player) {
        AbstractContainerMenu container = player.containerMenu;
        if (container instanceof DankMenu dankContainer) {
            DankInventory inventory = dankContainer.dankInventory;
            inventory.toggleGhostItem(slot);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

