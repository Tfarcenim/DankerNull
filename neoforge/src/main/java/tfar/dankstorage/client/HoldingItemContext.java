package tfar.dankstorage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import tfar.dankstorage.utils.CommonUtils;

public class HoldingItemContext implements IKeyConflictContext {

    public static final HoldingItemContext INSTANCE = new HoldingItemContext();
    @Override
    public boolean isActive() {
        if (!KeyConflictContext.IN_GAME.isActive()) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        return CommonUtils.isHoldingDank(player);
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return this == other;
    }
}
