package minecrafttransportsimulator.sounds;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Plane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public class StallSound extends MovingSound{
	private static final SoundEvent stallSoundEvent = new SoundEvent(new ResourceLocation(MTS.MODID + ":stall_buzzer"));
	private final EntityPlayer player;
	private List<EntityMultipartF_Plane> planesToBuzz = new ArrayList<EntityMultipartF_Plane>();
	
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
			if(planesToBuzz.contains(player.getRidingEntity())){
				this.xPosF = (float) player.getRidingEntity().posX;
				this.yPosF = (float) player.getRidingEntity().posY;
				this.zPosF = (float) player.getRidingEntity().posZ;
				volume = 1;
				return;
			}
		}
	}
	
	public void setOn(EntityMultipartF_Plane plane){
		if(!planesToBuzz.contains(plane)){
			planesToBuzz.add(plane);
		}
	}
	
	public void setOff(EntityMultipartF_Plane plane){
		planesToBuzz.remove(plane);
	}
}
