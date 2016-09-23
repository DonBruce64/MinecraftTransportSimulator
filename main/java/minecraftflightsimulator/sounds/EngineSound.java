package minecraftflightsimulator.sounds;

import minecraftflightsimulator.entities.parts.EntityEngine;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.utilities.MFSVector;
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
		if(engine.engineOn || engine.internalFuel > 0){
			this.xPosF = (float) player.posX;
			this.yPosF = (float) player.posY;
			this.zPosF = (float) player.posZ;
			playerPos.set(player.posX, player.posY, player.posZ);
			enginePos.set(engine.posX, engine.posY, engine.posZ);
			if(engine.parent != null){
				if(isPlayerRidingEnginesPlane(player)){
					if(engine.parent.equals(((EntitySeat) player.ridingEntity).parent)){
						this.field_147663_c=(float) (engine.engineRPM/pitchFactor);
					}else{
						soundVelocity = (playerPos.distanceTo(enginePos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(enginePos.add(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
						this.field_147663_c=(float) (engine.engineRPM*(1+soundVelocity/10)/pitchFactor);
					}
					if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
						this.volume = 0.5F;
					}else{
						this.volume = 1;
					}
				}else{
					soundVelocity = (playerPos.distanceTo(enginePos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(enginePos.add(engine.parent.motionX, engine.parent.motionY, engine.parent.motionZ)));
					this.field_147663_c=(float) (engine.engineRPM*(1+soundVelocity/10)/pitchFactor);
					this.volume = (float) (30*engine.engineRPM/pitchFactor/playerPos.distanceTo(enginePos));
				}
				playerLastX = player.posX;
				playerLastY = player.posY;
				playerLastZ = player.posZ;
			}
		}else{
			this.donePlaying=true;
		}
	}
	
	private boolean isPlayerRidingEnginesPlane(EntityPlayer player){
		if(player.ridingEntity instanceof EntitySeat){
			if(engine.parent.equals(((EntitySeat) player.ridingEntity).parent)){
				return true;
			}
		}
		return false;
	}
}
