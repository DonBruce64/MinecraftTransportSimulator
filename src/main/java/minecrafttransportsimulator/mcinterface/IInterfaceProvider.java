package minecrafttransportsimulator.mcinterface;

/**Interface that provides interfaces.  An instance of this interface is passed-in to
 * the {@link MasterLoader} on boot to kick-off the loading process.  This is made
 * as such as it allows the interface to be implemented on a class that holds
 * the actual interfaces while allowing that class to do other things.  This gives
 * flexibility on the mcintercae implementation on how it stores the interfaces.
 * Interfaces are ensured not to change during run-time, so they may be "gotten"
 * and stored somewhere else should it be more convenient.
 *
 * @author don_bruce
 */
public interface IInterfaceProvider{
	
	public String getDomain();
	
	public IInterfaceAudio getAudioInterface();
	
	public IInterfaceCore getCoreInterface();
	
	public IInterfaceGame getGameInterface();
	
	public IInterfaceInput getInputInterface();
	
	public IInterfaceNetwork getNetworkInterface();
	
	public IInterfaceOGGDecoder getOGGDecoderInterface();
	
	public IInterfaceRender getRenderInterface();
}
