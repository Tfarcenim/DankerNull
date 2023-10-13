package tfar.dankstorage.container;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import tfar.dankstorage.inventory.DankSlot;
import tfar.dankstorage.menu.CAbstractDankMenu;
import tfar.dankstorage.menu.CustomSync;
import tfar.dankstorage.network.DankPacketHandler;
import tfar.dankstorage.utils.PickupMode;
import tfar.dankstorage.world.DankInventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractDankMenu extends CAbstractDankMenu {

    public final int rows;
    public DankInventory dankInventory;
    protected final DataSlot pickup;

    public AbstractDankMenu(MenuType<?> type, int windowId, Inventory playerInventory, DankInventory dankInventory) {
        super(type, windowId,playerInventory);
        this.rows = dankInventory.dankStats.slots / 9;
        this.dankInventory = dankInventory;
        addDataSlots(dankInventory);
        if (!playerInventory.player.level().isClientSide) {
            setSynchronizer(new CustomSync((ServerPlayer) playerInventory.player));
        }
        pickup = playerInventory.player.level().isClientSide ? DataSlot.standalone(): getServerPickupData();
        addDataSlot(pickup);
    }

    protected abstract DataSlot getServerPickupData();
    public PickupMode getMode() {
        return PickupMode.PICKUP_MODES[pickup.get()];
    }

    protected void addDankSlots() {
        int slotIndex = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18;
                int y = row * 18 + 18;
                this.addSlot(new DankSlot(dankInventory, slotIndex, x, y));
                slotIndex++;
            }
        }
    }

    protected void addPlayerSlots(Inventory playerinventory) {
        int yStart = 32 + 18 * rows;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18;
                int y = row * 18 + yStart;
                this.addSlot(new Slot(playerinventory, col + row * 9 + 9, x, y));
            }
        }

        for (int row = 0; row < 9; ++row) {
            int x = 8 + row * 18;
            int y = yStart + 58;
            this.addSlot(new Slot(playerinventory, row, x, y));
        }
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();


            if (index < rows * 9) {
                if (!this.moveItemStackTo(slotStack, rows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, rows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess(){

            @Override
            public ItemStack get() {
                return getCarried();
            }

            @Override
            public boolean set(ItemStack itemStack) {
                setCarried(itemStack);
                return true;
            }
        };
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) {
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        //the remote inventory needs to know about locked slots
        for (int i = 0; i < dankInventory.dankStats.slots;i++) {
            DankPacketHandler.sendGhostItemSlot((ServerPlayer) playerInventory.player,containerId,i,dankInventory.getGhostItem(i));
        }
    }

}
