package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;

/**Contains numerous camera functions for view edits.
 * 
 * @author don_bruce
 */
public final class CameraSystem{
	public static boolean lockedView = true;
	public static boolean disableHUD = false;
	private static boolean active = false;
	public static int hudMode = 2;
	private static int zoomLevel = 4;
	
	private static EntityCamera camera;

	public static void setCameraActive(boolean active){
		if(active){
			if(camera == null){
				if(ClientEventSystem.playerLastSeat != null){
					if(ClientEventSystem.playerLastSeat.parent != null){
						camera = new EntityCamera(Minecraft.getMinecraft().thePlayer);
						Minecraft.getMinecraft().thePlayer.worldObj.spawnEntityInWorld(camera);
					}
				}
			}
		}else{
			Minecraft.getMinecraft().setRenderViewEntity(Minecraft.getMinecraft().thePlayer);
			if(camera != null){
				camera.setDead();
				camera = null;
			}
		}
	}
	
	public static void changeCameraZoom(boolean zoomOut){
		if(zoomLevel < 15 && zoomOut){
			++zoomLevel;
		}else if(zoomLevel > 4 && !zoomOut){
			--zoomLevel;
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		MTS.proxy.playSound(Minecraft.getMinecraft().thePlayer, "gui.button.press", 1, 1);
	}

	private static class EntityCamera extends EntityLivingBase{
		private final EntityPlayer player;
		private final EntitySeat seat;
		private final EntityMultipartMoving moving;
		private static MTSVector playerVector;
		private static MTSVector cameraVector;
		
		public EntityCamera(EntityPlayer seatedPlayer){
			super(seatedPlayer.getEntityWorld());
			this.player = seatedPlayer;
			this.seat = (EntitySeat) seatedPlayer.getRidingEntity();
			this.moving = (EntityMultipartMoving) seat.parent;
			this.onUpdate();
		}
		
		@Override
		public void onUpdate(){
			super.onUpdate();
			if(!Minecraft.getMinecraft().isGamePaused()){
				if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
					this.setPosition(player.posX, player.posY, player.posZ);
				}else{
					MTSVector playerVector = RotationSystem.getRotatedPoint((float) (player.posX - moving.posX), (float) (player.posY - moving.posY), (float) (player.posZ - moving.posZ), moving.rotationRoll, moving.rotationYaw, moving.rotationRoll);
					//MTSVector cameraVector = RotationSystem.getRotatedPoint((float) - Math.sin(rotationYaw)
					//TODO fix this.
					this.setPosition(player.posX, player.posY, player.posZ);
				}
				boolean mouseYoke = ConfigSystem.getBooleanConfig("mouseYoke");
				if(!mouseYoke || (mouseYoke && !CameraSystem.lockedView)){
					rotationYaw += player.rotationYaw - player.prevRotationYaw;
					rotationPitch += player.rotationPitch - player.prevRotationPitch;
				}
				
				if(CameraSystem.lockedView){
					rotationYaw += moving.rotationYaw - moving.prevRotationYaw;
					if(moving.rotationPitch > 90 || moving.rotationPitch < -90){
						rotationPitch -= moving.rotationPitch - moving.prevRotationPitch;
					}else{
						rotationPitch += moving.rotationPitch - moving.prevRotationPitch;
					}
					if((moving.rotationPitch > 90 || moving.rotationPitch < -90) ^ moving.prevRotationPitch > 90 || moving.prevRotationPitch < -90){
						//TODO get this camera flip feature working.
						//rider.rotationYaw+=180;
					}
				}
			}
		}

		@Override
		public Iterable<ItemStack> getArmorInventoryList(){
			return null;
		}

		@Override
		public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn){
			return null;
		}

		@Override
		public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack){}

		@Override
		public EnumHandSide getPrimaryHand(){
			return EnumHandSide.RIGHT;
		}
	}
}
