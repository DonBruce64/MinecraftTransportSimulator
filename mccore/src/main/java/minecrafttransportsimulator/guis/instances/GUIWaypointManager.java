package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

import java.util.*;

public class GUIWaypointManager extends AGUIBase {

    private int scrollSpot = 0;
    private final EntityVehicleF_Physics vehicle;
    private final AWrapperWorld world;
    private final IWrapperPlayer player;


    private GUIComponentButton componentListUpButton;
    private GUIComponentButton componentListDownButton;
    private GUIComponentButton addWaypointButton;
    private GUIComponentButton removeWaypointButton;
    private final List<GUIComponentButton> waypointSelectionButtons = new ArrayList<>();

    private final List<GUIComponentTextBox> waypointNameList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointXList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointYList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointZList = new ArrayList<>();
    private Map<String, NavWaypoint> globalWaypoint;
    private List<NavWaypoint> globalWaypointList;
    private NavWaypoint currentNavpoint;
    private int maxWaypointIndex;

    @Override
    public void setupComponents() {
        super.setupComponents();
        globalWaypointList = new ArrayList<>(globalWaypoint.values());

        waypointSelectionButtons.clear();
        for (int i = 0; i < 7; ++i) {
            int finalI = i;

            GUIComponentButton button = new GUIComponentButton(this, guiLeft + 10, guiTop + 20 + 20 * waypointSelectionButtons.size(), 25, 20, Integer.toString(i+scrollSpot+1)) {//width205
                @Override
                public void onClicked(boolean leftSide) {
                    for(GUIComponentButton button1:waypointSelectionButtons){
                        button1.enabled = true;
                    }
                    this.enabled = false;
                    currentNavpoint = globalWaypointList.get(finalI+scrollSpot);
                }
            };
            addComponent(button);


            String waypointNameString = "NONE";
            if(i+scrollSpot<globalWaypointList.size()){
                waypointNameString = globalWaypointList.get(i+scrollSpot).name;
            }
            String waypointXString = "NONE";
            if(i+scrollSpot<globalWaypointList.size()){
                waypointXString =  Double.toString(globalWaypointList.get(i+scrollSpot).position.x);
            }
            String waypointYString = "NONE";
            if(i+scrollSpot<globalWaypointList.size()){
                waypointYString =  Double.toString(globalWaypointList.get(i+scrollSpot).position.y);
            }
            String waypointZString = "NONE";
            if(i+scrollSpot<globalWaypointList.size()){
                waypointZString = Double.toString(globalWaypointList.get(i+scrollSpot).position.z);
            }

            GUIComponentTextBox waypointName = new GUIComponentTextBox(this, guiLeft + 40, guiTop + 23 + 20 * waypointNameList.size(), 40, 14, waypointNameString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                }
            };

            GUIComponentTextBox waypointX = new GUIComponentTextBox(this, guiLeft + 85, guiTop + 23 + 20 * waypointXList.size(), 40, 14, waypointXString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                }
            };

