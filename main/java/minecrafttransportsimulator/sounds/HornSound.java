package minecrafttransportsimulator.sounds;

import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.entities.main.EntityCar;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;

public class HornSound extends MovingSound{
	private final EntityCar car;
	private final EntityPlayer player;
	
	private MTSVector playerPos = new MTSVector(0, 0, 0);
	private MTSVector carPos = new MTSVector(0, 0, 0);
	private double soundVelocity;
	
	public HornSound(String soundName, EntityCar car){
		super(SFXSystem.getSoundEventFromName(soundName), SoundCategory.MASTER);
		this.volume=1;
		this.repeat=true;
		this.xPosF = (float) car.posX;
		this.yPosF = (float) car.posY;
		this.zPosF = (float) car.posZ;
		this.car = car;
		this.player = Minecraft.getMinecraft().thePlayer;
	}
	
	@Override
	public void update(){
		if(car.shouldSoundBePlaying()){
			this.xPosF = (float) player.posX;
			this.yPosF = (float) player.posY;
			this.zPosF = (float) player.posZ;
			playerPos.set(player.posX, player.posY, player.posZ);
			carPos.set(car.posX, car.posY,car.posZ);
			
			if(player.getRidingEntity() instanceof EntitySeat){
				if(car.equals(((EntitySeat) player.getRidingEntity()).parent)){
					this.pitch=1;
					if(SFXSystem.isPlayerInsideVehicle()){
						this.volume = 0.5F;
					}else{
						this.volume = 1;
					}
					return;
				}
			}
			soundVelocity = (playerPos.distanceTo(carPos) - playerPos.add(player.motionX, player.motionY, player.motionZ).distanceTo(carPos.add(car.motionX, car.motionY, car.motionZ)));
			this.pitch=(float) (1+soundVelocity/10);
			this.volume = (float) (5/playerPos.distanceTo(carPos));
			if(SFXSystem.isPlayerInsideVehicle()){
				this.volume *= 0.5F;
			}
		}else{
			this.donePlaying=true;
			car.setCurrentSound(null);
		}
	}
}
