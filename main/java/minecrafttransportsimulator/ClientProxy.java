package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import minecrafttransportsimulator.guis.GUIInstruments;
import minecrafttransportsimulator.guis.GUIManual;
import minecrafttransportsimulator.guis.GUIPropellerBench;
import minecrafttransportsimulator.items.core.ItemManual;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
import minecrafttransportsimulator.systems.SFXSystem.SoundPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**Class responsible for performing client-only updates and operations.
 * Any version-updatable, client-based method should be put in here.
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy{
	
	@Override
	public void initConfig(File configFile){
		ConfigSystem.initClient(configFile);
	}

	@Override
	public void initControls(){
		ControlSystem.init();
	}
	@Override
	public void openGUI(Object clicked, EntityPlayer clicker){
		if(clicked instanceof EntityMultipartE_Vehicle){
			FMLCommonHandler.instance().showGuiScreen(new GUIInstruments((EntityMultipartE_Vehicle) clicked, clicker));
		}else if(clicked instanceof TileEntityPropellerBench){
			FMLCommonHandler.instance().showGuiScreen(new GUIPropellerBench((TileEntityPropellerBench) clicked, clicker));
		}else if(clicked instanceof ItemStack && ((ItemStack) clicked).getItem() instanceof ItemManual){
			FMLCommonHandler.instance().showGuiScreen(new GUIManual((ItemStack) clicked));
		}
	}
	
	@Override
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){
		SFXSystem.playSound(soundPosition, soundName, volume, pitch);
	}
	
	@Override
	public void updateSoundPart(SoundPart part, World world){
		SFXSystem.doSound(part, world);
	}
	
	@Override
	public void updateFXPart(FXPart part, World world){
		SFXSystem.doFX(part, world);
	}
}
