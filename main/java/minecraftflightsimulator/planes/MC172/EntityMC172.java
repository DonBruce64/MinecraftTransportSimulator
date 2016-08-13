package minecraftflightsimulator.planes.MC172;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.containers.ContainerVehicle;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotItem;
import minecraftflightsimulator.containers.SlotPassenger;
import minecraftflightsimulator.containers.SlotPilot;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityMC172 extends EntityPlane{
	private static final ResourceLocation[] backplateTextures = getBackplateTextures();
	private static final ResourceLocation[] mouldingTextures = getMouldingTextures();
	
	public EntityMC172(World world){
		super(world);
	}
	
	public EntityMC172(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "MFS";
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = true;
		maxFuel = 5000;
		emptyMass=800;
		wingspan=11.0F;
		wingArea=16.0F;
		tailDistance=7;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		defaultElevatorAngle=-5F;
	}
	
	@Override
	protected void initChildPositions(){
		addCenterGearPosition(new float[]{0, -1F, 1.7F});
		addLeftGearPosition(new float[]{-1.65F, -1F, 0});
		addRightGearPosition(new float[]{1.65F, -1F, 0});
		addEnginePosition(new float[]{0, -0.3F, 1.65F});
		addPropellerPosition(new float[]{0, -0.375F, 2.5F});
		addControllerPosition(new float[]{0, -.1F, 0});
		addPassengerPosition(new float[]{0, -.1F, -1});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{0, -0.3F, 1.5F, 1, 1}, 
			{0, -0.3F, -2.75F, 1, 1}, 
			{0, -0.3F, -4.25F, 1, 1}, 
			{-1.5F, 0.5F, -4.25F, 1, 0.125F}, 
			{1.5F, 0.5F, -4.25F, 1, 0.125F},
			{0F, 0.5F, -4.25F, 0.25F, 1.5F}, 
			{0F, 1.6F, 0.25F, 1.25F, 0.125F},
			{-1.25F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{1.25F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{-2.5F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{2.5F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{-3.75F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{3.75F, 1.6F, 0.25F, 1.25F, 0.125F}
		};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicFlyableHUD(this, width, height, backplateTextures[this.textureOptions], mouldingTextures[this.textureOptions]);
	}
	
	@Override
	public void initVehicleContainerSlots(ContainerVehicle container){
		container.addSlotToContainer(new SlotItem(this, 86, 113, 1, MFSRegistry.wheelSmall));
		container.addSlotToContainer(new SlotItem(this, 50, 113, 2, MFSRegistry.wheelSmall, MFSRegistry.pontoon));
		container.addSlotToContainer(new SlotItem(this, 68, 113, 4, MFSRegistry.wheelSmall, MFSRegistry.pontoon));
		container.addSlotToContainer(new SlotItem(this, 131, 66, 6, MFSRegistry.engineSmall));
		container.addSlotToContainer(new SlotItem(this, 150, 66, 10, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotPilot(this, 110, 66));
		container.addSlotToContainer(new SlotPassenger(this, 92, 66));
		container.addSlotToContainer(new SlotItem(this, 7, 113, this.emptyBucketSlot));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotItem(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot, MFSRegistry.flightInstrument));
		}
	}
	
	private static ResourceLocation[] getMouldingTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_big_oak.png");
		return texArray;
	}
	
	private static ResourceLocation[] getBackplateTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}
}