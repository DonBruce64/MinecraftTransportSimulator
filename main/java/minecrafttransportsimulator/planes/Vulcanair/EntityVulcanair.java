package minecrafttransportsimulator.planes.Vulcanair;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.entities.parts.EntityChest;
import minecrafttransportsimulator.entities.parts.EntityEngineAircraft;
import minecrafttransportsimulator.entities.parts.EntityPontoon;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.entities.parts.EntityWheel;
import minecrafttransportsimulator.rendering.VehicleHUDs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityVulcanair extends EntityPlane{
	//TODO make this match plane texture.
	private static final ResourceLocation backplateTexture = new ResourceLocation("textures/blocks/wool_colored_white.png");
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
	protected void initProhibitedInstruments(){}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0, -0.75F, 4.62F, true, false, -1, EntityWheel.EntityWheelSmall.class));
		this.partData.add(new PartData(-1.75F, -0.75F, 0, EntityWheel.EntityWheelSmall.class, EntityPontoon.class));
		this.partData.add(new PartData(1.75F, -0.75F, 0F,  EntityWheel.EntityWheelSmall.class, EntityPontoon.class));
		this.partData.add(new PartData(2.69F, 1.25F, 1.17F, false, false, 0, EntityEngineAircraft.class));
		this.partData.add(new PartData(-2.69F, 1.25F, 1.17F, false, false, 0, EntityEngineAircraft.class));
		this.partData.add(new PartData(0, 0.1F, 2.37F, false, true, -1, EntitySeat.class));
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
		return backplateTexture;
	}
	
	@Override
	public ResourceLocation getMouldingTexture(){
		return mouldingTextures[this.textureOptions];
	}
	
	@Override
	public void drawHUD(int width, int height){
		VehicleHUDs.drawPlaneHUD(this, width, height);
	}
	
	private static ResourceLocation[] getMouldingTextures(){
		ResourceLocation[] texArray = new ResourceLocation[7];
		for(byte i=0; i<7; ++i){
			texArray[i] = new ResourceLocation(MTS.MODID, "textures/planes/vulcanair/moulding" + i + ".png");
		}
		return texArray;
	}
}
