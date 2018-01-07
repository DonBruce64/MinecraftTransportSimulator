package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.ConfigSystem;
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
	public void initConfig(File configFile){
		ConfigSystem.initCommon(configFile);
	}
	
	public void initControls(){}
	public void openGUI(Object clicked, EntityPlayer clicker){}
	public void playSound(Entity noisyEntity, String soundName, float volume, float pitch){}
	public void updateSFXEntity(SFXEntity entity, World world){}
}
