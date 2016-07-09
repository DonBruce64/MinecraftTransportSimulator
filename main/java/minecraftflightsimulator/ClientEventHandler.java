package minecraftflightsimulator;

import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.helpers.ControlHelper;
import minecraftflightsimulator.helpers.RenderHelper;
import minecraftflightsimulator.packets.general.GUIPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

//This class handles rendering/camera edits that need to happen when riding planes.
//It also calls GUI's up when the player presses the P key.
@SideOnly(Side.CLIENT)
public class ClientEventHandler{
	private static Minecraft minecraft = Minecraft.getMinecraft();	
	public static ClientEventHandler instance = new ClientEventHandler();
	
	@SubscribeEvent
	public void on(TickEvent.ClientTickEvent event){
		if(minecraft.theWorld != null){
			if(event.phase.equals(Phase.END)){
				for(Object entity : minecraft.theWorld.getLoadedEntityList()){
					if(entity instanceof EntityParent){
						((EntityParent) entity).moveChildren();
					}
				}
				if(minecraft.thePlayer.ridingEntity == null){
					RenderHelper.changeCameraRoll(0);
					RenderHelper.changeCameraZoom(0);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Pre event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			EntityParent parent = ((EntitySeat) event.entityPlayer.ridingEntity).parent;
			if(parent!=null){
				GL11.glPushMatrix();
				GL11.glRotated(parent.rotationPitch, Math.cos(parent.rotationYaw  * 0.017453292F), 0, Math.sin(parent.rotationYaw * 0.017453292F));
				GL11.glRotated(parent.rotationRoll, -Math.sin(parent.rotationYaw  * 0.017453292F), 0, Math.cos(parent.rotationYaw * 0.017453292F));
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderPlayerEvent.Post event){
		if(event.entityPlayer.ridingEntity instanceof EntitySeat){
			if(((EntitySeat) event.entityPlayer.ridingEntity).parent!=null){
				GL11.glPopMatrix();
			}
		}
	}
	
	@SubscribeEvent
	public void on(RenderGameOverlayEvent.Pre event){
		if(minecraft.thePlayer.ridingEntity instanceof EntitySeat){
			if(event.type.equals(RenderGameOverlayEvent.ElementType.HOTBAR)){
				event.setCanceled(true);
			}else if(event.type.equals(RenderGameOverlayEvent.ElementType.CHAT)){
				EntitySeat seat = ((EntitySeat) minecraft.thePlayer.ridingEntity);
				if(seat.parent != null && seat.driver && (minecraft.gameSettings.thirdPersonView==0 || RenderHelper.hudMode == 1)){
					seat.parent.drawHUD(event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
				}
			}
		}		
	}
	
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event){
    	if(ControlHelper.configKey.isPressed()){
    		EntityClientPlayerMP player = minecraft.thePlayer;
    		if(player.ridingEntity instanceof EntitySeat){
    			player.openGui(MFS.instance, -1, null, 0, 0, 0);
    		}else{
    			for(Object entity : player.worldObj.loadedEntityList){
    				if(entity instanceof EntityParent){
    					EntityParent parent = (EntityParent) entity;
    					if(parent.getDistanceToEntity(player) < 5){
    						MFS.MFSNet.sendToServer(new GUIPacket(parent.getEntityId()));
    						return;
    					}
    				}
    			}
    			
    		}
    	}
    }
}
