package tfar.dankstorage.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.MixinEnvironment;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.blockentity.CommonDockBlockEntity;
import tfar.dankstorage.blockentity.DockBlockEntity;
import tfar.dankstorage.inventory.DankInterface;
import tfar.dankstorage.inventory.DankSlot;
import tfar.dankstorage.network.ClientDankPacketHandlerFabric;
import tfar.dankstorage.network.DankPacketHandler;
import tfar.dankstorage.network.DankPacketHandlerFabric;
import tfar.dankstorage.network.client.S2CModPacket;
import tfar.dankstorage.network.server.C2SModPacket;
import tfar.dankstorage.platform.services.IPlatformHelper;
import tfar.dankstorage.utils.DankStats;
import tfar.dankstorage.world.DankInventoryFabric;

import java.util.Map;
import java.util.function.Function;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <MSG extends S2CModPacket> void registerClientPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf, MSG> reader) {
        if (MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT) {
            ClientDankPacketHandlerFabric.register(packetLocation,reader);
        }
    }

    @Override
    public <MSG extends C2SModPacket> void registerServerPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf, MSG> reader) {
        ServerPlayNetworking.registerGlobalReceiver(DankPacketHandler.packet(packetLocation), DankPacketHandlerFabric.wrapC2S(reader));
    }


    @Override
    public void sendToClient(S2CModPacket msg, ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        msg.write(buf);
        ServerPlayNetworking.send(player, DankPacketHandler.packet(msg.getClass()), buf);
    }

    @Override
    public void sendToServer(C2SModPacket msg) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        msg.write(buf);
        ClientPlayNetworking.send(DankPacketHandler.packet(msg.getClass()), buf);
    }

    @Override
    public DankInterface createInventory(DankStats stats, int frequency) {
        return new DankInventoryFabric(stats,frequency);
    }

    @Override
    public Slot createSlot(DankInterface dankInventory, int index, int xPosition, int yPosition) {
        return new DankSlot((DankInventoryFabric) dankInventory,index,xPosition,yPosition);
    }

    @Override
    public <F> void registerAll(Map<String,? extends F> map, Registry<F> registry, Class<? extends F> filter) {
        for (Map.Entry<String,? extends F> entry : map.entrySet()) {
            Registry.register(registry, DankStorage.id(entry.getKey()),entry.getValue());
        }
    }

    @Override
    public ItemStack getCloneStack(Level level, BlockPos pos, BlockState state, HitResult hitResult, Player player) {
        return state.getBlock().getCloneItemStack(level,pos,state);
    }

    @Override
    public boolean showPreview() {
        return true;
    }

    //hardcoded for now

    @Override
    public int previewX() {
        return -140;
    }

    @Override
    public int previewY() {
        return -25;
    }


    @Override
    public CommonDockBlockEntity<?> blockEntity(BlockPos pos, BlockState state) {
        return new DockBlockEntity(pos,state);
    }

    @Override
    public boolean onItemStackedOn(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        return false;
    }
}
