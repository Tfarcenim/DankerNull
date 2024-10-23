package tfar.dankstorage.utils;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

public enum UseType implements StringRepresentable {
    bag, construction;
    public static final UseType[] VALUES = UseType.values();

    public MutableComponent translate() {
        return CommonUtils.translatable("dankstorage.usetype." + this);
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
