package minecraftflightsimulator.planes.PZLP11;

import minecraftflightsimulator.MFSRegistry;
import minecraftflightsimulator.containers.ContainerVehicle;
import minecraftflightsimulator.containers.GUIParent;
import minecraftflightsimulator.containers.SlotFuel;
import minecraftflightsimulator.containers.SlotItem;
import minecraftflightsimulator.containers.SlotPilot;
import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityPZLP11 extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/hud_backplate.png");
	private static final ResourceLocation moldingTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/hud_moulding.png");
	
	public EntityPZLP11(World world){
		super(world);
	}
	
	public EntityPZLP11(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "MAX 4H70 M";
	}

	@Override
	protected void initPlaneProperties(){
		hasFlaps = false;
		maxFuel = 7000;		
		emptyMass=1150;
		wingspan=11.0F;
		wingArea=18.0F;
		tailDistance=7;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		defaultElevatorAngle=-10F;
	}
	
	@Override
	protected void initChildPositions(){
		addCenterGearPosition(new float[]{0F, -0.3F, -5.25F});
		addLeftGearPosition(new float[]{-1.4F, -1.8F, 0.375F});
		addRightGearPosition(new float[]{1.4F, -1.8F, 0.375F});
		addEnginePosition(new float[]{0, -0.3F, 0.65F});
		addPropellerPosition(new float[]{0, -0.375F, 1.65F});
		addControllerPosition(new float[]{0, -.1F, -1.3F});
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{{0, -0.3F, 1}, {0, 0F, -5F}};
	}
	
	@Override
	public void drawHUD(int width, int height){
		InstrumentHelper.drawBasicFlyableHUD(this, width, height, backplateTexture, moldingTexture);
	}
	
	@Override
	public void initVehicleContainerSlots(ContainerVehicle container){
		container.addSlotToContainer(new SlotItem(this, 86, 113, 2, MFSRegistry.wheelLarge, MFSRegistry.pontoon));
		container.addSlotToContainer(new SlotItem(this, 68, 113, 1, MFSRegistry.skid));
		container.addSlotToContainer(new SlotItem(this, 50, 113, 4, MFSRegistry.wheelLarge, MFSRegistry.pontoon));
		container.addSlotToContainer(new SlotItem(this, 131, 62, 6, MFSRegistry.engineLarge));
		container.addSlotToContainer(new SlotItem(this, 149, 62, 10, MFSRegistry.propeller));
		container.addSlotToContainer(new SlotPilot(this, 113, 62));
		container.addSlotToContainer(new SlotItem(this, 7, 113, this.emptyBucketSlot));
		container.addSlotToContainer(new SlotFuel(this, 7, 73));
		for(byte i=0; i<6; ++i){
			container.addSlotToContainer(new SlotItem(this, 7 + 18*(i%3), i < 3 ? 7 : 25, (i < 3 ? i + 1 : i + 3 ) + instrumentStartSlot, MFSRegistry.flightInstrument));
		}
	}
}