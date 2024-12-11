package tfar.dankstorage.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.lang3.tuple.Pair;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.DankStorageForge;
import tfar.dankstorage.blockentity.CommonDockBlockEntity;
import tfar.dankstorage.blockentity.DockBlockEntity;
import tfar.dankstorage.inventory.DankInterface;
import tfar.dankstorage.inventory.DankSlot;
import tfar.dankstorage.item.CDankItem;
import tfar.dankstorage.item.DankItemForge;
import tfar.dankstorage.network.DankPacketHandlerForge;
import tfar.dankstorage.network.client.S2CModPacket;
import tfar.dankstorage.network.server.C2SModPacket;
import tfar.dankstorage.platform.services.IPlatformHelper;
import tfar.dankstorage.utils.DankStats;
import tfar.dankstorage.world.DankInventoryForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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

    int i;

    @Override
    public <MSG extends S2CModPacket> void registerClientPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf, MSG> reader) {
        DankPacketHandlerForge.INSTANCE.registerMessage(i++, packetLocation, MSG::write, reader, DankPacketHandlerForge.wrapS2C());
    }

    @Override
    public <MSG extends C2SModPacket> void registerServerPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf, MSG> reader) {
        DankPacketHandlerForge.INSTANCE.registerMessage(i++, packetLocation, MSG::write, reader, DankPacketHandlerForge.wrapC2S());
    }


    @Override
    public void sendToClient(S2CModPacket msg, ServerPlayer player) {
        DankPacketHandlerForge.sendToClient(msg, player);
    }

    @Override
    public void sendToServer(C2SModPacket msg) {
        DankPacketHandlerForge.sendToServer(msg);
    }

    @Override
    public DankInterface createInventory(DankStats stats, int frequency) {
        return new DankInventoryForge(stats, frequency);
    }

    @Override
    public Slot createSlot(DankInterface dankInventory, int index, int xPosition, int yPosition) {
        return new DankSlot((DankInventoryForge) dankInventory, index, xPosition, yPosition);
    }

    @Override
    public <F> void registerAll(Map<String, ? extends F> map, Registry<F> registry, Class<? extends F> filter) {
        List<Pair<ResourceLocation, Supplier<?>>> list = DankStorageForge.registerLater.computeIfAbsent(registry, k -> new ArrayList<>());
        for (Map.Entry<String, ? extends F> entry : map.entrySet()) {
            list.add(Pair.of(new ResourceLocation(DankStorage.MODID, entry.getKey()), entry::getValue));
        }
    }

    @Override
    public <F> void unfreeze(Registry<F> registry) {
        ((MappedRegistry<F>)registry).unfreeze();
    }

    @Override
    public ItemStack getCloneStack(Level level, BlockPos pos, BlockState state, HitResult hitResult, Player player) {
        return state.getCloneItemStack(hitResult, level, pos, player);
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
    public CDankItem create(Item.Properties properties, DankStats stats) {
        return new DankItemForge(properties, stats);
    }

    @Override
    public CommonDockBlockEntity<?> blockEntity(BlockPos pos, BlockState state) {
        return new DockBlockEntity(pos,state);
    }

    @Override
    public boolean onItemStackedOn(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        return ForgeHooks.onItemStackedOn(carriedItem, stackedOnItem, slot, action, player, carriedSlotAccess);
    }
}