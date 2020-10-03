package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**This packet is sent to servers when a radio changes state. It sets the state on the server, and then
 * forwards the state change to all clients.  This allows song changes to be on both servers and clients.
 * 
 * @author don_bruce
 */
public class PacketRadioStateChange extends APacketBase{
	private final int radioID;
	private final RadioSources source;
	private final int volume;
	private final int preset;
	
	public PacketRadioStateChange(Radio radio, RadioSources source, int volume, int preset){
		super(null);
		this.radioID = radio.radioID;
		this.source = source;
		this.volume = volume;
		this.preset = preset;
	}
	
	public PacketRadioStateChange(ByteBuf buf){
		super(buf);
		this.radioID = buf.readInt();
		this.source = RadioSources.values()[buf.readByte()];
		this.volume = buf.readByte();
		this.preset = buf.readByte();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(radioID);
		buf.writeByte(source.ordinal());
		buf.writeByte(volume);
		buf.writeByte(preset);
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		Radio radio = world.isClient() ? Radio.createdClientRadios.get(radioID) : Radio.createdServerRadios.get(radioID);
		if(radio != null){
			if(world.isClient()){
				if(!radio.getSource().equals(source)){
					radio.changeSource(source, false);
				}
				if(radio.volume != volume){
					radio.changeVolume(volume, false);
				}
				if(radio.preset != preset){
					radio.pressPreset(preset, false);
				}
			}else{
				radio.setProperties(source, volume, preset);
			}
		}
	}
}
