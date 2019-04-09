#!/bin/bash
SRCPATH="../../forgeMTS_11/src/main/java"
DSTPATH="../../forgeMTS_12/src/main/java"
VERPREFIX="1.12.2-"

rm -r $DSTPATH
cp -rPnu $SRCPATH $DSTPATH
echo "Refreshing files."
FILES=$(find $DSTPATH -not -type l -not -type d)

for FILE in ${FILES[*]}; do

echo "Checking file " $FILE

#Global variable and method name changes
sed -i 's/fontRendererObj/fontRenderer/g' $FILE
sed -i 's/playerEntity/player/g' $FILE
sed -i 's/source.getEntity()/source.getTrueSource()/g' $FILE
sed -i 's/source.getSourceOfDamage()/source.getImmediateSource()/g' $FILE
sed -i 's/player.getLastAttacker()/player.getAttackingEntity()/g' $FILE
sed -i 's/player.getDistanceToEntity(/player.getDistance(/g' $FILE
sed -i 's/intersectsWith/intersects/' $FILE
sed -i 's/isVecInside(/contains(/g' $FILE
sed -i 's/expandXyz(/grow(/g' $FILE
sed -i 's/\.xCoord/\.x/g' $FILE
sed -i 's/\.yCoord/\.y/g' $FILE
sed -i 's/\.zCoord/\.z/g' $FILE

#GUIs changed their names for x and y positions.  Don't change globally cause it might mess up other x-y systems.
#GUIs also added some odd parameter for partialTicks on buttons.  No earthly idea why you'd need that, so it gets a 0.
if echo $FILE | grep -q "GUI"; then
	sed -i 's/xPosition/x/g' $FILE
	sed -i 's/yPosition/y/g' $FILE
	sed -i 's/drawButton(mc, mouseX, mouseY)/drawButton(mc, mouseX, mouseY, 0)/g' $FILE
fi

#Items now use ITooltipFlag rather than a simple boolean.
if echo $FILE | grep -q "GUIPartBench"; then 
	sed -i 's/addInformation(tempStack, player, descriptiveLines, false)/addInformation(tempStack, player.world, descriptiveLines, ITooltipFlag.TooltipFlags.NORMAL)/' $FILE
	sed -i '3iimport net.minecraft.client.util.ITooltipFlag;' $FILE
fi

#Rendering brightness was changed to remove partialTicks parameter.  About time seeing as it hasn't been used since 1.7.10.
if echo $FILE | grep -q "RenderVehicle"; then
	sed -i 's/getBrightnessForRender(partialTicks)/getBrightnessForRender()/' $FILE
fi

#TESR systems now have an extra parameter.
if echo $FILE | grep -q "RenderFuelPump"; then
	sed -i 's/renderTileEntityAt(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(pump, x, y, z, partialTicks, destroyStage)/super.render(pump, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

if echo $FILE | grep -q "RenderDecor"; then
	sed -i 's/renderTileEntityAt(TileEntityDecor decor, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityDecor decor, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(decor, x, y, z, partialTicks, destroyStage)/super.render(decor, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

if echo $FILE | grep -q "RenderPoleLighted"; then
	sed -i 's/renderTileEntityAt(TileEntityPoleWallConnector polePart, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityPoleWallConnector polePart, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(polePart, x, y, z, partialTicks, destroyStage)/super.render(polePart, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

if echo $FILE | grep -q "RenderPoleSign"; then
	sed -i 's/renderTileEntityAt(TileEntityPoleSign sign, double x, double y, double z, float partialTicks, int destroyStage)/render(TileEntityPoleSign sign, double x, double y, double z, float partialTicks, int destroyStage, float alpha)/' $FILE
	sed -i 's/super.renderTileEntityAt(sign, x, y, z, partialTicks, destroyStage)/super.render(sign, x, y, z, partialTicks, destroyStage, alpha)/' $FILE
fi

if echo $FILE | grep -q "GUISign"; then
	sed -i 's/TileEntityRendererDispatcher.instance.renderTileEntityAt(decorTemp, -0.5F, -0.5F, -0.5F, renderPartialTicks)/TileEntityRendererDispatcher.instance.render(decorTemp, -0.5F, -0.5F, -0.5F, renderPartialTicks, 0)/' $FILE
fi

#Items had a few changes on their methods with respect to parameters.
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
if echo $FILE | grep -q "APartEngine"; then
	sed -i 's/getBiome(vehicle.getPosition()).getTemperature()/getBiome(vehicle.getPosition()).getTemperature(vehicle.getPosition())/' $FILE
fi

#Registration for crafting has a new ResourceLocation parameter as a UID.  Here's where we get to use craftingNumber.
if echo $FILE | grep -q "MTSRegistry"; then
	sed -i 's/GameRegistry.addShapedRecipe(output, params)/GameRegistry.addShapedRecipe(new ResourceLocation(MTS.MODID, output.getItem().getUnlocalizedName() + craftingNumber), null, output, params)/' $FILE
fi

#Block slipperiness was changed to be state-dependent.
if echo $FILE | grep -q "EntityVehicleD_Moving"; then
	sed -i 's/world.getBlockState(pos).getBlock().slipperiness/world.getBlockState(pos).getBlock().getSlipperiness(world.getBlockState(pos), world, pos, null)/' $FILE
	sed -i 's/world.getBlockState(pos).getBlock().slipperiness/world.getBlockState(pos).getBlock().getSlipperiness(world.getBlockState(pos), world, pos, null)/' $FILE
fi

if echo $FILE | grep -q "PartGroundDevice"; then
	sed -i 's/vehicle.world.getBlockState(pos).getBlock().slipperiness/vehicle.world.getBlockState(pos).getBlock().getSlipperiness(vehicle.world.getBlockState(pos), vehicle.world, pos, null)/' $FILE
fi

#VertexBuffer in Particle was changed to BufferBuilder.
if echo $FILE | grep -q "PartBullet"; then
	sed -i 's/VertexBuffer/BufferBuilder/' $FILE
	sed -i 's/setEntityBoundingBox(/setBoundingBox(/' $FILE
fi

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