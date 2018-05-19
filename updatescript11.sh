#!/bin/bash
SRCPATH="main/java"
DSTPATH="../../forgeMTS_11/src/main/java"
VERPREFIX="1.11.2-"

rm -r $DSTPATH
cp -rPnu $SRCPATH $DSTPATH
echo "Refreshing files."
FILES=$(find $DSTPATH -not -type l -not -type d)

for FILE in ${FILES[*]}; do

echo "Checking file " $FILE

#Global variable and method name changes
sed -i 's/worldObj/world/g' $FILE
sed -i 's/thePlayer/player/g' $FILE
sed -i 's/theWorld/world/g' $FILE
sed -i 's/spawnEntityInWorld/spawnEntity/g' $FILE
sed -i 's/isUseableByPlayer/isUsableByPlayer/g' $FILE

#ItemStack was given a constructor rather than a helper method to create a stack from a NBT tag.
sed -i 's/ItemStack.loadItemStackFromNBT(itemTag)/new ItemStack(itemTag)/' $FILE

#ItemStack was removed as a parameter in interaction methods.  Use hand-based getters now.
sed -i 's/onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)/onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)/' $FILE
sed -i 's/processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand)/processInitialInteract(EntityPlayer player, EnumHand hand)/' $FILE
sed -i 's/processInitialInteract(player, stack, hand)/processInitialInteract(player, hand)/' $FILE
sed -i 's/onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)/onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)/' $FILE
sed -i 's/onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)/onItemRightClick(World world, EntityPlayer player, EnumHand hand)/' $FILE

#Creative tabs were changed to take an ItemStack as their icon rather than an Item.
if echo $FILE | grep -q "MTSCreativeTabs"; then
	sed -i 's/displayAllRelevantItems(List<ItemStack> givenList)/displayAllRelevantItems(NonNullList<ItemStack> givenList)/' $FILE
	sed -i 's/public Item getTabIconItem()/public ItemStack getTabIconItem()/' $FILE
	sed -i 's/defaultStack = new ItemStack(this.getTabIconItem())/defaultStack = this.getTabIconItem()/' $FILE
	sed -i 's/return MTSRegistry.wrench/return new ItemStack(MTSRegistry.wrench)/' $FILE
fi

#Collision methods got a few extra parameters.
sed -i 's/world.getCollisionBoxes(/world.getCollisionBoxes(null, /' $FILE
sed -i 's/addCollisionBoxToList(world, pos, box, collidingAABBList, null)/addCollisionBoxToList(world, pos, box, collidingAABBList, null, false)/' $FILE

#Blank Item slots are no longer null; they're AIR.  Check for that when players hit propellers to start them.
if echo $FILE | grep -q "EntityPropeller"; then
	sed -i 's/((EntityPlayer) source.getEntity()).getHeldItemMainhand() == null/((EntityPlayer) source.getEntity()).getHeldItemMainhand().getItem().equals(Items.AIR)/' $FILE
	sed -i '3iimport net.minecraft.init.Items;' $FILE;
fi

#Fluid handler was split into regular and item-based.  Change fuel pump to reflect this.
if echo $FILE | grep -q "BlockFuelPump"; then
	sed -i 's/FLUID_HANDLER_CAPABILITY/FLUID_HANDLER_ITEM_CAPABILITY/' $FILE
	sed -i 's/IFluidHandler/IFluidHandlerItem/' $FILE
	sed -i '/pump.fill(drainedStack, true);/a \\t\t\t\t\t\t\tplayer.setHeldItem(hand, handler.getContainer());' $FILE
fi

#Item method getSubItems was changed to use NonNullList.
if echo $FILE | grep -q "Item"; then
	sed -i 's/getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems)/getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> subItems)/' $FILE
fi

#Items stackSize variable is no longer visible.  Needs to be switched to getter getCount().
sed -i 's/stackSize/getCount()/g' $FILE

#Entity registration now requires a ResourceLocation for stuff.  Why we need that and still need a unique name is beyond me...
if echo $FILE | grep -q "MTSRegistry"; then
	sed -i 's/EntityRegistry.registerModEntity(/EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, entityClass.getSimpleName().substring(6).toLowerCase()), /' $FILE
	sed -i '3iimport net.minecraft.util.ResourceLocation;' $FILE;
fi

#Need to convert the inventory system on the chests to use NonNullList and ItemStack.EMPTY rather than an array and nulls.
if echo $FILE | grep -q "EntityChildInventory"; then
	sed -i 's/ItemStack\[\] contents = new ItemStack\[27\]/NonNullList<ItemStack> contents = NonNullList.<ItemStack>withSize(27, ItemStack.EMPTY)/' $FILE
	sed -i 's/this.contents\[slot\] = null;/this.contents.set(slot, ItemStack.EMPTY);/g' $FILE
	sed -i 's/this.contents\[slot\] = stack;/this.contents.set(slot, stack);/g' $FILE
	sed -i 's/this.contents\[slot\] = new ItemStack(itemTag)/this.contents.set(slot, new ItemStack(itemTag))/' $FILE
	sed -i 's/this.contents\[slot\]/this.contents.get(slot)/g' $FILE
	sed -i '/this.contents = new ItemStack\[27\];/d' $FILE
fi

#Need to import NonNullList anywhere we use it as it's new to 1.11.
if grep -q "NonNullList" $FILE; then $(sed -i '3iimport net.minecraft.util.NonNullList;' $FILE); fi

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

#Finally, build the mod.'
cd $DSTPATH/../../../
./gradlew build --offline
cd ../forge2/src