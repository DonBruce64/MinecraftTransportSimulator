package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.systems.SFXSystem;
import minecraftflightsimulator.utilites.MFSVector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class EngineSound extends MovingSound{
	private final EntityEngine engine;
	private final EntityPlayer player;
	private final float pitchFactor;
	
	private MFSVector playerPos = new MFSVector(0, 0, 0);
	private MFSVector enginePos = new MFSVector(0, 0, 0);
	private double playerLastX;
	private double playerLastY;
	private double playerLastZ;
	private double soundVelocity;
	
	public EngineSound(ResourceLocation location, EntityEngine engine, float pitchFactor){
		super(location);
		this.volume=1;
		this.repeat=true;
		this.xPosF = (float) engine.posX;
		this.yPosF = (float) engine.posY;
		this.zPosF = (float) engine.posZ;
		this.engine = engine;
		this.pitchFactor = pitchFactor;
		this.player = Minecraft.getMinecraft().thePlayer;
		playerLastX = player.posX;
		playerLastY = player.posY;
		playerLastZ = player.posZ;
	}
	
	@Override
	public void update(){
		if(engine.shouldSoundBePlaying()){
			this.xPosF = (float) engine.posX;
			this.yPosF = (float) engine.posY;
			this.zPosF = (float) engine.posZ;
			playerPos.set(player.posX, player.posY, player.posZ);
			enginePos.set(engine.posX, engine.posY, engine.posZ);
			if(engine.parent != null){
				if(player.ridingEntity instanceof EntitySeat){
					if(engine.parent.equals(((EntitySeat) player.ridingEntity).parent)){
						this.field_147663_c=(float) (engine.RPM/pitchFactor);
					}else{
						soundVelocity = (playerPos.distanceTo(enginePos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(enginePos.add(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
						this.field_147663_c=(float) (engine.RPM*(1+soundVelocity/10)/pitchFactor);
					}
					if(SFXSystem.isPlayerInsideVehicle()){
						this.volume = 0.5F;
					}else{
						this.volume = 1;
					}
				}else{
					soundVelocity = (playerPos.distanceTo(enginePos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(enginePos.add(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
					this.field_147663_c=(float) (engine.RPM*(1+soundVelocity/10)/pitchFactor);
					this.volume = (float) (30*engine.RPM/pitchFactor/playerPos.distanceTo(enginePos));
				}
				playerLastX = player.posX;
				playerLastY = player.posY;
				playerLastZ = player.posZ;
			}
		}else{
			this.donePlaying=true;
			engine.setCurrentSound(null);
		}
	}
}
