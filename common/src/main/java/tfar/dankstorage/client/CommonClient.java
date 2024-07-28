package tfar.dankstorage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import tfar.dankstorage.client.screens.CDankStorageScreen;
import tfar.dankstorage.init.ModMenuTypes;
import tfar.dankstorage.menu.DankMenu;
import tfar.dankstorage.menu.DockMenu;

public class CommonClient {

    public static ClientTooltipComponent tooltipImage(TooltipComponent data) {
        if (data instanceof DankTooltip dankTooltip) {
            return new ClientDankTooltip(dankTooltip);
        }
        return null;
    }

    public static Player getLocalPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void setup() {
        MenuScreens.register(ModMenuTypes.dank_1_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t1(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_1_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t1(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_2_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t2(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_2_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t2(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_3_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t3(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_3_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t3(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_4_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t4(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_4_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t4(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_5_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t5(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_5_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t5(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_6_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t6(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_6_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t6(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.dank_7_container, (DockMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t7(container, playerinventory, component));
        MenuScreens.register(ModMenuTypes.portable_dank_7_container, (DankMenu container, Inventory playerinventory, Component component) -> CDankStorageScreen.t7(container, playerinventory, component));
    }


}
