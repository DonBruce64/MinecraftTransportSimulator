package minecrafttransportsimulator.planes.Trimotor;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftLarge;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.PlaneHUD;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityTrimotor extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side.png");
	private static final ResourceLocation mouldingTexture = new ResourceLocation(MTS.MODID, "textures/planes/trimotor/side_rotated.png");
	
	public EntityTrimotor(World world){
		super(world);
	}
	
	public EntityTrimotor(World world, float posX, float posY, float posZ, float rotation, byte textureOptions){
		super(world, posX, posY, posZ, rotation, textureOptions);
		this.displayName = "DB32-1204-UL";
	}

	@Override
	protected void initProperties(){
		lightSetup = 13;
		numberPowerfulLights = 2;
		fuelCapacity = 25000;
		emptyMass=3000;
		wingspan=22.6F;
		wingArea=70.0F;
		tailDistance=12;
		rudderArea=5.0F;
		elevatorArea=7.5F;
		defaultElevatorAngle=-10;
	}
	
	@Override
	protected void initInstruments(){
		this.instruments.put((byte) 0, (byte) 0);
		this.instruments.put((byte) 1, (byte) 0);
		this.instruments.put((byte) 2, (byte) 0);
		this.instruments.put((byte) 3, (byte) 0);
		this.instruments.put((byte) 4, (byte) 0);
		this.instruments.put((byte) 5, (byte) 0);
		this.instruments.put((byte) 6, (byte) 0);
		this.instruments.put((byte) 7, (byte) 0);
		this.instruments.put((byte) 8, (byte) 0);
		this.instruments.put((byte) 9, (byte) 0);
		this.instruments.put((byte) 10, (byte) 0);
		this.instruments.put((byte) 11, (byte) 0);
		this.instruments.put((byte) 12, (byte) 0);
		this.instruments.put((byte) 13, (byte) 0);
		this.instruments.put((byte) 20, (byte) 0);
		this.instruments.put((byte) 21, (byte) 0);
		this.instruments.put((byte) 22, (byte) 0);
		this.instruments.put((byte) 23, (byte) 0);
		this.instruments.put((byte) 30, (byte) 0);
		this.instruments.put((byte) 31, (byte) 0);
		this.instruments.put((byte) 32, (byte) 0);
		this.instruments.put((byte) 33, (byte) 0);
	}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0, 0F, -10.5F, true, false, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(-2.875F, -2.3F, 0.8F, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(2.875F, -2.3F, 0.8F, EntityWheel.EntityWheelLarge.class));
		this.partData.add(new PartData(2.875F, -0.76F, 1.06F, false, false, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(0, -0.76F, 3.76F, false, false, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(-2.875F, -0.76F, 1.06F, false, false, EntityEngineAircraftLarge.class));
		this.partData.add(new PartData(0.5F, -.3F, 2.05F, false, true, EntitySeat.class));
		this.partData.add(new PartData(-0.5F, -.3F, 2.05F, false, true, EntitySeat.class));
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
		PlaneHUD.drawPlaneHUD(this, width, height);
	}
}