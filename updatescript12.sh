#!/bin/bash
SRCPATH="../../forgeMTS_11/src/main/java"
DSTPATH="../../forgeMTS_12/src/main/java"
VERPREFIX="1.12.0-"

rm -r $DSTPATH
cp -rPnu $SRCPATH $DSTPATH
echo "Refreshing files."
FILES=$(find $DSTPATH -not -type l -not -type d)

for FILE in ${FILES[*]}; do

#Don't check partmodels or blockmodels.  Those don't change.
if echo $FILE | grep -q "partmodels"; then
	continue
fi
if echo $FILE | grep -q "blockmodels"; then
	continue
fi

#Minecraft replaced Acheivements with Advancements in 1.12. Delete the Achivement file and skip
if echo $FILE | grep -q "MTSAchievements"; then
	rm $FILE
	continue;
fi

echo "Checking file " $FILE

#Global variable and method name changes
sed -i 's/fontRendererObj/fontRenderer/g' $FILE
sed -i 's/playerEntity/player/g' $FILE
sed -i 's/expandXyz/grow/g' $FILE
sed -i 's/source.getEntity()/source.getTrueSource()/g' $FILE
sed -i 's/player.getLastAttacker()/player.getAttackingEntity()/g' $FILE
sed -i 's/player.getDistanceToEntity(/player.getDistance(/g' $FILE

#GUIs changed their names for x and y positions.  Don't change globally cause it might mess up other x-y systems.
#GUIs also added some odd parameter for partialTicks on buttons.  No earthly idea why you'd need that, so it gets a 0.
if echo $FILE | grep -q "GUI"; then
	sed -i 's/xPosition/x/g' $FILE
	sed -i 's/yPosition/y/g' $FILE
	sed -i 's/drawButton(mc, mouseX, mouseY)/drawButton(mc, mouseX, mouseY, 0)/g' $FILE
fi

#Rendering brightness was changed to remove partialTicks parameter.  About time seeing as it hasn't been used since 1.7.10.
if echo $FILE | grep -q "RenderMultipart"; then
	sed -i 's/getBrightnessForRender(partialTicks)/getBrightnessForRender()/' $FILE
fi

#TESR systems now have an extra parameter.
if echo $FILE | grep -q "RenderFuelPump"; then
	sed -i 's/renderTileEntityAt(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(pump, x, y, z, partialTicks, destroyStage)/super.render(pump, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

if echo $FILE | grep -q "RenderPropellerBench"; then
	sed -i 's/renderTileEntityAt(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(bench, x, y, z, partialTicks, destroyStage)/super.render(bench, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

#Items had a few changes on their methods WRT parameters.
if echo $FILE | grep -q "Item"; then
	sed -i 's/getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> subItems)/getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems)/' $FILE
	sed -i 's/addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_)/addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn)/' $FILE
	if cat $FILE | grep -q "Nullable"; then
		sed -i '3iimport javax.annotation.Nullable;' $FILE
		sed -i '3iimport net.minecraft.world.World;' $FILE
		sed -i '3iimport net.minecraft.client.util.ITooltipFlag;' $FILE
	fi
fi

#Need to change this method globally to match change above.
sed -i 's/item.getSubItems(item, tab, givenList)/item.getSubItems(tab, givenList)/g' $FILE

#Temperature was changed to be position-dependent.
sed -i 's/getBiome(this.getPosition()).getTemperature()/getBiome(this.getPosition()).getTemperature(this.getPosition())/' $FILE

#Registration for crafting has a new ResourceLocation parameter as a UID.  Here's where we get to use craftingNumber.
if echo $FILE | grep -q "MTSRegistry"; then
	sed -i 's/GameRegistry.addShapedRecipe(output, params)/GameRegistry.addShapedRecipe(new ResourceLocation(MTS.MODID, output.getItem().getUnlocalizedName() + craftingNumber), null, output, params)/' $FILE
fi

#Block slipperiness was changed to be state-dependent.
if echo $FILE | grep -q "EntityMultipartMoving"; then
	sed -i 's/world.getBlockState(grounder.getPosition().down()).getBlock().slipperiness/world.getBlockState(grounder.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(grounder.getPosition().down()), world, grounder.getPosition().down(), null)/' $FILE
	sed -i 's/world.getBlockState(child.getPosition().down()).getBlock().slipperiness/world.getBlockState(child.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(child.getPosition().down()), world, child.getPosition().down(), null)/' $FILE
fi

if echo $FILE | grep -q "EntityEngineCar"; then
	sed -i 's/world.getBlockState(wheel.getPosition().down()).getBlock().slipperiness/world.getBlockState(wheel.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(wheel.getPosition().down()), world, wheel.getPosition().down(), null)/' $FILE
fi

#Remove possible references to the old Acheivement system.
sed -i '/MTSAchievements/d' $FILE

done

#Now that we are done with changes, update build number in build.gradle and run buildscript.
#First get the string of line with the mod version number.
VERSTRING=$(cat $DSTPATH/minecrafttransportsimulator/MTS.java | grep "MODVER")
#Now parse out the part right after MODVER.
VERSTRING=${VERSTRING##*MODVER}
#Now isolate the version.
VERSTRING=${VERSTRING:2:${#VERSTRING}-4}

#Now that we have the version we need to inject it into the build.gradle file.
echo "Configuring build.gradle for $VERPREFIX$VERSTRING"
sed -i '13s/.*version.*/version = "'$VERPREFIX$VERSTRING'"/' $DSTPATH/../../../build.gradle

#Finally, build the mod.
cd $DSTPATH/../../../
./gradlew build --offline
cd ../forge2/src