package minecraftflightsimulator.planes.MC172;

import minecraftflightsimulator.MFS;
import minecraftflightsimulator.containers.ContainerParent;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotItem;
import minecraftflightsimulator.containers.SlotPassenger;
import minecraftflightsimulator.containers.SlotPilot;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityMC172 extends EntityPlane{
	private static final ResourceLocation foregroundGUI = new ResourceLocation("mfs", "textures/planes/mc172/gui.png");
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
		momentRoll=1285;
		momentPitch=1825;
		momentYaw=2667;
		wingspan=11;
		wingArea=16;
		tailDistance=7;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		defaultElevatorAngle=-5;
		initialDragCoeff=0.03F;
		dragAtCriticalAoA=0.12F;
		dragCoeffOffset = (float) ((dragAtCriticalAoA - initialDragCoeff)/Math.pow(15, 2));		
	}
	
	@Override
	protected void initChildPositions(){
		addCenterGearPosition(new float[]{0, -1F, 1.7F});
		addLeftGearPosition(new float[]{-1.65F, -1F, 0});
		addRightGearPosition(new float[]{1.65F, -1F, 0});
		addEnginePosition(new float[]{0, -0.3F, 1.65F});
		addPropellerPosition(new float[]{0, -0.375F, 2.5F});
		addPilotPosition(new float[]{0, -.1F, 0});
		addPassengerPosition(new float[]{0, -.1F, -1});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, -0.3F, 1}, {0, -0.3F, -4.25F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicHUD(this, width, height, backplateTextures[this.textureOptions], mouldingTextures[this.textureOptions]);
	}
	
	@Override
	public GUIParent getGUI(EntityPlayer player){
		return new GUIParent(player, this, foregroundGUI);
	}
	
	@Override
	public void initParentContainerSlots(ContainerParent container){
		container.addSlotToContainer(new SlotItem(this, 86, 113, 1, MFS.proxy.wheelSmall));
		container.addSlotToContainer(new SlotItem(this, 50, 113, 2, MFS.proxy.wheelSmall, MFS.proxy.pontoon));
		container.addSlotToContainer(new SlotItem(this, 68, 113, 4, MFS.proxy.wheelSmall, MFS.proxy.pontoon));
		container.addSlotToContainer(new SlotItem(this, 131, 66, 6, MFS.proxy.engineSmall));
		container.addSlotToContainer(new SlotItem(this, 150, 66, 10, MFS.proxy.propeller));
		container.addSlotToContainer(new SlotPilot(this, 110, 66));
		container.addSlotToContainer(new SlotPassenger(this, 92, 66));
		container.addSlotToContainer(new SlotItem(this, 7, 113, this.emptyBucketSlot));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotItem(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot, MFS.proxy.flightInstrument));
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