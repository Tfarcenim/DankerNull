package tfar.dankstorage.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import tfar.dankstorage.inventory.DankInterface;
import tfar.dankstorage.inventory.LockedSlot;
import tfar.dankstorage.network.client.S2CSendGhostSlotPacket;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.PickupMode;

import javax.annotation.Nonnull;
import java.util.Optional;

public abstract class AbstractDankMenu extends AbstractContainerMenu {

    public final Inventory playerInventory;
    public final int rows;
    public final DankInterface dankInventory;
    protected final DataSlot pickup;

    public PickupMode getMode() {
        return PickupMode.VALUES[pickup.get()];
    }

    public enum ButtonAction {
        LOCK_FREQUENCY, SORT,
        TOGGLE_TAG, TOGGLE_PICKUP,  COMPRESS;
        static final ButtonAction[] VALUES = values();
    }


    public AbstractDankMenu(MenuType<?> type, int windowId, Inventory playerInventory, DankInterface dankInventory) {
        super(type, windowId);
        this.playerInventory = playerInventory;
        this.dankInventory = dankInventory;
        this.rows = dankInventory.getContainerSizeDank() /9;
        addDataSlots(dankInventory);
        if (!playerInventory.player.level().isClientSide) {
            setSynchronizer(new CustomSync((ServerPlayer) playerInventory.player));
        }
        pickup = playerInventory.player.level().isClientSide ? DataSlot.standalone(): getServerPickupData();
        addDataSlot(pickup);
    }

    protected abstract DataSlot getServerPickupData();

