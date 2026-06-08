package mcinterface1211;

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
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.packloading.PackParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
        return ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "main");
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
            this.background = guiHelper.createDrawable(ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "textures/guis/jei_crafting.png"), 0, 0, 134, 97);
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ((WrapperItemStack) benchItem.getNewStack(null)).stack);
        }

        @Override
        public RecipeType<PackRecipeWrapper> getRecipeType() {
            return RecipeType.create(InterfaceLoader.MODID, benchItem.getRegistrationName(), PackRecipeWrapper.class);
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
        public void setRecipe(IRecipeLayoutBuilder builder, PackRecipeWrapper recipe, IFocusGroup focuse) {
            final int numCols = 7;
            List<PackMaterialComponent> components = PackMaterialComponent.parseFromJSON(recipe.packItem, recipe.recipeIndex, true, true, recipe.forRepair, true);
            if (components != null) {
                int xOffset;
                int yOffset;
                int itemCount = 0;
                for (PackMaterialComponent component : components) {
                    List<ItemStack> stacks = new ArrayList<>();
                    for (IWrapperItemStack stack : component.possibleItems) {
                        stacks.add(((WrapperItemStack) stack).stack);
                    }
                    xOffset = 6 + 17 * (itemCount % numCols);
                    yOffset = 6 + 17 * (itemCount / numCols);
                    builder.addSlot(RecipeIngredientRole.INPUT, xOffset, yOffset).addItemStacks(stacks);
                    itemCount++;
                }
                builder.addSlot(RecipeIngredientRole.OUTPUT, 59, 71).addItemStack(((WrapperItemStack) recipe.packItem.getNewStack(null)).stack);
            }
        }
    }
}
