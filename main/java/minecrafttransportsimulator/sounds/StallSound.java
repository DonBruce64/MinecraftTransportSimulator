package minecrafttransportsimulator.sounds;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import minecrafttransportsimulator.systems.ClientEventSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class StallSound extends MovingSound{
	private static final SoundEvent stallSoundEvent = new SoundEvent(new ResourceLocation(MTS.MODID + ":stall_buzzer"));
	private final EntityPlayer player;
	private List<EntityPlane> planesToBuzz = new ArrayList<EntityPlane>();
	
	public StallSound(){
		super(stallSoundEvent, SoundCategory.MASTER);
		this.volume = 0.001F;
		this.repeat = true;
		this.player = Minecraft.getMinecraft().thePlayer;
		this.xPosF = (float) player.posX;
		this.yPosF = (float) player.posY;
		this.zPosF = (float) player.posZ;
	}
	
	@Override
	public void update(){
		this.volume = 0;
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
			if(ClientEventSystem.playerLastSeat != null){
				if(planesToBuzz.contains(ClientEventSystem.playerLastSeat.parent)){
					this.xPosF = (float) ClientEventSystem.playerLastSeat.parent.posX;
					this.yPosF = (float) ClientEventSystem.playerLastSeat.parent.posY;
					this.zPosF = (float) ClientEventSystem.playerLastSeat.parent.posZ;
					volume = 1;
					return;
				}
			}
		}
	}
	
	public void setOn(EntityPlane plane){
		if(!planesToBuzz.contains(plane)){
			planesToBuzz.add(plane);
		}
	}
	
	public void setOff(EntityPlane plane){
		planesToBuzz.remove(plane);
	}
}
