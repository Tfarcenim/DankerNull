package tfar.dankstorage.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
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
import tfar.dankstorage.blockentity.CommonDockBlockEntity;
import tfar.dankstorage.inventory.DankInterface;
import tfar.dankstorage.item.CDankItem;
import tfar.dankstorage.network.client.S2CModPacket;
import tfar.dankstorage.network.server.C2SModPacket;
import tfar.dankstorage.utils.DankStats;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

    <MSG extends S2CModPacket> void registerClientPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf,MSG> reader);

    <MSG extends C2SModPacket> void registerServerPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf,MSG> reader);


    void sendToClient(S2CModPacket msg, ServerPlayer player);
    void sendToServer(C2SModPacket msg);

    ItemStack getCloneStack(Level level, BlockPos pos, BlockState state, HitResult hitResult, Player player);


    DankInterface createInventory(DankStats stats,int frequency);
    boolean showPreview();
    int previewX();
    int previewY();

    Slot createSlot(DankInterface dankInventory, int index, int xPosition, int yPosition);

    default  <F> void registerAll(Class<?> clazz, Registry<F> registry, Class<? extends F> filter) {
        Map<String,F> map = new HashMap<>();
        unfreeze(registry);
        for (Field field : clazz.getFields()) {
            try {
                Object o = field.get(null);
                if (filter.isInstance(o)) {
                    map.put(field.getName().toLowerCase(Locale.ROOT),(F)o);
                }
            } catch (IllegalAccessException illegalAccessException) {
                illegalAccessException.printStackTrace();
            }
        }
        registerAll(map,registry,filter);
    }

    default <F> void unfreeze(Registry<F> registry) {

    }

    <F> void registerAll(Map<String,? extends F> map, Registry<F> registry, Class<? extends F> filter);

        //registry helpers

    default CDankItem create(Item.Properties properties,DankStats stats) {
        return new CDankItem(properties,stats);
    }

    CommonDockBlockEntity<?> blockEntity(BlockPos pos, BlockState state);

   boolean onItemStackedOn(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess);

}