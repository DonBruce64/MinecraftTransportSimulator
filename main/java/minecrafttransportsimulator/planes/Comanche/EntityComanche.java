package minecrafttransportsimulator.planes.Comanche;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.VehicleHUDs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityComanche extends EntityPlane{
	private static final ResourceLocation[] backplateTextures = getBackplateTextures();
	private static final ResourceLocation[] mouldingTextures = getMouldingTextures();
	
	public EntityComanche(World world){
		super(world);
	}
	
	public EntityComanche(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "MFS.WOLF";
	}

	@Override
	protected void initProperties(){
		hasFlaps = true;
		lightSetup = 15;
		numberPowerfulLights = 2;
		maxFuel = 10000;
		emptyMass=1030;
		wingspan=11.2F;
		wingArea=16.5F;
		tailDistance=4.125F;
		rudderArea=0.7F;
		elevatorArea=2.0F;
		defaultElevatorAngle=-7.5F;
	}
	
	@Override
	protected void initInstruments(){
		this.instruments.put((byte) 0, (byte) 0);
		this.instruments.put((byte) 1, (byte) 0);
		this.instruments.put((byte) 2, (byte) 0);
		this.instruments.put((byte) 3, (byte) 0);
		this.instruments.put((byte) 5, (byte) 0);
		this.instruments.put((byte) 6, (byte) 0);
		this.instruments.put((byte) 7, (byte) 0);
		this.instruments.put((byte) 8, (byte) 0);
		this.instruments.put((byte) 10, (byte) 0);
		this.instruments.put((byte) 11, (byte) 0);
		this.instruments.put((byte) 12, (byte) 0);
		this.instruments.put((byte) 13, (byte) 0);
		this.instruments.put((byte) 20, (byte) 0);
		this.instruments.put((byte) 21, (byte) 0);
		this.instruments.put((byte) 22, (byte) 0);
		this.instruments.put((byte) 23, (byte) 0);
	}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0, -0.75F, 2.35F, true, false, EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(-1.85F, -0.6F, -0.35F, EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(1.85F, -0.6F, -0.35F,  EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(2.5F, -0.05F, 1.47F, false, false, EntityEngineAircraftSmall.class));
		this.partData.add(new PartData(-2.5F, -0.05F, 1.47F, false, false, EntityEngineAircraftSmall.class));
		this.partData.add(new PartData(0, 0.5F, -0.05F, false, true, EntitySeat.class));
		this.partData.add(new PartData(0, 0.5F, -1.3F, EntitySeat.class, EntityChest.class));
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{-1.75F, 1.3F, -5.25F,  1, 0.125F},
			{-0.6F, 1.3F, -5.25F,  1, 0.125F},
			{0.6F, 1.3F, -5.25F,  1, 0.125F},
			{1.75F, 1.3F, -5.25F,  1, 0.125F},
			{0, 1.3F, -5.25F,  0.25F, 1F},
			{0, 1.8F, -1.5F,  1, 0.125F},
			{0, 1.8F, -0.5F,  1.25F, 0.125F},
			{0, 0.8F, -4.5F, 0.75F, 1.0F},
			{0, 0.5F, -3.5F, 1, 1.25F},
			{0, 0.3F, -2.5F, 1, 1.5F},
			{0, 0.0F, 1.5F, 1, 1.25F},
			{0, 0.3F, 2.5F, 0.5F, 0.75F},
			{0, 0.0F, -0.5F,  1.25F, 0.125F},
			{2.0F, 0.2F, -0.5F,  1.25F, 0.125F},
			{3.5F, 0.3F, -0.5F,  1.25F, 0.125F},
			{5.0F, 0.35F, -0.5F,  1.25F, 0.125F},
			{6.5F, 0.4F, -0.5F,  1.0F, 0.125F},
			{-2.0F, 0.2F, -0.5F,  1.25F, 0.125F},
			{-3.5F, 0.3F, -0.5F,  1.25F, 0.125F},
			{-5.0F, 0.35F, -0.5F,  1.25F, 0.125F},
			{-6.5F, 0.4F, -0.5F,  1.0F, 0.125F},
			{-2.6F, 0.2F, 1.25F,  1.0F, 0.5F},
			{2.6F, 0.2F, 1.25F,  1.0F, 0.5F},
		};
	}
	
	@Override
	public ResourceLocation getBackplateTexture(){
		return backplateTextures[this.textureOptions];
	}
	
	@Override
	public ResourceLocation getMouldingTexture(){
		return mouldingTextures[this.textureOptions];
	}
	
	@Override
	public void drawHUD(int width, int height){
		VehicleHUDs.drawPlaneHUD(this, width, height);
	}
	
	private static ResourceLocation[] getBackplateTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<4; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/comanche/backplate" + i + ".png");
		}
		return texArray;
	}
	
	private static ResourceLocation[] getMouldingTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<4; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/comanche/moulding" + i + ".png");
		}
		return texArray;
	}
}
