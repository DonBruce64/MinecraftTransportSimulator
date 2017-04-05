package minecrafttransportsimulator.planes.Trimotor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.VehicleHUDs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityTrimotor extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side.png");
	private static final ResourceLocation mouldingTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side_rotated.png");
	
	public EntityTrimotor(World world){
		super(world);
	}
	
	public EntityTrimotor(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "DB32-1204-UL";
	}

	@Override
	protected void initProperties(){
		lightSetup = 13;
		numberPowerfulLights = 2;
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
	protected void initProhibitedInstruments(){}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0, 0F, -10.5F, true, false, -1, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(-2.875F, -2.3F, 0.8F, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(2.875F, -2.3F, 0.8F, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(2.875F, -0.46F, 0.86F, false, false, 1, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(0, -0.46F, 3.56F, false, false, 1, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(-2.875F, -0.46F, 0.86F, false, false, 1, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(0.5F, -.3F, 2.05F, false, true, -1, EntitySeat.class));
		this.partData.add(new PartData(-0.5F, -.3F, 2.05F, false, true, -1, EntitySeat.class));
		this.partData.add(new PartData(0.625F, -0.7F, 1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, 1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0.625F, -0.7F, -0.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, -0.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0.625F, -0.7F, -1.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, -1.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0.625F, -0.7F, -2.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, -2.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0.625F, -0.7F, -3.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, -3.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0.625F, -0.7F, -4.1F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(-0.625F, -0.7F, -4.1F, EntitySeat.class, EntityChest.class));
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
	public ResourceLocation getBackplateTexture(){
		return backplateTexture;
	}
	
	@Override
	public ResourceLocation getMouldingTexture(){
		return mouldingTexture;
	}
	
	@Override
	public void drawHUD(int width, int height){
		VehicleHUDs.drawPlaneHUD(this, width, height);
	}
}