package mcinterface1165;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.packloading.PackParser;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

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
            registration.addRecipes(benchCategory.benchRecipes, benchCategory.getUid());
        }
    }

    private static class PackRecipeWrapper {
        private final AItemPack<?> packItem;
        private final int recipeIndex;
        private final boolean forRepair;

        private PackRecipeWrapper(AItemPack<?> packItem, int recipeIndex, boolean forRepair) {
            this.packItem = packItem;
            this.recipeIndex = recipeIndex;
            this.forRepair = forRepair;
        }
    }

    private static class BenchRecipeCategory implements IRecipeCategory<PackRecipeWrapper> {
        private final ItemDecor benchItem;
        private final List<PackRecipeWrapper> benchRecipes;
        private final IDrawable background;
        private final IDrawable icon;

        private BenchRecipeCategory(ItemDecor benchItem, List<PackRecipeWrapper> benchRecipes, IGuiHelper guiHelper) {
            this.benchItem = benchItem;
            this.benchRecipes = benchRecipes;
            this.background = guiHelper.createDrawable(new ResourceLocation(InterfaceManager.coreModID, "textures/guis/jei_crafting.png"), 0, 0, 134, 97);
            this.icon = guiHelper.createDrawableIngredient(((WrapperItemStack) benchItem.getNewStack(null)).stack);
        }

        @Override
        public Class<PackRecipeWrapper> getRecipeClass() {
            return PackRecipeWrapper.class;
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(InterfaceManager.coreModID, benchItem.getRegistrationName());
        }

        @Override
        public String getTitle() {
            return benchItem.getItemName();
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
        public void setIngredients(PackRecipeWrapper recipe, IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            for (PackMaterialComponent component : PackMaterialComponent.parseFromJSON(recipe.packItem, recipe.recipeIndex, true, true, recipe.forRepair)) {
                List<ItemStack> stacks = new ArrayList<>();
                for (IWrapperItemStack stack : component.possibleItems) {
                    stacks.add(((WrapperItemStack) stack).stack);
                }
                inputs.add(stacks);
            }
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutput(VanillaTypes.ITEM, ((WrapperItemStack) recipe.packItem.getNewStack(null)).stack);
        }

        @Override
        public void setRecipe(IRecipeLayout recipeLayout, PackRecipeWrapper recipeWrapper, IIngredients ingredients) {
            //Get stack bits.
            IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

            //Set output.  (For some reason the position in the texture is off by 1px for JEI?
            guiItemStacks.init(0, false, 58, 70);
            guiItemStacks.set(0, ingredients.getOutputs(VanillaTypes.ITEM).get(0));

            //Set inputs.
            List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
            final int numRows = 3;
            final int numCols = 7;
            int xOffset;
            int yOffset;
            for (int i = 0; i < inputs.size() && i < numRows * numCols; ++i) {
                xOffset = 5 + 17 * (i % numCols);
                yOffset = 5 + 17 * (i / numCols);
                guiItemStacks.init(i + 1, true, xOffset, yOffset);
                guiItemStacks.set(i + 1, inputs.get(i));
            }
        }
    }
}
