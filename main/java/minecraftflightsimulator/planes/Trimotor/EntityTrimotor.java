package minecraftflightsimulator.planes.Trimotor;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.containers.ContainerVehicle;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotItem;
import minecraftflightsimulator.containers.SlotLoadable;
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
		numberLights = 3;
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
		addMixedPosition(new float[]{0.625F, -0.7F, 0.5F});
		addMixedPosition(new float[]{-0.625F, -0.7F, 0.5F});
		addMixedPosition(new float[]{0.625F, -0.7F, -0.6F});
		addMixedPosition(new float[]{-0.625F, -0.7F, -0.6F});
		addMixedPosition(new float[]{0.625F, -0.7F, -1.6F});
		addMixedPosition(new float[]{-0.625F, -0.7F, -1.6F});
		addMixedPosition(new float[]{0.625F, -0.7F, -2.6F});
		addMixedPosition(new float[]{-0.625F, -0.7F, -2.6F});
		addMixedPosition(new float[]{0.625F, -0.7F, -3.6F});
		addMixedPosition(new float[]{-0.625F, -0.7F, -3.6F});
		addMixedPosition(new float[]{0.625F, -0.7F, -4.6F});
		addMixedPosition(new float[]{-0.625F, -0.7F, -4.6F});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{0, -0.5F, 2.75F, 1, 1},
			{-3F, -0.5F, 0F, 1, 1},
			{3F, -0.5F, 0F, 1, 1}, 
			{0, 1.375F, -0.75F, 3.5F, 0.125F},
			{-2.5F, 1.375F, -0.75F, 3.25F, 0.125F},
			{2.5F, 1.375F, -0.75F, 3.25F, 0.125F},
			{-4.5F, 1.375F, -0.75F, 3.0F, 0.125F},
			{4.5F, 1.375F, -0.75F, 3.0F, 0.125F},
			{-6.5F, 1.375F, -0.75F, 2.75F, 0.125F},
			{6.5F, 1.375F, -0.75F, 2.75F, 0.125F},
			{-8.5F, 1.375F, -0.75F, 2.5F, 0.125F},
			{8.5F, 1.375F, -0.75F, 2.5F, 0.125F},
			{-9.5F, 1.375F, -0.75F, 2.25F, 0.125F},
			{9.5F, 1.375F, -0.75F, 2.25F, 0.125F},
			{0, -0.7F, 2F, 1.5F, 0.125F},
			{0, -1F, -0.25F, 1.75F, 0.125F},
			{0, -1F, -2.125F, 1.75F, 0.125F},
			{0, -1F, -4F, 1.75F, 0.125F},
			{0, 1.375F, -4F, 1.5F, 0.125F},
			{0, -0.75F, -6F, 1.25F, 2.5F},
			{0, -0.5F, -7.5F, 1.25F, 2.0F},
			{0, 0F, -8.5F, 1F, 1.5F},
			{0, 0.5F, -9.5F, 0.75F, 1F},
			{-0.75F, 1.2F, -10.5F, 1.5F, 0.125F},
			{0.75F, 1.2F, -10.5F, 1.5F, 0.125F},
			{-2.5F, 1.2F, -10.5F, 1.5F, 0.125F},
			{2.5F, 1.2F, -10.5F, 1.5F, 0.125F},
			{0F, 1.2F, -10.5F, 0.25F, 1.25F},
			{0F, 1.2F, -11F, 0.25F, 1.25F}
		};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicFlyableHUD(this, width, height, backplateTexture, moldingTexture);
	}
	
	@Override
	public void initVehicleContainerSlots(ContainerVehicle container){
		container.addSlotToContainer(new SlotItem(this, 62, 113, 1, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 80, 113, 2, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 44, 113, 4, MFSRegistry.wheelLarge));
		container.addSlotToContainer(new SlotItem(this, 131, 63, 6, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 118, 37, 7, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 118, 89, 8, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 150, 63, 10, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotItem(this, 137, 37, 11, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotItem(this, 137, 89, 12, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotLoadable(this, 107, 63, SlotLoadable.SeatTypes.CONTROLLER));
		container.addSlotToContainer(new SlotLoadable(this, 89, 63, SlotLoadable.SeatTypes.FORWARD_MIXED));
		container.addSlotToContainer(new SlotLoadable(this, 71, 63, SlotLoadable.SeatTypes.AFT_MIXED));
		container.addSlotToContainer(new SlotItem(this, 7, 113, this.emptyBucketSlot));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(int i=0; i<10; ++i){
			container.addSlotToContainer(new SlotItem(this, 7 + 18*(i%5), i < 5 ? 7 : 25, i + instrumentStartSlot, MFSRegistry.flightInstrument));
		}
	}
}