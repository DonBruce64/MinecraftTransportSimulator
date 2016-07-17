package minecraftflightsimulator.planes.Vulcanair;

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

public class EntityVulcanair extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation("textures/blocks/wool_colored_white.png");
	private static final ResourceLocation[] mouldingTextures = getMouldingTextures();
	
	public EntityVulcanair(World world){
		super(world);
	}
	
	public EntityVulcanair(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "Wolfvanox";
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = true;
		maxFuel = 7000;
		emptyMass=1230;
		momentRoll=1845;
		momentPitch=2000;
		momentYaw=2667;
		wingspan=12.0F;
		wingArea=18.6F;
		tailDistance=7;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		defaultElevatorAngle=-10F;
	}
	
	@Override
	protected void initChildPositions(){
		addCenterGearPosition(new float[]{0, -0.75F, 4.62F});
		addLeftGearPosition(new float[]{-1.75F, -0.75F, 0});
		addRightGearPosition(new float[]{1.75F, -0.75F, 0F});
		addEnginePosition(new float[]{2.69F, 1.25F, 1.17F});
		addEnginePosition(new float[]{-2.69F, 1.25F, 1.17F});
		addPropellerPosition(new float[]{2.69F, 1.15F, 2.07F});
		addPropellerPosition(new float[]{-2.69F, 1.15F, 2.07F});
		addPilotPosition(new float[]{0, 0.1F, 2.37F});
		addPassengerPosition(new float[]{0, 0.1F, 1.245F});
		addPassengerPosition(new float[]{0, 0.1F, 0.12F});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, -0.3F, 4F}, {0, -0.3F, 0}, {0, 1.0F, -4.5F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicHUD(this, width, height, backplateTexture, mouldingTextures[this.textureOptions]);
	}
	
	@Override
	public void initParentContainerSlots(ContainerParent container){
		container.addSlotToContainer(new SlotItem(this, 86, 113, 1, MFS.proxy.wheelSmall));
		container.addSlotToContainer(new SlotItem(this, 50, 113, 2, MFS.proxy.wheelSmall, MFS.proxy.pontoon));
		container.addSlotToContainer(new SlotItem(this, 68, 113, 4, MFS.proxy.wheelSmall, MFS.proxy.pontoon));
		container.addSlotToContainer(new SlotItem(this, 111, 40, 6, MFS.proxy.engineSmall));
		container.addSlotToContainer(new SlotItem(this, 111, 84, 7, MFS.proxy.engineSmall));
		container.addSlotToContainer(new SlotItem(this, 130, 40, 10, MFS.proxy.propeller));
		container.addSlotToContainer(new SlotItem(this, 130, 84, 11, MFS.proxy.propeller));
		container.addSlotToContainer(new SlotPilot(this, 118, 62));
		container.addSlotToContainer(new SlotPassenger(this, 101, 62));
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
}