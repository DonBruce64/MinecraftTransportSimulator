package minecraftflightsimulator.planes.Trimotor;

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

public class EntityTrimotor extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation("mfs", "textures/planes/trimotor/side.png");
	private static final ResourceLocation moldingTexture = new ResourceLocation("mfs", "textures/planes/trimotor/side_rotated.png");
	
	public EntityTrimotor(World world){
		super(world);
	}
	
	public EntityTrimotor(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "DB32-1204-UL";
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = false;
		maxFuel = 25000;
		emptyMass=3000;
		wingspan=22.6F;
		wingArea=70.0F;
		tailDistance=12;
		rudderArea=5.0F;
		elevatorArea=7.5F;
		defaultElevatorAngle=-10;
	}
	
	@Override
	protected void initChildPositions(){
		addCenterGearPosition(new float[]{0, 0F, -11F});
		addLeftGearPosition(new float[]{-2.875F, -2.3F, 0.3F});
		addRightGearPosition(new float[]{2.875F, -2.3F, 0.3F});
		addEnginePosition(new float[]{0, -0.46F, 3.06F});
		addEnginePosition(new float[]{2.875F, -0.46F, 0.36F});
		addEnginePosition(new float[]{-2.875F, -0.46F, 0.36F});
		addPropellerPosition(new float[]{0, -0.535F, 4.06F});
		addPropellerPosition(new float[]{2.875F, -0.535F, 1.36F});
		addPropellerPosition(new float[]{-2.875F, -0.535F, 1.36F});
		addControllerPosition(new float[]{0.5F, -.3F, 1.55F});
		addControllerPosition(new float[]{-0.5F, -.3F, 1.55F});
		addPassengerPosition(new float[]{0.625F, -0.7F, 0.5F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, 0.5F});
		addPassengerPosition(new float[]{0.625F, -0.7F, -0.6F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, -0.6F});
		addPassengerPosition(new float[]{0.625F, -0.7F, -1.6F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, -1.6F});
		addPassengerPosition(new float[]{0.625F, -0.7F, -2.6F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, -2.6F});
		addPassengerPosition(new float[]{0.625F, -0.7F, -3.6F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, -3.6F});
		addPassengerPosition(new float[]{0.625F, -0.7F, -4.6F});
		addPassengerPosition(new float[]{-0.625F, -0.7F, -4.6F});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, -1F, 1F}, {0, -1F, -5F}, {0, 0.5F, -2.5F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicFlyableHUD(this, width, height, backplateTexture, moldingTexture);
	}
	
	@Override
	public void initVehicleContainerSlots(ContainerVehicle container){
		container.addSlotToContainer(new SlotItem(this, 80, 113, 1, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 44, 113, 2, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 62, 113, 4, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 131, 63, 6, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 118, 37, 7, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 118, 89, 8, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 150, 63, 10, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotItem(this, 137, 37, 11, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotItem(this, 137, 89, 12, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotPilot(this, 107, 63));
		container.addSlotToContainer(new SlotPassenger(this, 89, 63));
		container.addSlotToContainer(new SlotItem(this, 7, 113, this.emptyBucketSlot));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotItem(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot, MFSRegistry.flightInstrument));
		}
	}
}