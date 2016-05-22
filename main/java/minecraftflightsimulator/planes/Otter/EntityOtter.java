package minecraftflightsimulator.planes.Otter;

import minecraftflightsimulator.containers.ContainerParent;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.containers.SlotBucket;
import minecraftflightsimulator.containers.SlotEngineSmall;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotInstrument;
import minecraftflightsimulator.containers.SlotPassenger;
import minecraftflightsimulator.containers.SlotPilot;
import minecraftflightsimulator.containers.SlotPropeller;
import minecraftflightsimulator.containers.SlotWheelLarge;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityOtter extends EntityPlane{
	private static final ResourceLocation foregroundGUI = new ResourceLocation("mfs", "textures/planes/mc172/gui.png");
	private static final ResourceLocation backplateTexture = new ResourceLocation("minecraft", "textures/blocks/stone.png");
	private static final ResourceLocation moldingTexture = new ResourceLocation("minecraft", "textures/blocks/stone.png");
	
	public EntityOtter(World world){
		super(world);
	}
	
	public EntityOtter(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = true;
		maxFuel = 15000;
		emptyMass=3200;
		momentRoll=21259;
		momentPitch=39972;
		momentYaw=44944;
		wingspan=20;
		wingArea=39;
		tailDistance=10;
		rudderArea=6F;
		elevatorArea=6F;
		defaultElevatorAngle=-5;
		initialDragCoeff=0.03F;
		dragAtCriticalAoA=0.12F;
		dragCoeffOffset = (float) ((dragAtCriticalAoA - initialDragCoeff)/Math.pow(15 - 0, 2));//		
	}
	
	@Override
	protected void initChildPositions(){
		addCenterWheelPosition(new float[]{0, -0.5F, 4.4F});
		addLeftWheelPosition(new float[]{-2F, -0.45F, 0});
		addRightWheelPosition(new float[]{2F, -0.45F, 0});		
		addEnginePosition(new float[]{-2.975F, 1.7F, 1.91F});
		addEnginePosition(new float[]{2.975F, 1.7F, 1.91F});
		addPropellerPosition(new float[]{-2.975F, 1.6F, 2.7F});
		addPropellerPosition(new float[]{2.975F, 1.6F, 2.7F});
		addPilotPosition(new float[]{0.45F, 0.9F, 3.35F});
		addPilotPosition(new float[]{-0.45F, 0.9F, 3.35F});
		for(byte i=0; i<5; ++i){
			addPassengerPosition(new float[]{0.45F, 0.8F, 1.82F-i});
			addPassengerPosition(new float[]{-0.45F, 0.8F, 1.82F-i});
		}
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, 0.25F, 4}, {0, 0.25F, -4.25F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		//TODO get new HUD
		InstrumentHelper.drawBasicHUD(this, width, height, backplateTexture, moldingTexture);
	}
	
	@Override
	public GUIParent getGUI(EntityPlayer player){
		return new GUIParent(player, this, foregroundGUI);
	}
	
	@Override
	public void initParentContainerSlots(ContainerParent container){
		//TODO make new GUI
		container.addSlotToContainer(new SlotWheelLarge(this, 86, 113, 1));
		container.addSlotToContainer(new SlotWheelLarge(this, 50, 113, 2));
		container.addSlotToContainer(new SlotWheelLarge(this, 68, 113, 4));
		
		container.addSlotToContainer(new SlotEngineSmall(this, 131, 66, 6));
		container.addSlotToContainer(new SlotEngineSmall(this, 131, 82, 7));
		
		container.addSlotToContainer(new SlotPropeller(this, 150, 66, 10));
		container.addSlotToContainer(new SlotPropeller(this, 150, 82, 11));
		
		container.addSlotToContainer(new SlotPilot(this, 110, 66));
		container.addSlotToContainer(new SlotPassenger(this, 92, 66));
		container.addSlotToContainer(new SlotBucket(this, 7, 113));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotInstrument(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot));
		}
	}
}