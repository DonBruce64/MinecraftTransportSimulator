package minecrafttransportsimulator;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ForgeContainerGUISystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**Contains registration methods used by {@link MTSRegistry} and methods overridden by ClientProxy. 
 * See the latter for more info on overridden methods.
 * 
 * @author don_bruce
 */
public class CommonProxy{
	public void preInit(FMLPreInitializationEvent event){
		ConfigSystem.initCommon(event.getSuggestedConfigurationFile());
		PackParserSystem.init();
	}
	
	public void init(FMLInitializationEvent event){
		MTSRegistry.instance.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(MTS.instance, new ForgeContainerGUISystem());
	}
	
	public void openGUI(Object clicked, EntityPlayer clicker){}
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){}
	public void updateSFXEntity(SFXEntity entity, World world){}
}
