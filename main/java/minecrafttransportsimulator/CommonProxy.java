package minecrafttransportsimulator;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ForgeContainerGUISystem;
import minecrafttransportsimulator.systems.SFXSystem.SFXEntity;
import minecrafttransportsimulator.systems.pack.PackParserSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

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
