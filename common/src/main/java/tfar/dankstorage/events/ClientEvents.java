package tfar.dankstorage.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import tfar.dankstorage.item.DankItem;
import tfar.dankstorage.network.server.C2SScrollSlotPacket;
import tfar.dankstorage.platform.Services;
import tfar.dankstorage.utils.CommonUtils;
import tfar.dankstorage.utils.UseType;

public class ClientEvents {

    public static final Minecraft mc = Minecraft.getInstance();

    public static boolean onScroll(double delta) {
        Player player = mc.player;
        if (player!=null) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (player.isCrouching() && (DankItem.isConstruction(main) || DankItem.isConstruction(off))) {
                boolean right = delta < 0;
                C2SScrollSlotPacket.send(right);
                return true;
            }
        }
        return false;
    }

    public static void renderSelectedItem(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        Player player = mc.player;
        if (player == null)
            return;
        if (!(player.containerMenu instanceof InventoryMenu)) return;
        ItemStack bag = player.getMainHandItem();
        if (!(bag.getItem() instanceof DankItem)) {
            bag = player.getOffhandItem();
            if (!(bag.getItem() instanceof DankItem))
                return;
        }
        int xStart = guiGraphics.guiWidth() / 2 + previewX();
        int yStart = guiGraphics.guiHeight() + previewY();

        ItemStack toPlace = DankItem.getSelectedItem(bag);

        if (!toPlace.isEmpty() && shouldPreview()) {
            Integer color = toPlace.getRarity().color().getColor();
            int c = color != null ? color : 0xFFFFFF;
            renderHotbarItem(guiGraphics, xStart, yStart, toPlace);
        }
        UseType mode = DankItem.getUseType(bag);
        MutableComponent translate = mode.translate();

        final int stringX = xStart + 8 - mc.font.width(translate) / 2;
        final int stringY = yStart + 16;
        guiGraphics.drawString(mc.font,translate, stringX, stringY, 0xffffff);
    }

    private static void renderHotbarItem(GuiGraphics poses, int x, int y, ItemStack stack) {
        poses.renderFakeItem(stack, x, y);
        poses.renderItemDecorations(mc.font, stack, x, y);
    }

    private static boolean shouldPreview() {
        return Services.PLATFORM.getConfig().showPreview();
    }

    private static int previewX() {
        return Services.PLATFORM.getConfig().posX();
    }

    private static int previewY() {
        return Services.PLATFORM.getConfig().posY();
    }
}
