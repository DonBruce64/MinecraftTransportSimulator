package minecrafttransportsimulator.sound;

import minecrafttransportsimulator.guis.instances.GUIRadio;

/**Interface for classes that need to have stream-able radios on them.
 * These are controlled via {@link GUIRadio}.
 * Note that providers must be {@link ISoundProvider}s as they provide sound at a fixed position and velocity.
 *
 * @author don_bruce
 */
public interface IRadioProvider extends ISoundProvider{
    /**
	 *  Gets the radio from this provider.
	 */
    public Radio getRadio();
}
