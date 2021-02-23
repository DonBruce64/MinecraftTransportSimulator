package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONConnection{
	@JSONDescription("If true, this connection will be flagged as a hookup. If false, it is assumed to be a hitch.")
	public boolean hookup;
	
	@JSONRequired
	@JSONDescription("The type of connection.  This can be anything you want, but it is recommended to use a generic enough name that allows your pack to be compatible with other packs wishing to use the same connection.  “Tow” is a common one in use for a hitch position on vehicles that allows them to be towed by a tow truck, for example.")
	public String type;
	
	@JSONRequired
	@JSONDescription("The position of this connection on the vehicle.")
	public Point3d pos;
	
	@JSONDescription("If true, then connecting vehicles will be mounted to this point rather than dragged.  Useful for things like flat-bed trailers, where you want the vehicle to stay in one place.")
	public boolean mounted;
	
	@JSONDescription("A list of connector objects.  These are rendered bits that appear when this connection is active.  MTS will render the model specified by modelName along a line from startingPos to endPos.  It will try to keep the connectors spaced evenly like treads, so keep this in mind when designing your connector, as there might be gaps if you have fractional distances.")
	public List<JSONConnectionConnector> connectors;
	
	public class JSONConnectionConnector{
		@JSONRequired
		@JSONDescription("The name of the model for this connector.  All connector models should be placed the “assets/yourPackID/connectors” folder, irrespective of your pack structure.  The PNGs for each connector belong there as well.  As such, simply put the file-name of the connector you wish to use here and MTS will do the rest.")
    	public String modelName;
		
		@JSONRequired
		@JSONDescription("The starting position for this connection.")
    	public Point3d startingPos;
		
		@JSONRequired
		@JSONDescription("The starting position for this connection.")
    	public Point3d endingPos;
		
		@JSONDescription("How long each segment of your connector is.  This is based on the model, as it tells MTS how often it needs to repeat rendering the model.")
    	public double segmentLength;
    }
}
