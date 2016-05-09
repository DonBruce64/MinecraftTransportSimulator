package minecraftflightsimulator.planes.PZLP11;

import minecraftflightsimulator.containers.ContainerParent;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.containers.SlotBucket;
import minecraftflightsimulator.containers.SlotEngineLarge;
import minecraftflightsimulator.containers.SlotEngineSmall;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotInstrument;
import minecraftflightsimulator.containers.SlotPassenger;
import minecraftflightsimulator.containers.SlotPilot;
import minecraftflightsimulator.containers.SlotPropeller;
import minecraftflightsimulator.containers.SlotWheelLarge;
import minecraftflightsimulator.containers.SlotWheelSmall;
import minecraftflightsimulator.entities.EntityPlane;
import minecraftflightsimulator.helpers.InstrumentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityPZLP11 extends EntityPlane{
	private static final ResourceLocation foregroundGUI = new ResourceLocation("mfs", "textures/gui_mc172.png");
	private static final ResourceLocation backplateTexture = new ResourceLocation("mfs", "textures/trimotor_side.png");
	private static final ResourceLocation moldingTexture = new ResourceLocation("mfs", "textures/trimotor_side_rotated.png");
	
	public EntityPZLP11(World world){
		super(world);
	}
	
	public EntityPZLP11(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = false;
		taildragger = true;
		aileronIncrement = 2;
		elevatorIncrement = 6;
		rudderIncrement = 6;
		maxFuel = 5000;
		
		mass=1150;
		centerOfGravity=-1;
		momentRoll=1285;
		momentPitch=1825;
		momentYaw=2667;
		wingspan=11;
		
		//TODO get new properties
		wingArea=16;
		wingEfficiency=0.8F;
		tailDistance=7;
		thirdWheelDistance=5;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		maxLiftCoeff=2F;
		angleOfIncidence=0;
		defaultElevatorAngle=0F;
		criticalAoA=15;
		initialDragCoeff=0.03F;
		dragAtCriticalAoA=0.12F;
		dragCoeffOffset = (float) ((dragAtCriticalAoA - initialDragCoeff)/Math.pow(criticalAoA - 0, 2));		
	}
	
	@Override
	protected void initChildPositions(){
		//TODO set new positions
		addCenterWheelPosition(new float[]{0F, -0.5F, -5.5F});
		addLeftWheelPosition(new float[]{-1.4F, -1.8F, 0.375F});
		addRightWheelPosition(new float[]{1.4F, -1.8F, 0.375F});
		addEnginePosition(new float[]{0, -0.3F, 0.65F});
		addPropellerPosition(new float[]{0, -0.375F, 1.65F});
		addPilotPosition(new float[]{0, -.1F, -1.3F});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, -0.3F, 1}, {0, -0.35F, -5F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicHUD(this, width, height, backplateTexture, moldingTexture);
	}
	
	@Override
	public GUIParent getGUI(EntityPlayer player){
		//TODO make new GUI
		return new GUIParent(player, this, foregroundGUI);
	}
	
	@Override
	public void initParentContainerSlots(ContainerParent container){
		//TODO make new GUI
		container.addSlotToContainer(new SlotWheelSmall(this, 86, 113, 1));
		container.addSlotToContainer(new SlotWheelLarge(this, 50, 113, 2));
		container.addSlotToContainer(new SlotWheelLarge(this, 68, 113, 4));
		container.addSlotToContainer(new SlotEngineLarge(this, 131, 66, 6));
		container.addSlotToContainer(new SlotPropeller(this, 150, 66, 10));
		container.addSlotToContainer(new SlotPilot(this, 110, 66));
		container.addSlotToContainer(new SlotBucket(this, 7, 113));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotInstrument(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot));
		}
	}
}