package minecrafttransportsimulator.radio;

/**Thread spawned to update the {@link RadioManager}.  This is used in place
 * of Forge events to keep the radio's update logic from blocking client events and
 * hurting FPS.  The only thing we need to worry about is the location of the listener.
 * This can be set from anywhere, though it's preferred to set it at the end of the 
 * tick as that's when their position will set in stone and no longer modified.
 *
 * @author don_bruce
 */
public class RadioThread extends Thread{
	private double listenerX = 0;
	private double listenerY = 0;
	private double listenerZ = 0;
	private boolean enablePlayback;
	
	@Override
	public void run(){
		//Keep running until the main game instance dies.
		while(true){
			RadioManager.updateRadios(listenerX, listenerY, listenerZ, enablePlayback);
			//Wait 1 second before updating again.
			try{
				this.sleep(1000);
			}catch(InterruptedException e){
				//Do nothing.
			}
		}
	}

	public void setListenerPosition(double x, double y, double z, boolean enabled){
		listenerX = x;
		listenerY = y;
		listenerZ = z;
		enablePlayback = enabled;
	}
}
