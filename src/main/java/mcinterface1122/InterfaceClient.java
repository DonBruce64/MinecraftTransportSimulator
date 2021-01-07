package mcinterface1122;

import minecrafttransportsimulator.mcinterface.IInterfaceClient;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
class InterfaceClient implements IInterfaceClient{

	@Override
	public boolean isGamePaused(){
		return Minecraft.getMinecraft().isGamePaused();
	}
	
	@Override
	public boolean isChatOpen(){
		return Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen();
	} 
	
	@Override
	public boolean inFirstPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}
	
	@Override
	public boolean inThirdPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 1;
	}
	
	@Override
	public void toggleFirstPerson(){
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
		}else{
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
		}
	}
	
	@Override
	public float getFOV(){
		return Minecraft.getMinecraft().gameSettings.fovSetting;
	}
	
	@Override
	public void setFOV(float setting){
		Minecraft.getMinecraft().gameSettings.fovSetting = setting;
	}
	
	@Override
	public IWrapperWorld getClientWorld(){
		return WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
	}
	
	@Override
	public IWrapperPlayer getClientPlayer(){
		EntityPlayer player = Minecraft.getMinecraft().player;
		return player != null ? WrapperWorld.getWrapperFor(player.world).getWrapperFor(player) : null;
	}
	
	@Override
	public IWrapperEntity getRenderViewEntity(){
		Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
		return WrapperWorld.getWrapperFor(entity.world).getWrapperFor(entity);
	}
}
