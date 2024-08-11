package mcinterface1201;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.packloading.PackParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for the JEI system.  This is responsible for populating JEI with the various items,
 * benches, configurations and whatnots.
 *
 * @author don_bruce
 */
@JeiPlugin
public class InterfaceJEI implements IModPlugin {
    private static final List<BenchRecipeCategory> benchCategories = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(InterfaceManager.coreModID, "main");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        //Check all pack items for benches.
        benchCategories.clear();
        for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
            if (packItem instanceof ItemDecor) {
                ItemDecor benchItem = (ItemDecor) packItem;
                if (benchItem.definition.decor.crafting != null) {

                    //For each bench, get all things they can craft, and add them as recipes.
                    List<PackRecipeWrapper> benchRecipes = new ArrayList<>();
                    for (AItemPack<?> packItemToTest : PackParser.getAllPackItems()) {
                        if (packItemToTest.isBenchValid(benchItem.definition.decor.crafting)) {
                            for (int i = 0; i < packItemToTest.definition.general.materialLists.size(); ++i) {
                                benchRecipes.add(new PackRecipeWrapper(packItemToTest, i, false));
                            }

                            if (packItemToTest.definition.general.repairMaterialLists != null) {
                                for (int i = 0; i < packItemToTest.definition.general.repairMaterialLists.size(); ++i) {
                                    benchRecipes.add(new PackRecipeWrapper(packItemToTest, i, true));
                                }
                            }
                        }
                    }

                    //All recipes created.  Now create and register the bench.
                    benchCategories.add(new BenchRecipeCategory(benchItem, benchRecipes, registry.getJeiHelpers().getGuiHelper()));
                }
            }
        }

        //Register all benches.
        registry.addRecipeCategories(benchCategories.toArray(new IRecipeCategory[benchCategories.size()]));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        //Register all recipes in all benches.
        for (BenchRecipeCategory benchCategory : benchCategories) {
            registration.addRecipes(benchCategory.getRecipeType(), benchCategory.benchRecipes);
        }
    }

    private static class PackRecipeWrapper {
        private final ResourceLocation id;
        private final AItemPack<?> packItem;
        private final int recipeIndex;
        private final boolean forRepair;

        private PackRecipeWrapper(AItemPack<?> packItem, int recipeIndex, boolean forRepair) {
            this.id = new ResourceLocation(InterfaceLoader.MODID, packItem.getRegistrationName());
            this.packItem = packItem;
            this.recipeIndex = recipeIndex;
            this.forRepair = forRepair;
        }
    }

    private static class BenchRecipeCategory implements IRecipeCategory<PackRecipeWrapper> {
        private final RecipeType<PackRecipeWrapper> recipeType;
        private final ItemDecor benchItem;
        private final List<PackRecipeWrapper> benchRecipes;
        private final IDrawable background;
        private final IDrawable icon;

        private BenchRecipeCategory(ItemDecor benchItem, List<PackRecipeWrapper> benchRecipes, IGuiHelper guiHelper) {
            this.recipeType = RecipeType.create(
                    InterfaceManager.coreModID,
                    benchItem.getRegistrationName(),
                    PackRecipeWrapper.class
            );
            this.benchItem = benchItem;
            this.benchRecipes = benchRecipes;
            this.background = guiHelper.createDrawable(new ResourceLocation(InterfaceManager.coreModID, "textures/guis/jei_crafting.png"), 0, 0, 134, 97);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ((WrapperItemStack) benchItem.getNewStack(null)).stack);
        }

        @Override
        public RecipeType<PackRecipeWrapper> getRecipeType() {
            return recipeType;
        }

        @Override
        public Component getTitle() {
            return Component.literal(benchItem.getItemName());
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, PackRecipeWrapper recipe, IFocusGroup focuses) {
            List<PackMaterialComponent> components = PackMaterialComponent.parseFromJSON(recipe.packItem, recipe.recipeIndex, true, true, recipe.forRepair, true);
            if (components != null) {
                //Set inputs.
                final int numRows = 3;
                final int numCols = 7;
                int xOffset;
                int yOffset;

                for (int i = 0; i < components.size() && i < numRows * numCols; ++i) {
                    List<ItemStack> inputs = new ArrayList<>();

                    for (IWrapperItemStack stack : components.get(i).possibleItems) {
                        inputs.add(((WrapperItemStack) stack).stack);
                    }

                    xOffset = 5 + 17 * (i % numCols);
                    yOffset = 5 + 17 * (i / numCols);
                    builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset)
                            .addItemStacks(inputs);
                }

                //Set output.  (For some reason the position in the texture is off by 1px for JEI?
                builder.addSlot(RecipeIngredientRole.OUTPUT, 58, 70)
                        .addItemStack(((WrapperItemStack) recipe.packItem.getNewStack(null)).stack);
            }
        }

        @Override
        public @Nullable ResourceLocation getRegistryName(PackRecipeWrapper recipe) {
            return recipe.id;
        }
    }
}
