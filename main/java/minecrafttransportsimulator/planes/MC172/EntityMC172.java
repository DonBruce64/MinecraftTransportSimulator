package minecrafttransportsimulator.planes.MC172;

import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraftSmall;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.PlaneHUD;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityMC172 extends EntityPlane{
	private static final ResourceLocation[] backplateTextures = getBackplateTextures();
	private static final ResourceLocation[] mouldingTextures = getMouldingTextures();
	
	public EntityMC172(World world){
		super(world);
	}
	
	public EntityMC172(World world, float posX, float posY, float posZ, float rotation, byte textureOptions){
		super(world, posX, posY, posZ, rotation, textureOptions);
		//this.displayName = "MFS";
	}

	@Override
	protected void initProperties(){
		//hasFlaps = true;
		//lightSetup = 15;
		//numberPowerfulLights = 1;
		//fuelCapacity = 5000;
		//emptyMass=800;
		//wingspan=11.0F;
		//wingArea=16.0F;
		//tailDistance=7;
		//rudderArea=1.5F;
		//elevatorArea=3.0F;
		//defaultElevatorAngle=-5F;
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
	}
	
	@Override
	protected void initPartData(){
		this.getEntityString()
		this.partData.add(new PartData(0, -1F, 1.7F, true, false, EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(-1.65F, -1F, 0, EntityWheel.EntityWheelSmall.class, EntityPontoon.EntityPontoonDummy.class));
		this.partData.add(new PartData(1.65F, -1F, 0,  EntityWheel.EntityWheelSmall.class, EntityPontoon.EntityPontoonDummy.class));
		this.partData.add(new PartData(0, -0.4F, 1.65F, false, false, EntityEngineAircraftSmall.class));
		this.partData.add(new PartData(0, -.1F, 0, false, true, EntitySeat.class));
		this.partData.add(new PartData(0, -.1F, -1, EntitySeat.class, EntityChest.class));
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{0, -0.3F, 1.5F, 1, 1}, 
			{0, -0.3F, -2.75F, 1, 1}, 
			{0, -0.3F, -4.25F, 1, 1}, 
			{-1.5F, 0.5F, -4.25F, 1, 0.125F}, 
			{1.5F, 0.5F, -4.25F, 1, 0.125F},
			{0F, 0.5F, -4.25F, 0.25F, 1.5F}, 
			{0F, 1.6F, 0.25F, 1.25F, 0.125F},
			{-1.25F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{1.25F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{-2.5F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{2.5F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{-3.75F, 1.6F, 0.25F, 1.25F, 0.125F}, 
			{3.75F, 1.6F, 0.25F, 1.25F, 0.125F}
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
		PlaneHUD.drawPlaneHUD(this, width, height);
	}
	
	private static ResourceLocation[] getMouldingTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_birch.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/log_big_oak.png");
		return texArray;
	}
	
	private static ResourceLocation[] getBackplateTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_birch.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}
}