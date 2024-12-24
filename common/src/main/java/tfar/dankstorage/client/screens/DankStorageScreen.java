package tfar.dankstorage.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import tfar.dankstorage.DankStorage;
import tfar.dankstorage.ModTags;
import tfar.dankstorage.TextComponents;
import tfar.dankstorage.client.*;
import tfar.dankstorage.client.button.SmallButton;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.menu.DankMenu;
import tfar.dankstorage.network.server.C2SLockSlotPacket;
import tfar.dankstorage.network.server.C2SSetFrequencyPacket;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.PickupMode;
import tfar.dankstorage.utils.TxtColor;

import java.util.List;
import java.util.function.Supplier;

public class DankStorageScreen extends AbstractContainerScreen<DankMenu> {

    static final ResourceLocation background1 = DankStorage.id(
            "textures/container/gui/dank1.png");
    static final ResourceLocation background2 = DankStorage.id(
            "textures/container/gui/dank2.png");
    static final ResourceLocation background3 = DankStorage.id(
            "textures/container/gui/dank3.png");
    static final ResourceLocation background4 = DankStorage.id(
            "textures/container/gui/dank4.png");
    static final ResourceLocation background5 = DankStorage.id(
            "textures/container/gui/dank5.png");
    static final ResourceLocation background6 = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    static final ResourceLocation background7 = DankStorage.id(
            "textures/container/gui/dank7.png");

    final ResourceLocation background;
    EditBox frequency;
    protected final boolean is7;

    Button changeSortingType;
    Button autoSort;


    private final ConfigComponent configComponent = new ConfigComponent();

    public DankStorageScreen(DankMenu $$0, Inventory $$1, Component $$2, ResourceLocation background) {
        super($$0, $$1, $$2);
        this.imageHeight = 114 + this.menu.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;

        this.background = background;
        this.is7 = this.menu.rows > 6;
    }

