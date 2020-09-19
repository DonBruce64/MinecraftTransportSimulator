package mcinterface;

import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;

/**Interface for the core MC system.  This class has methods for registrations
 * file locations, and other core things that are common to clients and servers.
 * Client-specific things go into {@link InterfaceGame}, rendering goes into
 * {@link InterfaceRender}.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public class InterfaceCore{
	
	/**
	 *  Returns true if the mod with the passed-in modID is present.
	 */
	public static boolean isModPresent(String modID){
		return Loader.isModLoaded(modID);
	}
	
	/**
	 *  Returns the text-based name for the passed-in mod.
	 */
	public static String getModName(String modID){
		return Loader.instance().getIndexedModList().get(modID).getName();
	}
	
	/**
	 *  Returns the text-based name for the passed-in fluid.
	 */
	public static String getFluidName(String fluidID){
		return FluidRegistry.getFluid(fluidID) != null ? new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName() : "INVALID";
	}

	/**
	 *  Returns the translation of the passed-in text from the lang file.
	 *  Put here to prevent the need for referencing the MC class directly, which
	 *  will change during updates.
	 */
	public static String translate(String text){
		return  I18n.translateToLocal(text);
	}
}
