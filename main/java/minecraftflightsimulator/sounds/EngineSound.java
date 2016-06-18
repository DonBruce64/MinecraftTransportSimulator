package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.helpers.MFSVector;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class EngineSound extends EntitySoundBase{
	private EntityEngine engine;
	public float pitchFactor;
	
	private final float originalVolume;
	private MFSVector playerPos = new MFSVector(0, 0, 0);
	private MFSVector soundPos = new MFSVector(0, 0, 0);
	double soundVelocity;
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
			playerPos.set(player.posX, player.posY, player.posZ);
			soundPos.set(this.xPosF, this.yPosF, this.zPosF);
			if(engine.parent != null){
				soundVelocity = (playerPos.distanceTo(soundPos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(soundPos.add(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
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
