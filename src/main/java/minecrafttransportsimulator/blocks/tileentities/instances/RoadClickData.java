package minecrafttransportsimulator.blocks.tileentities.instances;

/**Helper class for containing data of what was clicked on this road.
 * Note that laneClicked MAY be a negative number in order to allow
 * for offset road-linkings.  Keep this in mind.
 *
 * @author don_bruce
 */
public class RoadClickData{
	public TileEntityRoad roadClicked;
	public int laneClicked;
	public boolean clickedStart;
	public boolean clickedSameDirection;

	public RoadClickData(TileEntityRoad roadClicked, int laneClicked, boolean clickedStart, boolean clickedForward){
		this.roadClicked = roadClicked;
		this.laneClicked = laneClicked;
		this.clickedStart = clickedStart;
		this.clickedSameDirection = clickedForward;
	}
}