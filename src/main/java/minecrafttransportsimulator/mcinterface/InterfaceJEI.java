package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.packloading.PackMaterialComponent;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**Interface for the JEI system.  This is responsible for populating JEI with the various items,
 * benches, configurations and whatnots.
 *
 * @author don_bruce
 */
@JEIPlugin
public class InterfaceJEI implements IModPlugin{
	private static final List<BenchRecipeCategory> benchCategories = new ArrayList<BenchRecipeCategory>();

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry){
		//Check all pack items for benches.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			if(packItem instanceof ItemDecor){
				ItemDecor benchItem = (ItemDecor) packItem;
				if(benchItem.definition.decor.crafting != null){
					
					//For each bench, get all things they can craft, and add them as recipes.
					List<IRecipeWrapper> benchRecipes = new ArrayList<IRecipeWrapper>();
					for(AItemPack<?> packItemToTest : PackParserSystem.getAllPackItems()){
						if(packItemToTest.isBenchValid(benchItem.definition.decor.crafting)){
							benchRecipes.add(new PackRecipeWrapper(packItemToTest, false));
							if(packItemToTest.definition.general.repairMaterials != null){
								benchRecipes.add(new PackRecipeWrapper(packItemToTest, true));
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
	public void register(IModRegistry registry){
		//Register all recipes in all benches.
		for(BenchRecipeCategory benchCategory : benchCategories){
			registry.addRecipes(benchCategory.benchRecipes, benchCategory.getUid());
		}
	}
	
	private static class PackRecipeWrapper implements IRecipeWrapper{
		private final AItemPack<?> packItem;
		private final boolean forRepair;
		
		private PackRecipeWrapper(AItemPack<?> packItem, boolean forRepair){
			this.packItem = packItem;
			this.forRepair = forRepair;
		}
		
		@Override
		public void getIngredients(IIngredients ingredients){
			List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>();
			for(PackMaterialComponent component : PackMaterialComponent.parseFromJSON(packItem, true, true, false, forRepair)){
				List<ItemStack> stacks = new ArrayList<ItemStack>();
				for(WrapperItemStack stack : component.possibleItems){
					stacks.add(stack.stack);
				}
				inputs.add(stacks);
			}
			ingredients.setInputLists(VanillaTypes.ITEM, inputs);
			ingredients.setOutput(VanillaTypes.ITEM, packItem.getNewStack(null).stack);
		}
	}
	
	private static class BenchRecipeCategory implements IRecipeCategory<PackRecipeWrapper>{
		private final ItemDecor benchItem;
		private final List<IRecipeWrapper> benchRecipes;
		private final IDrawable background;
		private final IDrawable icon;
		
		private BenchRecipeCategory(ItemDecor benchItem, List<IRecipeWrapper> benchRecipes, IGuiHelper guiHelper){
			this.benchItem = benchItem;
			this.benchRecipes = benchRecipes;
			this.background = guiHelper.createDrawable(new ResourceLocation(MasterLoader.MODID, "textures/guis/jei_crafting.png"), 0, 0, 134, 97);
			this.icon = guiHelper.createDrawableIngredient(benchItem.getNewStack(null).stack);
		}

		@Override
		public String getUid(){
			return MasterLoader.MODID + "." + benchItem.getRegistrationName();
		}

		@Override
		public String getTitle(){
			return benchItem.getItemName();
		}

		@Override
		public String getModName(){
			return PackParserSystem.getPackConfiguration(benchItem.definition.packID).packName;
		}
		
		@Override
		public IDrawable getBackground(){
			return background;
		}

		@Override
		public IDrawable getIcon(){
			return icon;
		}

		@Override
		public void setRecipe(IRecipeLayout recipeLayout, PackRecipeWrapper recipeWrapper, IIngredients ingredients){
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
			for(int i=0; i<inputs.size() && i<numRows*numCols; ++i){
				xOffset = 5 + 17*(i%numCols);
				yOffset = 5 + 17*(i/numCols);
				guiItemStacks.init(i+1, true, xOffset, yOffset);
				guiItemStacks.set(i+1, inputs.get(i));
			}
		}
	}
}