    public static DankStorageScreen t1(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background1);
    }

    public static DankStorageScreen t2(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background2);
    }

    public static DankStorageScreen t3(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background3);
    }

    public static DankStorageScreen t4(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background4);
    }

    public static DankStorageScreen t5(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background5);
    }

    public static DankStorageScreen t6(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background6);
    }

    public static DankStorageScreen t7(DankMenu container, Inventory playerinventory, Component component) {
        return new DankStorageScreen(container, playerinventory, component, background7);
    }

    protected void initEditbox() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.frequency = new NumberEditBox(this.font, i + 92, j + inventoryLabelY, 56, 12, CommonUtils.translatable("dank"));
        this.frequency.setCanLoseFocus(true);
        this.frequency.setTextColor(-1);
        this.frequency.setTextColorUneditable(-1);
        this.frequency.setBordered(false);
        this.frequency.setMaxLength(10);
        this.frequency.setResponder(this::onNameChanged);
        this.frequency.setValue("");
        this.frequency.setTextColor(0xff00ff00);
        this.addWidget(this.frequency);
    }

    private void onNameChanged(String string) {
        try {
            int i = Integer.parseInt(string);
            C2SSetFrequencyPacket.send(i, false);
        } catch (NumberFormatException e) {
            C2SSetFrequencyPacket.send(-1, false);
        }
    }


    public void renderSlot(GuiGraphics pGuiGraphics, Slot pSlot) {
        if (!menu.isDankSlot(pSlot)) {
            super.renderSlot(pGuiGraphics, pSlot);
        } else {
            int i = pSlot.x;
            int j = pSlot.y;
            ItemStack itemstack = pSlot.getItem();
            boolean flag = false;
            boolean flag1 = pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
            ItemStack itemstack1 = this.menu.getCarried();
            String s = "";
            if (pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemstack.isEmpty()) {
                itemstack = itemstack.copyWithCount(itemstack.getCount() / 2);
            } else if (this.isQuickCrafting && this.quickCraftSlots.contains(pSlot) && !itemstack1.isEmpty()) {
                if (this.quickCraftSlots.size() == 1) {
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(pSlot, itemstack1, true) && this.menu.canDragTo(pSlot)) {
                    flag = true;
                    int k = Math.min(itemstack1.getMaxStackSize(), pSlot.getMaxStackSize(itemstack1));
                    int l = pSlot.getItem().isEmpty() ? 0 : pSlot.getItem().getCount();
                    int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(this.quickCraftSlots, this.quickCraftingType, itemstack1) + l;
                    if (i1 > k) {
                        i1 = k;
                        s = ChatFormatting.YELLOW.toString() + k;
                    }

                    itemstack = itemstack1.copyWithCount(i1);
                } else {
                    this.quickCraftSlots.remove(pSlot);
                    this.recalculateQuickCraftRemaining();
                }
            }

            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
            if (itemstack.isEmpty() && pSlot.isActive()) {
                Pair<ResourceLocation, ResourceLocation> pair = pSlot.getNoItemIcon();
                if (pair != null) {
                    TextureAtlasSprite textureatlassprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
                    pGuiGraphics.blit(i, j, 0, 16, 16, textureatlassprite);
                    flag1 = true;
                }
            }

            boolean locked = pSlot.index < menu.dankInventory.slotCount() && menu.dankInventory.hasGhostItem(pSlot.index);

            if (!flag1) {
                if (flag) {
                    pGuiGraphics.fill(i, j, i + 16, j + 16, -2130706433);
                }

                pGuiGraphics.renderItem(itemstack, i, j, pSlot.x + pSlot.y * this.imageWidth);
                pGuiGraphics.renderItemDecorations(this.font, itemstack, i, j,"");

                int count = itemstack.getCount();
                if (count > 1 || !s.isEmpty() || locked) {
                    String string =s+ (count <=0 && locked ?"lock": CommonUtils.formatLargeNumber(itemstack.getCount()));
                    StackSizeRenderer.renderSizeLabelCustom(pGuiGraphics, Minecraft.getInstance().font, i, j, string, Services.PLATFORM.getConfig().textSize());
                }
            }

            pGuiGraphics.pose().popPose();

            int i1 = pSlot.x;
            int j1 = pSlot.y;
            if (!pSlot.hasItem() && locked) {
                pGuiGraphics.renderFakeItem(menu.dankInventory.getGhostItem(pSlot.index), i1, j1);
                RenderSystem.depthFunc(516);
                pGuiGraphics.fill(i1, j1, i1 + 16, j1 + 16, 0x40ffffff);
                RenderSystem.depthFunc(515);
            }
        }
    }

    private void sendButtonToServer(DankMenu.ButtonAction action) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, action.ordinal());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        int color = menu.dankInventory.getTextColor();
        this.frequency.setTextColor(color);

        PickupMode pickupMode = menu.getMode();
        modeCycleButton.setValue(pickupMode);

        this.frequency.render(graphics, mouseX, mouseY, partialTicks);

        this.configComponent.render(graphics, mouseX, mouseY, partialTicks);

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics poseStack, int i, int j) {
        RenderSystem.disableBlend();
        super.renderLabels(poseStack, i, j);
        int id = DankItem.getFrequency(menu.getBag());//menu.dankInventory.get(menu.rows * 9);
        int color = 0x008000;
        poseStack.drawString( font,"ID: " + id, 62, inventoryLabelY, color,false);
    }

    public void appendDankInfo(List<Component> tooltip, ItemStack stack) {
        if (stack.is(ModTags.BLACKLISTED_STORAGE)) tooltip.add(STORAGE_TXT);
        if (stack.is(ModTags.BLACKLISTED_USAGE)) tooltip.add(USAGE_TXT);
        if (menu.isDankSlot(hoveredSlot)) {
            Component component1 = CommonUtils.translatable("text.dankstorage.lock",
                    DankKeybinds.LOCK_SLOT.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY);
            tooltip.add(component1);
            if (stack.getCount() >= 1000) {
                Component component2 = CommonUtils.translatable(
                        "text.dankstorage.exact", CommonUtils.literal(Integer.toString(stack.getCount())).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY);
                tooltip.add(component2);
            }
        }
    }

    protected CycleButton<PickupMode> modeCycleButton;

    @Override
    protected void init() {
        super.init();
        this.configComponent.init(this.width, this.height, this.minecraft, this.menu);

        int j = (this.height - this.imageHeight) / 2;


        DynamicTooltip dynamicTooltip = DynamicTooltip.dynamic(() -> Component.translatable("dankstorage.sorting_type."+menu.dankInventory.getSortingType()));

        Button sort = new SmallButton(leftPos + 143-16, topPos + 4, 26, 12, Component.literal("Sort"), b -> {
            sendButtonToServer(DankMenu.ButtonAction.SORT);
            dynamicTooltip.dirty  = true;
        });


        sort.setTooltip(dynamicTooltip);


        this.addRenderableWidget(sort);

        modeCycleButton = CycleButton.<PickupMode>builder(pickupMode -> Component.literal("P")
                        .withStyle(Style.EMPTY.withColor(DankItem.getPickupMode(menu.getBag()).getColor())))
                .withValues(PickupMode.VALUES)
                .withTooltip(mode -> Tooltip.create(DankItem.getPickupMode(menu.getBag()).translate()))
                .withInitialValue(DankItem.getPickupMode(menu.getBag()))
                .displayOnlyValue()
                .create(leftPos + 101-16, topPos + 4, 12, 12, Component.empty(), (pickupModeCycleButton, pickupMode) -> sendButtonToServer(DankMenu.ButtonAction.TOGGLE_PICKUP));

        addRenderableWidget(modeCycleButton);

        Tooltip freqTooltip = new DualTooltip(
                Component.translatable("text.dankstorage.unlock_button"),
                Component.translatable("text.dankstorage.lock_button"),null,this);

        SmallButton lock = new SmallButton(leftPos + 115-16, topPos + 4, 12, 12,
                Component.literal(""), button -> sendButtonToServer(DankMenu.ButtonAction.LOCK_FREQUENCY)) {
            @Override
            public Component getMessage() {
                return menu.dankInventory.frequencyLocked() ? Component.literal("X").withStyle(ChatFormatting.RED) :
                        Component.literal("O");
            }
        };

        lock.setTooltip(freqTooltip);

        this.addRenderableWidget(lock);

        Tooltip saveTooltip = Tooltip.create(SAVE_C);

        SmallButton s = new SmallButton(leftPos + 155, j + inventoryLabelY - 2, 12, 12,
                Component.literal("s"), b -> {
            try {
                if (menu.dankInventory.frequencyLocked()) return;
                int id1 = Integer.parseInt(frequency.getValue());
                C2SSetFrequencyPacket.send(id1, true);
            } catch (NumberFormatException e) {

            }
        });

        s.setTooltip(saveTooltip);

        this.addRenderableWidget(s);

        Tooltip compressTooltip = Tooltip.create(Component.translatable("text.dankstorage.compress_button"));

        SmallButton c = new SmallButton(leftPos + 129-16, topPos + 4, 12, 12,
                Component.literal("C"), button -> sendButtonToServer(DankMenu.ButtonAction.COMPRESS));
        c.setTooltip(compressTooltip);

        this.addRenderableWidget(c);

        Tooltip tooltip = Tooltip.create(TextComponents.OPEN_CONFIG);
        this.addRenderableWidget(Button.builder(Component.literal("\uD83D\uDD27"), b -> {
            toggleConfig();
                })
                .pos(leftPos + 157, topPos + 4)
                .size(12, 12)
                .tooltip(tooltip).build());

        DynamicTooltip dynamicTooltipSorting = DynamicTooltip.dynamic(() -> Component.translatable("dankstorage.sorting_type."+menu.dankInventory.getSortingType()+".desc"));

        changeSortingType = new Button(leftPos -89, topPos + 24, 90, 16,Component.empty(), b -> {
            sendButtonToServer(DankMenu.ButtonAction.CYCLE_SORT_TYPE);
            dynamicTooltipSorting.dirty = true;
        }, Supplier::get) {
            @Override
            public Component getMessage() {
                return Component.translatable("dankstorage.sorting_type."+menu.dankInventory.getSortingType());
            }
        };

        changeSortingType.setTooltip(dynamicTooltipSorting);

        this.addRenderableWidget(changeSortingType);

        autoSort = new Button(leftPos - 89, topPos + 44, 90, 16,Component.empty(), b -> sendButtonToServer(DankMenu.ButtonAction.TOGGLE_AUTO_SORT), Supplier::get) {
            @Override
            public Component getMessage() {
                return Component.translatable("dankstorage.auto_sort").append(" ")
                        .append(menu.dankInventory.autoSort() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
            }
        };


        this.addRenderableWidget(autoSort);
        if (!configComponent.isVisible()) {
            changeSortingType.visible = false;
            autoSort.visible = false;
        }

        initEditbox();
    }

    void toggleConfig() {
        configComponent.toggleVisibility();
        changeSortingType.visible = configComponent.isVisible();
        autoSort.visible = configComponent.isVisible();
    }


    @Override
    public List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> tooltipFromItem = super.getTooltipFromContainerItem(itemStack);
        appendDankInfo(tooltipFromItem, itemStack);
        return tooltipFromItem;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        if (is7)
            guiGraphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
        else
            guiGraphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderLockedSlots(guiGraphics);
        configComponent.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    }

    protected void renderLockedSlots(GuiGraphics guiGraphics) {
        for (int i = 0; i < menu.rows * 9; i++) {
            int j = i % 9;
            int k = i / 9;
            int offsetx = 8;
            int offsety = 18;
            if (this.menu.dankInventory.hasGhostItem(i)) {
                guiGraphics.fill(leftPos + j * 18 + offsetx, topPos + k * 18 + offsety,
                        leftPos + j * 18 + offsetx + 16, topPos + k * 18 + offsety + 16, 0xFFFF0000);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.player.closeContainer();
        }

        //slot locking takes priority over frequency changing
        boolean match = DankKeybinds.LOCK_SLOT.matches(keyCode, scanCode);
        if (match) {
            if (hoveredSlot != null && menu.isDankSlot(hoveredSlot)) {
                C2SLockSlotPacket.send(hoveredSlot.index);
                return true;
            }
        }

        if (!match && (this.frequency.keyPressed(keyCode, scanCode, modifiers) || this.frequency.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    static final MutableComponent SAVE_C = buildSaveComponent();
    static final MutableComponent PICKUP_C = buildPickupComponent();

    private static MutableComponent buildSaveComponent() {
        return CommonUtils.translatable("text.dankstorage.save_frequency_button",
                CommonUtils.translatable("text.dankstorage.save_frequency_button.invalid",
                                CommonUtils.translatable("text.dankstorage.save_frequency_button.invalidtxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(TxtColor.INVALID.color)),
                CommonUtils.translatable("text.dankstorage.save_frequency_button.too_high",
                                CommonUtils.translatable("text.dankstorage.save_frequency_button.too_hightxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(TxtColor.TOO_HIGH.color)),
                CommonUtils.translatable("text.dankstorage.save_frequency_button.different_tier",
                                CommonUtils.translatable("text.dankstorage.save_frequency_button.different_tiertxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(TxtColor.DIFFERENT_TIER.color)),
                CommonUtils.translatable("text.dankstorage.save_frequency_button.good",
                                CommonUtils.translatable("text.dankstorage.save_frequency_button.goodtxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(TxtColor.GOOD.color))
                , CommonUtils.translatable("text.dankstorage.save_frequency_button.locked_frequency",
                                CommonUtils.translatable("text.dankstorage.save_frequency_button.locked_frequencytxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(TxtColor.LOCKED.color))
        );
    }

    private static MutableComponent buildPickupComponent() {
        return CommonUtils.translatable("text.dankstorage.pickup_button",
                CommonUtils.translatable("text.dankstorage.pickup_button.none",
                                CommonUtils.translatable("text.dankstorage.pickup_button.nonetxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(PickupMode.none.getColor())),
                CommonUtils.translatable("text.dankstorage.pickup_button.all",
                                CommonUtils.translatable("text.dankstorage.pickup_button.alltxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(PickupMode.pickup_all.getColor())),
                CommonUtils.translatable("text.dankstorage.pickup_button.filtered",
                                CommonUtils.translatable("text.dankstorage.pickup_button.filteredtxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(PickupMode.filtered_pickup.getColor())),
                CommonUtils.translatable("text.dankstorage.pickup_button.void",
                                CommonUtils.translatable("text.dankstorage.pickup_button.voidtxt")
                                        .withStyle(ChatFormatting.GRAY))
                        .withStyle(Style.EMPTY.withColor(PickupMode.void_pickup.getColor()))
        );
    }

    static final MutableComponent STORAGE_TXT = CommonUtils.translatable("text.dankstorage.blacklisted_storage").withStyle(ChatFormatting.DARK_RED);
    static final MutableComponent USAGE_TXT = CommonUtils.translatable("text.dankstorage.blacklisted_usage").withStyle(ChatFormatting.DARK_RED);

}
