package minecrafttransportsimulator.planes.Vulcanair;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.VehicleHUDs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityVulcanair extends EntityPlane{
	private static final ResourceLocation[] backplateTextures = getBackplateTextures();
	private static final ResourceLocation[] mouldingTextures = getMouldingTextures();
	
	public EntityVulcanair(World world){
		super(world);
	}
	
	public EntityVulcanair(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "Wolfvanox";
	}

	@Override
	protected void initProperties(){
		hasFlaps = true;
		lightSetup = 15;
		numberPowerfulLights = 1;
		maxFuel = 15000;
		emptyMass=1230;
		wingspan=12.0F;
		wingArea=18.6F;
		tailDistance=7;
		rudderArea=1.5F;
		elevatorArea=3.0F;
		defaultElevatorAngle=-10F;
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
	}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0, -0.75F, 4.62F, true, false, EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(-1.75F, -0.75F, 0, EntityWheel.EntityWheelSmall.class, EntityPontoon.EntityPontoonDummy.class));
		this.partData.add(new PartData(1.75F, -0.75F, 0F,  EntityWheel.EntityWheelSmall.class, EntityPontoon.EntityPontoonDummy.class));
		this.partData.add(new PartData(2.69F, 1.15F, 1.17F, false, false, EntityEngineAircraftSmall.class));
		this.partData.add(new PartData(-2.69F, 1.15F, 1.17F, false, false, EntityEngineAircraftSmall.class));
		this.partData.add(new PartData(0, 0.1F, 2.37F, false, true, EntitySeat.class));
		this.partData.add(new PartData(0, 0.1F, 1.245F, EntitySeat.class, EntityChest.class));
		this.partData.add(new PartData(0, 0.1F, 0.12F, EntitySeat.class, EntityChest.class));
	}

	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{0, -0.3F, 4F, 1, 1},
			{0, 1.0F, -4.5F, 1, 0.5F},
			{-1.25F, 1.2F, -4.75F, 1, 0.125F},
			{1.25F, 1.2F, -4.75F, 1, 0.125F},
			{0F, 1.2F, -4.75F, 0.25F, 2.25F},
			{0F, 0.0F, -1.5F, 1.75F, 1.75F},
			{0F, 0.5F, -3.5F, 1.25F, 1.25F},
			
			{0F, 1.7F, 1.5F, 1.75F, 0.125F},
			{0F, 1.7F, 0.25F, 1.75F, 0.125F},
			{-2.0F, 1.7F, 0.25F, 1.75F, 0.125F}, 
			{2.0F, 1.7F, 0.25F, 1.75F, 0.125F}, 
			{-4.25F, 1.7F, 0.25F, 1.75F, 0.125F}, 
			{4.25F, 1.7F, 0.25F, 1.75F, 0.125F}, 
			{-6.5F, 1.7F, 0.25F, 1.75F, 0.125F}, 
			{6.5F, 1.7F, 0.25F, 1.75F, 0.125F}
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
		for(byte i=0; i<7; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/vulcanair/backplate" + i + ".png");
		}
		return texArray;
	}
	
	private static ResourceLocation[] getMouldingTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<7; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/vulcanair/moulding" + i + ".png");
		}
		return texArray;
	}
}
