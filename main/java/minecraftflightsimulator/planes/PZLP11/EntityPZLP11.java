package minecraftflightsimulator.planes.PZLP11;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.entities.parts.EntityEngineAircraft;
import minecraftflightsimulator.entities.parts.EntityPontoon;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.entities.parts.EntitySkid;
import minecraftflightsimulator.entities.parts.EntityWheel;
import minecraftflightsimulator.rendering.VehicleHUDs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class EntityPZLP11 extends EntityPlane{
	private static final ResourceLocation backplateTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/hud_backplate.png");
	private static final ResourceLocation mouldingTexture = new ResourceLocation("mfs", "textures/planes/pzlp11/hud_moulding.png");
	
	public EntityPZLP11(World world){
		super(world);
	}
	
	public EntityPZLP11(World world, float posX, float posY, float posZ, float rotation, int textureCode){
		super(world, posX, posY, posZ, rotation, textureCode);
		this.displayName = "MAX 4H70 M";
	}

	@Override
	protected void initProperties(){
		openTop = true;
		lightSetup = 0;
		numberPowerfulLights = 0;
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
	protected void initProhibitedInstruments(){
		this.instruments.put((byte) 0, (byte) -1);
		this.instruments.put((byte) 4, (byte) -1);
		this.instruments.put((byte) 5, (byte) -1);
		this.instruments.put((byte) 9, (byte) -1);
	}
	
	@Override
	protected void initPartData(){
		this.partData.add(new PartData(0F, -0.3F, -5.25F,  EntitySkid.class));
		this.partData.add(new PartData(-1.4F, -1.8F, 0.375F, EntityWheel.EntityWheelLarge.class, EntityPontoon.class));
		this.partData.add(new PartData(1.4F, -1.8F, 0.375F, EntityWheel.EntityWheelLarge.class, EntityPontoon.class));
		this.partData.add(new PartData(0, -0.3F, 0.65F, false, false, 1, EntityEngineAircraft.class));
		this.partData.add(new PartData(0, -.1F, -1.3F, false, true, -1, EntitySeat.class));
	}
	
	@Override
	public float[][] getCoreLocations(){
		return new float[][]{
			{0, -0.3F, 0.5F, 1, 1},
			{0, -0.3F, -0.25F, 1, 1},
			{0, 0F, -2.5F, 1, 0.75F},
			{0, 0F, -3.75F, 1, 0.75F},
			{0, 0F, -5F, 1, 0.75F},
			{-1F, 0.5F, -5.5F, 1, 0.125F},
			{1F, 0.5F, -5.5F, 1, 0.125F},
			{0F, 0.5F, -5.5F, 0.25F, 1.25F},
			{-1.75F, 1.2F, -0.1F, 1.25F, 0.125F}, 
			{1.75F, 1.2F, -0.1F, 1.25F, 0.125F}, 
			{-3.25F, 1.2F, -0.1F, 1.25F, 0.125F}, 
			{3.25F, 1.2F, -0.1F, 1.25F, 0.125F}, 
			{-4.5F, 1.2F, -0.1F, 1.25F, 0.125F}, 
			{4.5F, 1.2F, -0.1F, 1.25F, 0.125F}
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