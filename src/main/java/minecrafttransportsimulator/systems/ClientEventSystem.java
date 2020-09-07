package minecrafttransportsimulator.systems;

import mcinterface.BuilderGUI;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUIPackMissing;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**This class handles rendering/camera edits that need to happen when riding vehicles,
 * as well as clicking of vehicles and their parts, as well as some other misc things.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class ClientEventSystem{
    //TODO move this to the abstracted create tabs when we do that.
    
    /**
     * Renders a warning on the MTS core creative tab if there is no pack data.
     */
    @SubscribeEvent
    public static void on(DrawScreenEvent.Post event){
    	if(MTSRegistry.packItemMap.size() == 0){
	    	if(event.getGui() instanceof GuiContainerCreative){
	    		GuiContainerCreative creativeScreen = (GuiContainerCreative) event.getGui();
	    		if(CreativeTabs.CREATIVE_TAB_ARRAY[creativeScreen.getSelectedTabIndex()].equals(MTSRegistry.coreTab)){
	    			BuilderGUI.openGUI(new GUIPackMissing());
	    		}
	    	}
    	}
    }
}
