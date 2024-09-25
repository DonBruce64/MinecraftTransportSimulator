package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.NavWaypoint;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketWaypointUpdate;
import minecrafttransportsimulator.packets.instances.PacketWaypointUpdateRequest;

import java.util.*;

public class GUIWaypointManager extends AGUIBase {

    private final AWrapperWorld world;
    private final IWrapperPlayer player;

    /**
     * Global Waypoint Related Variable
     *
     * */
    private int scrollSpot = 0;
    private GUIComponentButton PageUpButton;
    private GUIComponentButton PageDownButton;
    private GUIComponentButton addWaypointButton;
    private GUIComponentButton removeWaypointButton;
    private final List<GUIComponentButton> waypointSelectionButtons = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointNameList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointXList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointYList = new ArrayList<>();
    private final List<GUIComponentTextBox> waypointZList = new ArrayList<>();

    //cache map directly from getAllWaypointsFromWorld()
    private Map<String, NavWaypoint> globalWaypoint;
    //cache list should be shown
    private List<NavWaypoint> globalWaypointList;
    private NavWaypoint currentWaypoint;
    private int currentWaypointIndex = -1;
    //cache max index
    private int maxWaypointIndex;

    /**
     * Vehicle Waypoint Related Variable
     *
     * */
    private final EntityVehicleF_Physics vehicle;
    private int V_scrollSpot = 0;
    private GUIComponentButton V_PageUpButton;
    private GUIComponentButton V_PageDownButton;
    private GUIComponentButton V_addWaypointButton;
    private GUIComponentButton V_removeWaypointButton;
    private final List<GUIComponentButton> V_waypointSelectionButtons = new ArrayList<>();
    private final List<GUIComponentTextBox> V_waypointNameList = new ArrayList<>();
    private final List<GUIComponentTextBox> V_waypointXList = new ArrayList<>();
    private final List<GUIComponentTextBox> V_waypointYList = new ArrayList<>();
    private final List<GUIComponentTextBox> V_waypointZList = new ArrayList<>();

    //cache map directly from getAllWaypointsFromWorld()
    private Map<String, NavWaypoint> V_globalWaypoint;
    //cache list should be shown
    private List<NavWaypoint> V_globalWaypointList;
    private NavWaypoint V_currentWaypoint;
    private int V_currentWaypointIndex = -1;
    //cache max index
    private int V_maxWaypointIndex;


    public GUIWaypointManager(IWrapperPlayer player,EntityVehicleF_Physics vehicle)
    {
        super();
        this.player = player;
        this.world = player.getWorld();
        this.vehicle = vehicle;
        this.globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
    }

