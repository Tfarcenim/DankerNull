package tfar.dankstorage.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tfar.dankstorage.block.DockBlock;
import tfar.dankstorage.init.ModBlockEntityTypes;
import tfar.dankstorage.inventory.DankInventory;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.*;
import tfar.dankstorage.world.DankSavedData;

import javax.annotation.Nullable;

public class DockBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    public DockBlockEntity(BlockPos $$1, BlockState $$2) {
        super(ModBlockEntityTypes.dock, $$1, $$2);
    }

    ItemStack dank = ItemStack.EMPTY;

    public int numPlayersUsing = 0;

    public ItemStack getDank() {
        return dank;
    }

    public void setFrequency(int freq) {
        if (!dank.isEmpty()) {
            DankItem.setFrequency(dank,freq);
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.numPlayersUsing = type;
            this.setChanged();
            return true;
        } else {
            return super.triggerEvent(id, type);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (dank.getItem() instanceof DankItem tankItem) {
            return tankItem.createProvider(dank).createMenu(i, inventory, player);
        }
        return null;
    }

    public static final DankInventory EMPTY = Services.PLATFORM.createInventory(DankStats.zero,null);

    public DankInventory getInventory() {
        if (!(dank.getItem() instanceof DankItem)) return EMPTY;
        int frequency = DankItem.getFrequency(dank);
        if (frequency < 0) {
            return EMPTY;
            //DankItem.assignNextFreeId(level.getServer(),dank);
            //return DankSavedData.getOrCreate(DankItem.getFrequency(dank),level.getServer()).getOrCreateInventory();
        }

        return DankSavedData.get(DankItem.getFrequency(dank),level.getServer()).getOrCreateInventory();
    }

    public Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public Component getName() {
        Component custom = getCustomName();
        return custom != null ? custom : getDefaultName();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return dank.isEmpty() ? null : dank.getHoverName();
    }

    public int getComparatorSignal() {
        return this.getInventory().calcRedstone();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(tag, pRegistries);
        dank = ItemStack.parseOptional(pRegistries,tag.getCompound("dank"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(tag, pRegistries);
        tag.put("dank",dank.saveOptional(pRegistries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    public void giveToPlayer(Player player) {
        ItemStack dankInStack = removeDankWithoutItemSpawn();

        if (!player.addItem(dankInStack)) {
            ItemEntity entity = new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), dankInStack);
            level.addFreshEntity(entity);
        }
    }

    public void removeDankWithItemSpawn() {
        ItemStack dankInStack = removeDankWithoutItemSpawn();
        ItemEntity entity = new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), dankInStack);
        level.addFreshEntity(entity);
    }

    public ItemStack removeDankWithoutItemSpawn() {
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(DockBlock.TIER, 0));
        ItemStack stack = dank.copy();
        dank = ItemStack.EMPTY;
        setChanged();
        return stack;
    }



    public void addDank(ItemStack tank) {
        if (tank.getItem() instanceof DankItem tankItem) {
            DankStats stats = tankItem.stats;
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(DockBlock.TIER, stats.ordinal()));
            this.dank = tank.split(1);
            setChanged();
        }
    }

    public void upgradeTo(DankStats stats) {
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(DockBlock.TIER, stats.ordinal()));
        DankInventory dankInventory = getInventory();
        dankInventory.upgradeTo(stats);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        if (!dank.isEmpty()) {
            DataComponentMap components = dank.getComponents();
            for(DataComponentType<?> type : components.keySet()) {
                pComponents.set((DataComponentType) type,dank.get(type));
            }
        }
    }
}
