package tfar.dankstorage.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import tfar.dankstorage.network.client.AdvancedContainerSetDataPayload;
import tfar.dankstorage.network.client.S2CInitialSyncContainerPacket;
import tfar.dankstorage.network.client.S2CSendExtendedSlotChangePacket;
import tfar.dankstorage.platform.Services;

public class CustomSync implements ContainerSynchronizer {

    private final ServerPlayer player;

    public CustomSync(ServerPlayer player) {
        this.player = player;
    }

    public void sendInitialData(AbstractContainerMenu abstractContainerMenu, NonNullList<ItemStack> stacks, ItemStack carried, int[] is) {
        //problem, vanilla containers send itemstack size in bytes
        Services.PLATFORM.sendToClient(new S2CInitialSyncContainerPacket(abstractContainerMenu.containerId, abstractContainerMenu.incrementStateId(), stacks,carried), player);
        for(int i = 0; i < is.length; ++i) {
            this.broadcastDataValue(abstractContainerMenu, i, is[i]);
        }
    }

    @Override
    public void sendSlotChange(AbstractContainerMenu abstractContainerMenu, int slot, ItemStack stack) {
        //problem, vanilla containers send itemstack size in bytes
        Services.PLATFORM.sendToClient(new S2CSendExtendedSlotChangePacket(abstractContainerMenu.containerId, abstractContainerMenu.incrementStateId(),slot,stack), player);
    }

    @Override
    public void sendCarriedChange(AbstractContainerMenu abstractContainerMenu, ItemStack stack) {
        this.broadcastCarriedItem(stack);
    }

    @Override
    public void sendDataChange(AbstractContainerMenu abstractContainerMenu, int i, int j) {
        this.broadcastDataValue(abstractContainerMenu, i, j);
    }

    private void broadcastDataValue(AbstractContainerMenu abstractContainerMenu, int slot, int value) {
        Services.PLATFORM.sendToClient(new AdvancedContainerSetDataPayload((byte) abstractContainerMenu.containerId, (short) slot, value),player);
    }

    private void broadcastCarriedItem(ItemStack itemStack) {
        player.connection.send(new ClientboundContainerSetSlotPacket(-1, player.containerMenu.incrementStateId(), -1, itemStack));
    }
}