    /**
     * Global
     * */
    public void updateGlobalWaypointList() {
        globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
        globalWaypointList = new ArrayList<>(globalWaypoint.values());
        NavWaypoint.sortWaypointListByIndex(globalWaypointList);
    }
    public void updateCurrentWaypoint(int index) {
        if(index<globalWaypointList.size() && index>-1){
            currentWaypointIndex = index;
            currentWaypoint = globalWaypointList.get(index);
        }else{
            currentWaypoint = null;
            currentWaypointIndex = -1;
        }
    }
    public void updateMaxWaypointIndex() {

        if(globalWaypointList.size()>0){
            for(NavWaypoint waypoint:globalWaypointList){
                maxWaypointIndex = Math.max(maxWaypointIndex,Integer.parseInt(waypoint.index));
            }
        }else{
            maxWaypointIndex = -1;
        }
    }
    public void setupGlobalPart() {
        updateGlobalWaypointList();
        waypointSelectionButtons.clear();
        waypointNameList.clear();
        waypointXList.clear();
        waypointYList.clear();
        waypointZList.clear();

        for (int pageIndex = 0; pageIndex < 7; ++pageIndex) {
            int finalpageIndex = pageIndex;

            //Index button init
            GUIComponentButton button = new GUIComponentButton(this, guiLeft + 10, guiTop + 10 + 12 * waypointSelectionButtons.size(), 25, 12, Integer.toString(pageIndex+scrollSpot+1)) {//width205
                @Override
                public void onClicked(boolean leftSide) {
                    for(GUIComponentButton IndexButton:waypointSelectionButtons){
                        IndexButton.enabled = true;
                    }
                    this.enabled = false;
                    updateCurrentWaypoint(finalpageIndex+scrollSpot);
                }
            };
            addComponent(button);

            //init TextBox text
            String waypointNameString = "";
            if(pageIndex+scrollSpot<globalWaypointList.size()){
                waypointNameString = globalWaypointList.get(pageIndex+scrollSpot).name;
            }
            String waypointXString = "";
            if(pageIndex+scrollSpot<globalWaypointList.size()){
                waypointXString =  Double.toString(globalWaypointList.get(pageIndex+scrollSpot).position.x);
            }
            String waypointYString = "";
            if(pageIndex+scrollSpot<globalWaypointList.size()){
                waypointYString =  Double.toString(globalWaypointList.get(pageIndex+scrollSpot).position.y);
            }
            String waypointZString = "";
            if(pageIndex+scrollSpot<globalWaypointList.size()){
                waypointZString = Double.toString(globalWaypointList.get(pageIndex+scrollSpot).position.z);
            }

            //init TextBox
            GUIComponentTextBox waypointName = new GUIComponentTextBox(this, guiLeft + 36, guiTop + 10 + 12 * waypointNameList.size(), 32, 10, waypointNameString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(currentWaypoint.index,getText(),Double.toString(currentWaypoint.targetSpeed),Double.toString(currentWaypoint.bearing),Double.toString(currentWaypoint.position.x),Double.toString(currentWaypoint.position.y),Double.toString(currentWaypoint.position.z),"false"));
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player,currentWaypoint.index));
                }
            };

            GUIComponentTextBox waypointX = new GUIComponentTextBox(this, guiLeft + 70, guiTop + 10 + 12 * waypointXList.size(), 45, 10, waypointXString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(currentWaypoint.index,currentWaypoint.name,Double.toString(currentWaypoint.targetSpeed),Double.toString(currentWaypoint.bearing),getText(),Double.toString(currentWaypoint.position.y),Double.toString(currentWaypoint.position.z),"false"));
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player, currentWaypoint.index));
                }
            };

            GUIComponentTextBox waypointY = new GUIComponentTextBox(this, guiLeft + 117, guiTop + 10 + 12 * waypointXList.size(), 45, 10, waypointYString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(currentWaypoint.index,currentWaypoint.name,Double.toString(currentWaypoint.targetSpeed),Double.toString(currentWaypoint.bearing),Double.toString(currentWaypoint.position.x),getText(),Double.toString(currentWaypoint.position.z),"false"));
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player, currentWaypoint.index));
                }
            };

            GUIComponentTextBox waypointZ = new GUIComponentTextBox(this, guiLeft + 164, guiTop + 10 + 12 * waypointXList.size(), 45, 10, waypointZString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(currentWaypoint.index,currentWaypoint.name,Double.toString(currentWaypoint.targetSpeed),Double.toString(currentWaypoint.bearing),Double.toString(currentWaypoint.position.x),Double.toString(currentWaypoint.position.y),getText(),"false"));
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player, currentWaypoint.index));
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
        addComponent(PageUpButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 10, 20, 20, "/\\") {
            @Override
            public void onClicked(boolean leftSide) {

                scrollSpot -= 7;

            }
        });
        addComponent(PageDownButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 74, 20, 20, "\\/") {
            @Override
            public void onClicked(boolean leftSide) {

                scrollSpot += 7;
            }
        });

        addComponent(addWaypointButton = new GUIComponentButton(this, guiLeft + 225 - 13, guiTop + 34, 22+13, 16, "ADD") {
            @Override
            public void onClicked(boolean leftSide) {
                updateMaxWaypointIndex();

                InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(Integer.toString(maxWaypointIndex+1),"P"+Integer.toString(maxWaypointIndex+1),"0.0","0.0",Double.toString(player.getPosition().x),Double.toString(player.getPosition().y),Double.toString(player.getPosition().z),"false"));
                InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player,Integer.toString(maxWaypointIndex+1)));
            }
        });
        addComponent(removeWaypointButton = new GUIComponentButton(this, guiLeft + 225 - 13, guiTop + 54, 22+13, 16, "DELETE") {
            @Override
            public void onClicked(boolean leftSide) {
                if(currentWaypoint != null){
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdate(currentWaypoint.index,currentWaypoint.name,Double.toString(currentWaypoint.targetSpeed),Double.toString(currentWaypoint.bearing),Double.toString(currentWaypoint.position.x),Double.toString(currentWaypoint.position.y),Double.toString(currentWaypoint.position.z),"true"));
                    InterfaceManager.packetInterface.sendToServer(new PacketWaypointUpdateRequest(player, currentWaypoint.index));
                    updateCurrentWaypoint(-1);
                }
            }
        });
    }

    /**
     * Vehicle
     * */
    public void V_updateGlobalWaypointList() {
        V_globalWaypoint = NavWaypoint.getAllWaypointsFromWorld(world);
        V_globalWaypointList = new ArrayList<>(V_globalWaypoint.values());
        NavWaypoint.sortWaypointListByIndex(V_globalWaypointList);
    }
    public void V_updateCurrentWaypoint(int index) {
        if(index<V_globalWaypointList.size() && index>-1){
            V_currentWaypointIndex = index;
            V_currentWaypoint = V_globalWaypointList.get(index);
        }else{
            V_currentWaypoint = null;
            V_currentWaypointIndex = -1;
        }
    }
    public void V_updateMaxWaypointIndex() {

        if(V_globalWaypointList.size()>0){
            for(NavWaypoint waypoint:V_globalWaypointList){
                V_maxWaypointIndex = Math.max(V_maxWaypointIndex,Integer.parseInt(waypoint.index));
            }
        }else{
            V_maxWaypointIndex = -1;
        }
    }

    private final int VehiclePartTop = 90;
    public void setupVehiclePart() {
        V_updateGlobalWaypointList();
        V_waypointSelectionButtons.clear();
        V_waypointNameList.clear();
        V_waypointXList.clear();
        V_waypointYList.clear();
        V_waypointZList.clear();

        for (int pageIndex = 0; pageIndex < 7; ++pageIndex) {
            int finalpageIndex = pageIndex;

            //Index button init
            GUIComponentButton button = new GUIComponentButton(this, guiLeft + 10, guiTop + 10 + 12 * V_waypointSelectionButtons.size() + VehiclePartTop, 25, 12, Integer.toString(pageIndex+V_scrollSpot+1)) {//width205
                @Override
                public void onClicked(boolean leftSide) {
                    for(GUIComponentButton IndexButton:V_waypointSelectionButtons){
                        IndexButton.enabled = true;
                    }
                    this.enabled = false;
                    V_updateCurrentWaypoint(finalpageIndex+V_scrollSpot);
                }
            };
            addComponent(button);

            //init TextBox text
            String waypointNameString = "";
            if(pageIndex+V_scrollSpot<V_globalWaypointList.size()){
                waypointNameString = V_globalWaypointList.get(pageIndex+V_scrollSpot).name;
            }
            String waypointXString = "";
            if(pageIndex+V_scrollSpot<V_globalWaypointList.size()){
                waypointXString =  Double.toString(V_globalWaypointList.get(pageIndex+V_scrollSpot).position.x);
            }
            String waypointYString = "";
            if(pageIndex+V_scrollSpot<V_globalWaypointList.size()){
                waypointYString =  Double.toString(V_globalWaypointList.get(pageIndex+V_scrollSpot).position.y);
            }
            String waypointZString = "";
            if(pageIndex+V_scrollSpot<V_globalWaypointList.size()){
                waypointZString = Double.toString(V_globalWaypointList.get(pageIndex+V_scrollSpot).position.z);
            }

            //init TextBox
            GUIComponentTextBox waypointName = new GUIComponentTextBox(this, guiLeft + 36, guiTop + 10 + 12 * V_waypointNameList.size() + VehiclePartTop, 32, 10, waypointNameString, ColorRGB.WHITE, 5) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    //TODO
                }
            };

            GUIComponentTextBox waypointX = new GUIComponentTextBox(this, guiLeft + 70, guiTop + 10 + 12 * V_waypointXList.size() + VehiclePartTop, 45, 10, waypointXString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    //TODO
                }
            };

            GUIComponentTextBox waypointY = new GUIComponentTextBox(this, guiLeft + 117, guiTop + 10 + 12 * V_waypointXList.size() + VehiclePartTop, 45, 10, waypointYString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    //TODO
                }
            };

            GUIComponentTextBox waypointZ = new GUIComponentTextBox(this, guiLeft + 164, guiTop + 10 + 12 * V_waypointXList.size() + VehiclePartTop, 45, 10, waypointZString, ColorRGB.WHITE, 7) {
                @Override
                public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control) {
                    super.handleKeyTyped(typedChar, typedCode, control);
                    //TODO
                }
            };

            addComponent(waypointName);
            addComponent(waypointX);
            addComponent(waypointY);
            addComponent(waypointZ);


            V_waypointSelectionButtons.add(button);
            V_waypointNameList.add(waypointName);
            V_waypointXList.add(waypointX);
            V_waypointYList.add(waypointY);
            V_waypointZList.add(waypointZ);
        }
        addComponent(V_PageUpButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 10 + VehiclePartTop, 20, 20, "/\\") {
            @Override
            public void onClicked(boolean leftSide) {

                scrollSpot -= 7;

            }
        });
        addComponent(V_PageDownButton = new GUIComponentButton(this, guiLeft + 225, guiTop + 74 + VehiclePartTop, 20, 20, "\\/") {
            @Override
            public void onClicked(boolean leftSide) {

                scrollSpot += 7;
            }
        });

        addComponent(V_addWaypointButton = new GUIComponentButton(this, guiLeft + 225 - 13, guiTop + 34 + VehiclePartTop, 22+13, 16, "INSERT") {
            @Override
            public void onClicked(boolean leftSide) {
                V_updateMaxWaypointIndex();
                //TODO
            }
        });
        addComponent(V_removeWaypointButton = new GUIComponentButton(this, guiLeft + 225 - 13, guiTop + 54 + VehiclePartTop, 22+13, 16, "REMOVE") {
            @Override
            public void onClicked(boolean leftSide) {
                if(currentWaypoint != null){
                    //TODO
                    V_updateCurrentWaypoint(-1);
                }
            }
        });
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        //setupComponents will be executed every time the window is resized!

        GUIComponentLabel GlobalWaypointLabel = new GUIComponentLabel(guiLeft + 10, guiTop + 2, ColorRGB.GREEN, "Global Waypoints");
        GUIComponentLabel V_GlobalWaypointLabel = new GUIComponentLabel(guiLeft + 10, guiTop + 2 + VehiclePartTop, ColorRGB.GREEN, "Flight Plan Waypoints");
        GUIComponentLabel V_isEnabled = new GUIComponentLabel(guiLeft + 130, guiTop + 2 + VehiclePartTop, ColorRGB.RED, "(Not Usable Without AIRCRAFT)");
        setupGlobalPart();

        addComponent(GlobalWaypointLabel);
        addComponent(V_GlobalWaypointLabel);

        if(vehicle != null){
            setupVehiclePart();
        }else{
            addComponent(V_isEnabled);
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        setStatesGlobal();
        if(vehicle != null)setStatesVehicle();
    }

    /**
     * Global
     *
     * */
    public void setStatesGlobal() {
        updateGlobalWaypointList();

        //avoid invalid page button state
        if(scrollSpot==0) PageUpButton.enabled = false;
        else PageUpButton.enabled = true;
        if(scrollSpot+7+1>globalWaypointList.size()) PageDownButton.enabled = false;
        else PageDownButton.enabled = true;

        for (int i = 0; i < 7; ++i){
            int finalpageIndex = i;

            GUIComponentButton button = waypointSelectionButtons.get(finalpageIndex);
            GUIComponentTextBox waypointName = waypointNameList.get(finalpageIndex);
            GUIComponentTextBox waypointX = waypointXList.get(finalpageIndex);
            GUIComponentTextBox waypointY = waypointYList.get(finalpageIndex);
            GUIComponentTextBox waypointZ = waypointZList.get(finalpageIndex);

            button.text = Integer.toString(finalpageIndex+scrollSpot+1);
            //update state
            if(finalpageIndex+scrollSpot != currentWaypointIndex)button.enabled = true;

            //update text in TextBox
            if(!waypointName.focused) {
                updateNameText(finalpageIndex);
            }else if(finalpageIndex+scrollSpot<globalWaypointList.size()){
                button.enabled = false;
                updateCurrentWaypoint(finalpageIndex+scrollSpot);
            }
            if(!waypointX.focused) {
                updateXText(finalpageIndex);
            }else if(finalpageIndex+scrollSpot<globalWaypointList.size()){
                button.enabled = false;
                updateCurrentWaypoint(finalpageIndex+scrollSpot);
            }
            if(!waypointY.focused){
                updateYText(finalpageIndex);
            }else if(finalpageIndex+scrollSpot<globalWaypointList.size()){
                button.enabled = false;
                updateCurrentWaypoint(finalpageIndex+scrollSpot);
            }
            if(!waypointZ.focused){
                updateZText(finalpageIndex);
            }else if(finalpageIndex+scrollSpot<globalWaypointList.size()){
                button.enabled = false;
                updateCurrentWaypoint(finalpageIndex+scrollSpot);
            }

            //update components visibility
            if(finalpageIndex+scrollSpot+1> globalWaypointList.size()){
                button.visible =false;
                waypointName.visible = false;
                waypointX.visible = false;
                waypointY.visible = false;
                waypointZ.visible = false;
            }else{
                button.visible = true;
                waypointName.visible = true;
                waypointX.visible = true;
                waypointY.visible = true;
                waypointZ.visible = true;
            }
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

    /**
     * Vehicle
     *
     * */
    public void setStatesVehicle() {
        V_updateGlobalWaypointList();

        //avoid invalid page button state
        if(scrollSpot==0) V_PageUpButton.enabled = false;
        else V_PageUpButton.enabled = true;
        if(scrollSpot+7+1>globalWaypointList.size()) V_PageDownButton.enabled = false;
        else V_PageDownButton.enabled = true;

        for (int i = 0; i < 7; ++i){
            int finalpageIndex = i;

            GUIComponentButton button = V_waypointSelectionButtons.get(finalpageIndex);
            GUIComponentTextBox waypointName = V_waypointNameList.get(finalpageIndex);
            GUIComponentTextBox waypointX = V_waypointXList.get(finalpageIndex);
            GUIComponentTextBox waypointY = V_waypointYList.get(finalpageIndex);
            GUIComponentTextBox waypointZ = V_waypointZList.get(finalpageIndex);

            button.text = Integer.toString(finalpageIndex+V_scrollSpot+1);
            //update state
            if(finalpageIndex+V_scrollSpot != V_currentWaypointIndex)button.enabled = true;

            //update text in TextBox
            if(!waypointName.focused) {
                V_updateNameText(finalpageIndex);
            }else if(finalpageIndex+V_scrollSpot<V_globalWaypointList.size()){
                button.enabled = false;
                V_updateCurrentWaypoint(finalpageIndex+V_scrollSpot);
            }
            if(!waypointX.focused) {
                V_updateXText(finalpageIndex);
            }else if(finalpageIndex+V_scrollSpot<V_globalWaypointList.size()){
                button.enabled = false;
                V_updateCurrentWaypoint(finalpageIndex+V_scrollSpot);
            }
            if(!waypointY.focused){
                V_updateYText(finalpageIndex);
            }else if(finalpageIndex+V_scrollSpot<V_globalWaypointList.size()){
                button.enabled = false;
                V_updateCurrentWaypoint(finalpageIndex+V_scrollSpot);
            }
            if(!waypointZ.focused){
                V_updateZText(finalpageIndex);
            }else if(finalpageIndex+V_scrollSpot<V_globalWaypointList.size()){
                button.enabled = false;
                V_updateCurrentWaypoint(finalpageIndex+V_scrollSpot);
            }

            //update components visibility
            if(finalpageIndex+V_scrollSpot+1> V_globalWaypointList.size()){
                button.visible =false;
                waypointName.visible = false;
                waypointX.visible = false;
                waypointY.visible = false;
                waypointZ.visible = false;
            }else{
                button.visible = true;
                waypointName.visible = true;
                waypointX.visible = true;
                waypointY.visible = true;
                waypointZ.visible = true;
            }
        }
    }

    public void V_updateNameText(int i){
        GUIComponentTextBox waypointName = V_waypointNameList.get(i);
        String waypointNameString = "NONE";
        if(i+scrollSpot<V_globalWaypointList.size()){
            waypointNameString = V_globalWaypointList.get(i+scrollSpot).name;
        }
        waypointName.setText(waypointNameString);
    }
    public void V_updateYText(int i){
        GUIComponentTextBox waypointY = V_waypointYList.get(i);
        String waypointYString = "NONE";
        if(i+scrollSpot<V_globalWaypointList.size()){
            waypointYString = Double.toString(V_globalWaypointList.get(i+scrollSpot).position.y);
        }
        waypointY.setText(waypointYString);
    }
    public void V_updateXText(int i){
        GUIComponentTextBox waypointX = V_waypointXList.get(i);
        String waypointXString = "NONE";
        if(i+scrollSpot<V_globalWaypointList.size()){
            waypointXString = Double.toString(V_globalWaypointList.get(i+scrollSpot).position.x);
        }
        waypointX.setText(waypointXString);
    }
    public void V_updateZText(int i){
        GUIComponentTextBox waypointZ = V_waypointZList.get(i);
        String waypointZString = "NONE";
        if(i+scrollSpot<V_globalWaypointList.size()){
            waypointZString = Double.toString(V_globalWaypointList.get(i+scrollSpot).position.z);
        }
        waypointZ.setText(waypointZString);
    }
}


