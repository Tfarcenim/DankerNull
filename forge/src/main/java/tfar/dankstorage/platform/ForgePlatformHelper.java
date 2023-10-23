package tfar.dankstorage.platform;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tfar.dankstorage.DankStorageForge;
import tfar.dankstorage.inventory.DankInterface;
import tfar.dankstorage.network.DankPacketHandler;
import tfar.dankstorage.network.server.*;
import tfar.dankstorage.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import tfar.dankstorage.utils.ButtonAction;
import tfar.dankstorage.utils.Utils;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public void sendGhostItemSlot(ServerPlayer player, int id, int slot, ItemStack stack) {
        DankPacketHandler.sendGhostItemSlot(player, id, slot, stack);
    }

    @Override
    public void sendCustomSyncData(ServerPlayer player, int stateID, int containerID, NonNullList<ItemStack> stacks, ItemStack carried) {
        DankPacketHandler.sendCustomSyncData(player, stateID, containerID, stacks, carried);
    }

    @Override
    public void sendCustomSlotChange(ServerPlayer player, int id, int slot, ItemStack stack) {
        DankPacketHandler.sendCustomSlotChange(player, id, slot, stack);
    }

    @Override
    public void sendRequestContentsPacket(int frequency) {
        C2SRequestContentsPacket.send(frequency);
    }

    @Override
    public void sendScrollPacket(boolean right) {
        C2SScrollSlotPacket.send(right);
    }

    @Override
    public void sendFrequencyPacket(int frequency, boolean set) {
        C2SSetFrequencyPacket.send(frequency,set);
    }

    @Override
    public void sendLockSlotPacket(int index) {
        C2SLockSlotPacket.send(index);
    }

    @Override
    public void sendButtonPacket(ButtonAction action) {
        C2SButtonPacket.send(action);
    }

    @Override
    public boolean showPreview() {
        return DankStorageForge.ClientConfig.preview.get();
    }

    @Override
    public int previewX() {
        return DankStorageForge.ClientConfig.preview_x.get();
    }

    @Override
    public int previewY() {
        return DankStorageForge.ClientConfig.preview_y.get();
    }

    @Override
    public DankInterface getInventoryCommon(ItemStack bag, Level level) {
        return Utils.getInventory(bag,level);
    }
}