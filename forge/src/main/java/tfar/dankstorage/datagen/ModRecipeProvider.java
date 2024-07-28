package tfar.dankstorage.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import tfar.dankstorage.init.ModItems;
import tfar.dankstorage.init.ModRecipeSerializers;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DANKS.get("dank_1"))
                .define('c', ItemTags.COALS)
                .define('b', Tags.Items.BARRELS_WOODEN)
                .pattern("ccc")
                .pattern("cbc")
                .pattern("ccc")
                .unlockedBy(getHasName(Blocks.BARREL),has(Tags.Items.BARRELS_WOODEN))
                .save(pWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DOCK)
                .define('c', Blocks.BLACK_CONCRETE)
                .pattern("ccc")
                .pattern("c c")
                .pattern("ccc")
                .unlockedBy(getHasName(Blocks.BLACK_CONCRETE),has(Blocks.BLACK_CONCRETE))
                .save(pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_2"),ModItems.UPGRADES.get("1_to_2"),ModItems.DANKS.get("dank_1"),
                Ingredient.of(Blocks.REDSTONE_BLOCK),Ingredient.of(Blocks.REDSTONE_BLOCK),pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_3"),ModItems.UPGRADES.get("2_to_3"),ModItems.DANKS.get("dank_2"),
                Ingredient.of(Blocks.GOLD_BLOCK),Ingredient.of(Blocks.GOLD_BLOCK),pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_4"),ModItems.UPGRADES.get("3_to_4"),ModItems.DANKS.get("dank_3"),
                Ingredient.of(Blocks.EMERALD_BLOCK),Ingredient.of(Blocks.EMERALD_BLOCK),pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_5"),ModItems.UPGRADES.get("4_to_5"),ModItems.DANKS.get("dank_4"),
                Ingredient.of(Blocks.DIAMOND_BLOCK),Ingredient.of(Blocks.DIAMOND_BLOCK),pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_6"),ModItems.UPGRADES.get("5_to_6"),ModItems.DANKS.get("dank_5"),
                Ingredient.of(Blocks.CRYING_OBSIDIAN),Ingredient.of(Blocks.AMETHYST_BLOCK),pWriter);

        createDankAndUpgrade(ModItems.DANKS.get("dank_7"),ModItems.UPGRADES.get("6_to_7"),ModItems.DANKS.get("dank_6"),
                Ingredient.of(Items.NETHER_STAR),Ingredient.of(Items.NETHER_STAR),pWriter);
    }

    protected void createDankAndUpgrade(Item dank, Item upgrade, Item previousDank, Ingredient around,Ingredient around2,Consumer<FinishedRecipe> pWriter) {
        ShapedRecipeBuilderCustom.shaped(RecipeCategory.TOOLS, dank)
                .define('c', around)
                .define('d', around2)
                .define('b', previousDank)
                .pattern("dcd")
                .pattern("cbc")
                .pattern("dcd")
                .serializer(ModRecipeSerializers.upgrade)
                .unlockedBy(getHasName( previousDank),has(previousDank))
                .save(pWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, upgrade)
                .define('c', around)
                .define('d', around2)
                .pattern("dcd")
                .pattern("c c")
                .pattern("dcd")
                .unlockedBy(getHasName( previousDank),has(previousDank))
                .save(pWriter);
    }

}
