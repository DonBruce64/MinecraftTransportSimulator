package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

public class EngineSound extends EntitySoundBase{
	private EntityEngine engine;
	public float pitchFactor;
	
	private final float originalVolume;
	private Vec3 playerPos;
	private Vec3 soundPos;
	private double playerLastX;
	private double playerLastY;
	private double playerLastZ;
	private final EntityPlayer player;
	
	public EngineSound(ResourceLocation location, EntityEngine engine, float volume, float pitchFactor){
		super(location, engine, volume);
		this.originalVolume = volume;
		this.entity = this.engine = engine;
		this.pitchFactor = pitchFactor;
		this.player = Minecraft.getMinecraft().thePlayer;
		playerLastX = player.posX;
		playerLastY = player.posY;
		playerLastZ = player.posZ;
	}
	
	@Override
	public void update(){
		if(engine.fueled || engine.internalFuel > 0){
			super.update();
			playerPos = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
			soundPos = Vec3.createVectorHelper(this.xPosF, this.yPosF, this.zPosF);
			if(engine.parent != null){
				double soundVelocity = (playerPos.distanceTo(soundPos) - playerPos.addVector(player.motionX, player.motionY, player.motionZ).distanceTo(soundPos.addVector(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
				this.field_147663_c=(float) (engine.engineRPM*(1+soundVelocity/10)/pitchFactor);
				if(player.ridingEntity instanceof EntitySeat && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
					this.volume = (float) (originalVolume*0.5);
				}else{
					this.volume = originalVolume;
				}
				playerLastX = player.posX;
				playerLastY = player.posY;
				playerLastZ = player.posZ;
			}
		}else{
			this.donePlaying=true;
		}
	}
}
