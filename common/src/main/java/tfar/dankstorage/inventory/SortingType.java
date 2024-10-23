package tfar.dankstorage.inventory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Objects;

public enum SortingType {

    descending((stack1, stack2) -> stack2.getCount() - stack1.getCount()),
    ascending(Comparator.comparingInt(ItemStack::getCount)),
    registry_name((stack1,stack2) -> {
        String path1 = BuiltInRegistries.ITEM.getKey(stack1.getItem()).getPath();
        String path2 = BuiltInRegistries.ITEM.getKey(stack2.getItem()).getPath();
        return path1.toString().compareTo(path2.toString());
    }),
    modid((stack1,stack2) -> {

        ResourceLocation rl1 = BuiltInRegistries.ITEM.getKey(stack1.getItem());
        ResourceLocation rl2 = BuiltInRegistries.ITEM.getKey(stack2.getItem());

        if (Objects.equals(rl1.getNamespace(),rl2.getNamespace())) {
            return rl1.getPath().compareTo(rl2.getPath());
        }
        return rl1.getNamespace().compareTo(rl2.getNamespace());
    });

    public final Comparator<ItemStack> comparator;

    SortingType(Comparator<ItemStack> comparator) {
        this.comparator = comparator;
    }

}
