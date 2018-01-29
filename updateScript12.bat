RMDIR /S /Q "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator"

XCOPY "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_11\src\main\java\minecrafttransportsimulator" "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" /E /K /O 

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltipLines, boolean p_77624_4_)" --replace "public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "public void getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> subItems)" --replace "public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "fontRendererObj" --replace "fontRenderer"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "playerEntity" --replace "player"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "drawButton(mc, mouseX, mouseY)" --replace "drawButton(mc, mouseX, mouseY, 0)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "expandXyz" --replace "grow"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "source.getEntity()" --replace "source.getTrueSource()"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "xPosition" --replace "x"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "yPosition" --replace "y"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "player.getLastAttacker()" --replace "player.getAttackingEntity()"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "player.getDistanceToEntity(this)" --replace "player.getDistance(this)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "getBrightnessForRender(partialTicks)" --replace "getBrightnessForRender()"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "renderTileEntityAt(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage)" --replace "render(TileEntityFuelPump pump, double x, double y, double z, float partialTicks, int destroyStage, float alpha)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "super.renderTileEntityAt(pump, x, y, z, partialTicks, destroyStage)" --replace "super.render(pump, x, y, z, partialTicks, destroyStage, alpha)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "renderTileEntityAt(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage)" --replace "render(TileEntityPropellerBench bench, double x, double y, double z, float partialTicks, int destroyStage, float alpha)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "super.renderTileEntityAt(bench, x, y, z, partialTicks, destroyStage)" --replace "super.render(bench, x, y, z, partialTicks, destroyStage, alpha)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "world.getBiome(new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ)).getTemperature()" --replace "world.getBiome(new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ)).getTemperature(new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ))"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "item.getSubItems(item, tab, givenList);" --replace "item.getSubItems(tab, givenList);"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "GameRegistry.addRecipe(output, params);" --replace "GameRegistry.addShapedRecipe(new ResourceLocation(MTS.MODID, output.getItem().getUnlocalizedName() + output.getItemDamage()), null, output, params);"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "world.getBlockState(grounder.getPosition().down()).getBlock().slipperiness" --replace "world.getBlockState(grounder.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(grounder.getPosition().down()), world, grounder.getPosition().down(), null)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "world.getBlockState(child.getPosition().down()).getBlock().slipperiness" --replace "world.getBlockState(child.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(child.getPosition().down()), world, child.getPosition().down(), null)"

"C:\Programs\Text Find&Replace\fnr.exe" --cl --dir "C:\Users\chris\AppData\Roaming\.minecraft\forgeMTS_12\src\main\java\minecrafttransportsimulator" --fileMask "*.java" --includeSubDirectories --caseSensitive --find "world.getBlockState(wheel.getPosition().down()).getBlock().slipperiness" --replace "world.getBlockState(wheel.getPosition().down()).getBlock().getSlipperiness(world.getBlockState(wheel.getPosition().down()), world, wheel.getPosition().down(), null)"


