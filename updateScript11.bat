RMDIR /S /Q "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator"

XCOPY "C:\Users\chris\AppData\Roaming\.minecraft\forge2\src\main\java\minecrafttransportsimulator" "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" /E /K /O 

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "worldObj" --replace "world"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "thePlayer" --replace "player"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "theWorld" --replace "world"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "spawnEntityInWorld" --replace "spawnEntity"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "ItemStack.loadItemStackFromNBT(itemTag);" --replace "new ItemStack(itemTag);"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)" --replace "onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "isUseableByPlayer" --replace "isUsableByPlayer"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "displayAllRelevantItems(List<ItemStack> givenList)" --replace "displayAllRelevantItems(NonNullList<ItemStack> givenList)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "processInitialInteract(EntityPlayer player, @Nullable ItemStack stack, EnumHand hand)" --replace "processInitialInteract(EntityPlayer player, EnumHand hand)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "processInitialInteract(player, stack, hand)" --replace "processInitialInteract(player, hand)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems)" --replace "getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> subItems)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){" --replace "public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "world.getCollisionBoxes(" --replace "world.getCollisionBoxes(null, "

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "addCollisionBoxToList(world, pos, box, collidingAABBList, null)" --replace "addCollisionBoxToList(world, pos, box, collidingAABBList, null, false)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "stackSize" --replace "getCount()"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "EntityRegistry.registerModEntity(" --replace "EntityRegistry.registerModEntity(new ResourceLocation(MTS.MODID, entityClass.getSimpleName().substring(6).toLowerCase()), "

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "defaultStack = new ItemStack(this.getTabIconItem());" --replace "defaultStack = this.getTabIconItem();"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "public Item getTabIconItem()" --replace "public ItemStack getTabIconItem()"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator\dataclasses" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "return MTSRegistry.engineAircraftLarge;" --replace "return new ItemStack(MTSRegistry.engineAircraftLarge);"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator\entities\parts" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "((EntityPlayer) source.getEntity()).getHeldItemMainhand() == null" --replace "((EntityPlayer) source.getEntity()).getHeldItemMainhand().getItem().equals(Items.AIR)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand)" --replace "public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)"