    protected void addPlayerSlots(Inventory playerinventory, int locked) {
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
            if (row != locked)
                this.addSlot(new Slot(playerinventory, row, x, y));
            else
                this.addSlot(new LockedSlot(playerinventory, row, x, y));
        }
    }

    @Override
    public void doClick(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {

        if (pClickType == ClickType.SWAP && pSlotId > 0 && isDankSlot(slots.get(pSlotId))) {
            return;
        }

        if (pClickType != ClickType.PICKUP) {
            super.doClick(pSlotId, pButton, pClickType, pPlayer);
        } else
        {
            Inventory inventory = pPlayer.getInventory();
            if (this.quickcraftStatus != 0) {
                this.resetQuickCraft();
            } else if (pButton == 0 || pButton == 1) {
                ClickAction clickaction = pButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
                if (pSlotId == SLOT_CLICKED_OUTSIDE) {
                    if (!this.getCarried().isEmpty()) {
                        if (clickaction == ClickAction.PRIMARY) {
                            pPlayer.drop(this.getCarried(), true);
                            this.setCarried(ItemStack.EMPTY);
                        } else {
                            pPlayer.drop(this.getCarried().split(1), true);
                        }
                    }
                } else {
                    if (pSlotId < 0) {
                        return;
                    }

                    Slot slot7 = this.slots.get(pSlotId);
                    ItemStack itemstack9 = slot7.getItem();
                    ItemStack itemstack10 = this.getCarried();
                    pPlayer.updateTutorialInventoryAction(itemstack10, slot7.getItem(), clickaction);
                    if (!this.tryItemClickBehaviourOverride(pPlayer, clickaction, slot7, itemstack9, itemstack10)) {
                        if (!Services.PLATFORM.onItemStackedOn(itemstack9, itemstack10, slot7, clickaction, pPlayer, createCarriedSlotAccess()))
                            if (itemstack9.isEmpty()) {
                                if (!itemstack10.isEmpty()) {
                                    int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                                    this.setCarried(slot7.safeInsert(itemstack10, i3));
                                }
                            } else if (slot7.mayPickup(pPlayer)) {
                                if (itemstack10.isEmpty()) {
                                    int j3 = clickaction == ClickAction.PRIMARY ? itemstack9.getCount() : (itemstack9.getCount() + 1) / 2;
                                    Optional<ItemStack> optional1 = slot7.tryRemove(j3, Integer.MAX_VALUE, pPlayer);
                                    optional1.ifPresent((p_150421_) -> {
                                        this.setCarried(p_150421_);
                                        slot7.onTake(pPlayer, p_150421_);
                                    });
                                } else if (slot7.mayPlace(itemstack10)) {
                                    if (ItemStack.isSameItemSameTags(itemstack9, itemstack10)) {
                                        int k3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : 1;
                                        this.setCarried(slot7.safeInsert(itemstack10, k3));
                                    } else if (itemstack10.getCount() <= slot7.getMaxStackSize(itemstack10) && itemstack9.getCount() <=itemstack9.getMaxStackSize()) {//thanks vanilla
                                        this.setCarried(itemstack9);
                                        slot7.setByPlayer(itemstack10);
                                    }
                                } else if (ItemStack.isSameItemSameTags(itemstack9, itemstack10)) {
                                    Optional<ItemStack> optional = slot7.tryRemove(itemstack9.getCount(), itemstack10.getMaxStackSize() - itemstack10.getCount(), pPlayer);
                                    optional.ifPresent((p_150428_) -> {
                                        itemstack10.grow(p_150428_.getCount());
                                        slot7.onTake(pPlayer, p_150428_);
                                    });
                                }
                            }
                    }

                    slot7.setChanged();
                }
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= ButtonAction.VALUES.length) return false;
        ButtonAction buttonAction = ButtonAction.VALUES[id];
        if (player instanceof ServerPlayer serverPlayer) {
            switch (buttonAction) {
                case LOCK_FREQUENCY -> dankInventory.toggleFrequencyLock();
                case SORT -> dankInventory.sort();
                case COMPRESS -> dankInventory.compress(serverPlayer);
                case TOGGLE_TAG -> CommonUtils.toggleTagMode(serverPlayer);
                case TOGGLE_PICKUP -> CommonUtils.togglePickupMode(serverPlayer);
            }
        }
        return true;
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

    protected void addDankSlots() {
        int slotIndex = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18;
                int y = row * 18 + 18;
                this.addSlot(Services.PLATFORM.createSlot(dankInventory, slotIndex, x, y));
                slotIndex++;
            }
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player playerIn) {
        return true;
    }


    //used by quick transfer, needs to respect locked slots
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse) {
        boolean didSomething = false;
        int i = startIndex;

        if (reverse) {
            i = endIndex - 1;
        }

        while (!stack.isEmpty()) {
            if (reverse) {
                if (i < startIndex) break;
            } else {
                if (i >= endIndex) break;
            }

            Slot slot = this.slots.get(i);
            ItemStack slotStack = slot.getItem();

            if (!slotStack.isEmpty() && slotStack.getItem() == stack.getItem() && ItemStack.isSameItemSameTags(stack, slotStack)) {
                int combinedCount = slotStack.getCount() + stack.getCount();
                int maxSize = slot.getMaxStackSize(slotStack);

                if (combinedCount <= maxSize) {
                    stack.setCount(0);
                    slotStack.setCount(combinedCount);
                    slot.setChanged();
                    didSomething = true;
                } else if (slotStack.getCount() < maxSize) {
                    stack.shrink(maxSize - slotStack.getCount());
                    slotStack.setCount(maxSize);
                    slot.setChanged();
                    didSomething = true;
                }
            }

            i += reverse ? -1 : 1;
        }

        if (!stack.isEmpty()) {
            if (reverse) i = endIndex - 1;
            else i = startIndex;

            while (true) {
                if (reverse) {
                    if (i < startIndex) break;
                } else {
                    if (i >= endIndex) break;
                }

                Slot slot = this.slots.get(i);
                ItemStack itemstack1 = slot.getItem();

                if (itemstack1.isEmpty() && slot.mayPlace(stack)) {
                    if (stack.getCount() > slot.getMaxStackSize(stack)) {
                        slot.set(stack.split(slot.getMaxStackSize(stack)));
                    } else {
                        slot.set(stack.split(stack.getCount()));
                    }

                    slot.setChanged();
                    didSomething = true;
                    break;
                }

                i += reverse ? -1 : 1;
            }
        }

        return didSomething;
    }

    public boolean isDankSlot(Slot slot) {
        return slot.getClass().getName().endsWith("DankSlot");
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        //the remote inventory needs to know about locked slots
        for (int i = 0; i < dankInventory.getDankStats().slots; i++) {
            Services.PLATFORM.sendToClient(new S2CSendGhostSlotPacket(containerId,i, dankInventory.getGhostItem(i)), (ServerPlayer)
                    playerInventory.player);
        }
    }

    public abstract void setFrequency(int freq);
}
