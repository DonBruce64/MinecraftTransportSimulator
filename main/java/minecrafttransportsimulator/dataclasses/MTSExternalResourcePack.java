package minecrafttransportsimulator.dataclasses;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Sets;

import minecrafttransportsimulator.MTS;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

public class MTSExternalResourcePack implements IResourcePack{
	@Override
	public InputStream getInputStream(ResourceLocation location) throws IOException{
		return FileUtils.openInputStream(new File(MTS.assetDir + File.separatorChar + location.getResourcePath()));
	}

	@Override
	public boolean resourceExists(ResourceLocation location){
		return new File(MTS.assetDir, location.getResourcePath()).exists();
	}

	@Override
	public Set<String> getResourceDomains() {
		return Sets.newHashSet(MTS.MODID);
	}

	@Override
	public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException{
		return null;
	}

	@Override
	public BufferedImage getPackImage() throws IOException{
		return null;
	}

	@Override
	public String getPackName(){
		return MTS.MODID;
	}
}