            GUIComponentTextBox waypointY = new GUIComponentTextBox(this, guiLeft + 130, guiTop + 23 + 20 * waypointXList.size(), 40, 14, waypointYString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                }
            };

            GUIComponentTextBox waypointZ = new GUIComponentTextBox(this, guiLeft + 175, guiTop + 23 + 20 * waypointXList.size(), 40, 14, waypointZString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                }
            };

            addComponent(waypointName);
            addComponent(waypointX);
            addComponent(waypointY);
            addComponent(waypointZ);


            waypointSelectionButtons.add(button);
            waypointNameList.add(waypointName);
            waypointXList.add(waypointX);
            waypointYList.add(waypointY);
            waypointZList.add(waypointZ);

        }
        addComponent(componentListUpButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 25, 20, 20, "/\\") {
            @Override
            public void onClicked(boolean leftSide) {
                scrollSpot -= 7;
            }
        });
        addComponent(componentListDownButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 135, 20, 20, "\\/") {
            @Override
            public void onClicked(boolean leftSide) {
                scrollSpot += 7;
            }
        });

        addComponent(addWaypointButton = new GUIComponentButton(this, guiLeft + 20, guiTop + 160, 80, 20, "ADD") {
            @Override
            public void onClicked(boolean leftSide) {


                NavWaypoint waypoint = new NavWaypoint(world,Integer.toString(Math.max(globalWaypointList.size(),maxWaypointIndex)+1),0.0,0.0,player.getPosition());

                maxWaypointIndex = Math.max(globalWaypointList.size(),maxWaypointIndex)+1;

                globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
                globalWaypointList = new ArrayList<>(globalWaypoint.values());

                System.out.println(maxWaypointIndex);

            }
        });
        addComponent(removeWaypointButton = new GUIComponentButton(this, guiLeft + 110, guiTop + 160, 80, 20, "DELETE") {
            @Override
            public void onClicked(boolean leftSide) {
                if(currentNavpoint!=null)NavWaypoint.removeFromWorld(world, currentNavpoint.name);
                globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
                globalWaypointList = new ArrayList<>(globalWaypoint.values());
                currentNavpoint = null;
            }
        });
    }

    @Override
    public void setStates() {
        super.setStates();

        if(scrollSpot==0)componentListUpButton.enabled = false;
        else componentListUpButton.enabled = true;

        if(scrollSpot+7+1>globalWaypointList.size())componentListDownButton.enabled = false;
        else componentListDownButton.enabled = true;



        for (int i = 0; i < 7; ++i){
            //selection button
            GUIComponentButton button = waypointSelectionButtons.get(i);
            if(i+scrollSpot+1> globalWaypointList.size()){

                button.visible = false;

            }else{
                button.visible = true;
            }
            if(currentNavpoint==null){
                button.enabled = true;
            }
            button.text = Integer.toString(i+scrollSpot+1);
            //waypoint name
            GUIComponentTextBox waypointName = waypointNameList.get(i);
            if(i+scrollSpot+1> globalWaypointList.size()){
                waypointName.visible = false;
            }else{
                waypointName.visible = true;
            }
            if(!waypointName.focused) updateNameText(i);
            //waypoint x
            GUIComponentTextBox waypointX = waypointXList.get(i);
            if(i+scrollSpot+1> globalWaypointList.size()){
                waypointX.visible = false;
            }else{
                waypointX.visible = true;
            }
            if(!waypointX.focused)updateXText(i);
            //waypoint y
            GUIComponentTextBox waypointY = waypointYList.get(i);
            if(i+scrollSpot+1> globalWaypointList.size()){
                waypointY.visible = false;
            }else{
                waypointY.visible = true;
            }
            if(!waypointY.focused)updateYText(i);
            //waypoint z
            GUIComponentTextBox waypointZ = waypointZList.get(i);
            if(i+scrollSpot+1> globalWaypointList.size()){
                waypointZ.visible = false;
            }else{
                waypointZ.visible = true;
            }
            if(!waypointZ.focused)updateZText(i);
        }
    }

    public void updateNameText(int i){
        GUIComponentTextBox waypointName = waypointNameList.get(i);
        String waypointNameString = "NONE";
        if(i+scrollSpot<globalWaypointList.size()){
            waypointNameString = globalWaypointList.get(i+scrollSpot).name;
        }
        waypointName.setText(waypointNameString);
    }
    public void updateYText(int i){
        GUIComponentTextBox waypointY = waypointYList.get(i);
        String waypointYString = "NONE";
        if(i+scrollSpot<globalWaypointList.size()){
            waypointYString = Double.toString(globalWaypointList.get(i+scrollSpot).position.y);
        }
        waypointY.setText(waypointYString);
    }
    public void updateXText(int i){
        GUIComponentTextBox waypointX = waypointXList.get(i);
        String waypointXString = "NONE";
        if(i+scrollSpot<globalWaypointList.size()){
            waypointXString = Double.toString(globalWaypointList.get(i+scrollSpot).position.x);
        }
        waypointX.setText(waypointXString);
    }
    public void updateZText(int i){
        GUIComponentTextBox waypointZ = waypointZList.get(i);
        String waypointZString = "NONE";
        if(i+scrollSpot<globalWaypointList.size()){
            waypointZString = Double.toString(globalWaypointList.get(i+scrollSpot).position.z);
        }
        waypointZ.setText(waypointZString);
    }



    public GUIWaypointManager(IWrapperPlayer player,EntityVehicleF_Physics vehicle)
    {
        super();
        this.player = player;
        this.world = player.getWorld();
        this.vehicle = vehicle;
        this.globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
    }
}


