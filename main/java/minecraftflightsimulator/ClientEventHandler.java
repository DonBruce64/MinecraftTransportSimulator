package minecraftflightsimulator;

import minecraftflightsimulator.entities.EntityParent;
import minecraftflightsimulator.entities.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

//This class handles rendering/camera edits that need to happen when riding planes.
public class ClientEventHandler{
	public static ClientEventHandler instance = new ClientEventHandler();
	private float offset;
	
	@SubscribeEvent
	public void on(TickEvent.ClientTickEvent event){
		if(Minecraft.getMinecraft().theWorld != null && event.phase.equals(Phase.END)){
			for(int i=0; i < Minecraft.getMinecraft().theWorld.loadedEntityList.size(); ++i){
				if(Minecraft.getMinecraft().theWorld.loadedEntityList.get(i) instanceof EntityParent){
					((EntityParent) Minecraft.getMinecraft().theWorld.loadedEntityList.get(i)).moveChildren();
				}
			}
		}
	}
	
	@SubscribeEvent
	public void on(TickEvent.PlayerTickEvent event){
		if(Minecraft.getMinecraft().thePlayer.ridingEntity == null){
			MFS.proxy.changeCameraRoll(0);
			MFS.proxy.changeCameraZoom(0);
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Pre event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			EntityParent parent = ((EntitySeat) event.entityPlayer.ridingEntity).parent;
			if(parent!=null){
				GL11.glPushMatrix();
				offset = event.entityPlayer.yOffset;
				if(offset!=0){
					event.entityPlayer.yOffset=0.6F;
					GL11.glTranslatef(0, -1.02F, 0);
				}
				GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
				GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Post event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			if(((EntitySeat) event.entityPlayer.ridingEntity).parent!=null){
				event.entityPlayer.yOffset=offset;
				GL11.glPopMatrix();
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderGameOverlayEvent.Pre event){
		if(Minecraft.getMinecraft().thePlayer.ridingEntity instanceof EntitySeat){
			if(event.type.equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
				event.setCanceled(true);
			}else if(event.type.equals(RenderGameOverlayEvent.ElementType.CHAT)){
				EntitySeat seat = ((EntitySeat) Minecraft.getMinecraft().thePlayer.ridingEntity);
				if(seat.parent != null && seat.driver && Minecraft.getMinecraft().gameSettings.thirdPersonView==0){
					seat.parent.drawHUD(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
				}
			}
		}		
	}
	
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event){
    	if(ClientProxy.configKey.isPressed()){
    		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    		player.openGui(MFS.instance, -1, player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
    	}
    }
}
