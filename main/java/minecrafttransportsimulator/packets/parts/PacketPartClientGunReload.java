package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackPartObject.PartBulletConfig;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.vehicles.parts.APartGun;
import net.minecraft.nbt.NBTTagCompound;


/**This packet is sent to clients when a gun is reloaded.
 * Adds the specified ammo to the gun, and plays the
 * reloading sound for that gun.
 * 
 * @author don_bruce
 */
public class PacketPartClientGunReload extends APacketVehiclePart{
	private String bulletReloaded;

	public PacketPartClientGunReload(){}
	
	public PacketPartClientGunReload(APartGun gun, String bulletReloaded){
		super(gun);
		this.bulletReloaded = bulletReloaded;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		bulletReloaded = tag.getString("bulletReloaded");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setString("bulletReloaded", bulletReloaded);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		APartGun gun = (APartGun) getPart(world);
		PartBulletConfig bulletPack = PackParserSystem.getPartPack(bulletReloaded).bullet;
		gun.loadedBullet = bulletReloaded;
		gun.bulletsLeft += bulletPack.quantity;
		gun.reloadTimeRemaining = gun.pack.gun.reloadTime;
		gun.reloading = true;
		MTS.proxy.playSound(player.getPosition(), gun.partName + "_reloading", 1, 1);
	}
}
