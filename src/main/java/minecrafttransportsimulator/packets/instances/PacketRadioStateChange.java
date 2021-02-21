package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.sound.Radio;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**This packet is sent to servers when a radio changes state. It sets the state on the server.  After this,
 * the radio will send out packets to all clients to update their states.  This allows song changes to be 
 * on both servers and clients.
 * 
 * @author don_bruce
 */
public class PacketRadioStateChange extends APacketEntity<Radio>{
	private final RadioSources source;
	private final int volume;
	private final int preset;
	
	public PacketRadioStateChange(Radio radio, RadioSources source, int volume, int preset){
		super(radio);
		this.source = source;
		this.volume = volume;
		this.preset = preset;
	}
	
	public PacketRadioStateChange(ByteBuf buf){
		super(buf);
		this.source = RadioSources.values()[buf.readByte()];
		this.volume = buf.readByte();
		this.preset = buf.readByte();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(source.ordinal());
		buf.writeByte(volume);
		buf.writeByte(preset);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, Radio radio){
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
		return false;
	}
}
