package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("For MTS to even consider something as a pack, it must have a packdefinition.json file inside of it.  This file tells MTS the packID, what files to load, and where they will be found.  This file also controls dynamic loading, which allows packs to only load specific components if specific mods or packs are present.\nNo matter what you want to put into your pack, you'll have put it in the right place, otherwise MTS won't find it.  As such, there is a specific pack folder structure for all packs, though the exact structure varies depending on how you want to lay out your assets.\nNo matter which structure you follow, there is one constant: ALL assets must be located inside 'assets/[yourpackid]/'  This is because all assets are loaded from the asset folder, and because pack assets are stored in their pack-specific folders.  After this, however, the folder structure varies based on what you chose for the fileStructure parameter.")
public class JSONPack {
    @JSONRequired
    @JSONDescription("The ID for your pack.  This must be unique!")
    public String packID;

    @JSONRequired
    @JSONDescription("A name for your pack.  Used in GUIs and creative tabs.")
    public String packName;

    @JSONDescription("The name of the item that will be the icon for the creative tab.  Leave blank to have the icon cycle through all items.")
    public String packItem;

    @JSONDescription("A number that represents the file structure of this pack.  0 is default, 1 is layered, and 2 is modular.  This is to allow flexibility in how you arrange your pack components and to make it easier to see where things are and where components are missing.  Each structure is explained in detail in the following sections.  However, there are a few things you should be aware of:<ul>" + "<li>All assets MUST be located in 'assets/[yourpackid]'  This prefix will be omitted in the follwing section for clarity, but please remember this.</li>" + "<li>Whenever you see a [===], it means that you can put the listed component into a sub-folder for easier organization.</li>" + "<li>Whenever you see a [+++] it means that the activator/blocker folder should be used at this point, if you have one set and wish to use it for the assets.  If not, you can simply omit the folder.</li>"
            + "<li>Whenever you see a [###] it means that the component-specific folder name needs to be used here.  This is based on the name of the component.  For example, vehicles go in a 'vehicles/' folder.</li>" + "<li>The main 1024x1024 instrument texture is located in 'assets/[yourpackid]/textures'. It cannot go anywhere else, even if you have sub-folders for textures.</li>" + "<li>Sounds are located in the 'assets/[yourpackid]/sounds' folder.</li>" + "</ul>" + "The default structure is the classic pack format.  Pack components are grouped into folders based on their type, and some sub-folders exist that allow for some segmentation of components.  Item textures and optional JSON models are hard-coded to the 'textures/items' and 'models/item' folders respectively.  No sub-folders are permitted anywhere except the jsondefs folder.  Therefore, the folder structure is as follows:<ul>" + "<li>Pack JSON: 'assets/[yourpackid]/[+++]/jsondefs/[###]/[===]/component.json'</li>"
            + "<li>OBJ Models: 'assets/[yourpackid]/objmodels/[###]/component .obj'</li>" + "<li>OBJ Textures: 'assets/[yourpackid]/textures/[###]/component .png'</li>" + "<li>Item Textures: 'assets/[yourpackid]/textures/items/[###]/component .png'</li>" + "<li>Item JSON: 'assets/[yourpackid]/models/item/[###]/component .json'</li>" + "</ul>" + "The layered structure is similar to the default format, except that when MTS loads a JSON in a sub-folder, it remembers that sub-folder and will load any assets assuming they are also in that sub-folder.  For example, if it finds the file 'jsondefs/vehicles/planes/mc172.json', it will realize that the json was inside the 'planes' sub-folder, and will then look for other assets, such as the model and texture, in such sub-folders.  This allows for a bit more organization in pack files than the default format, while still allowing for common asset folders with the activator/blocker system.  Therefore, the folder structure is as follows:<ul>"
            + "<li>Pack JSON: 'assets/[yourpackid]/[+++]/jsondefs/[###]/[===]/component .json'</li>" + "<li>OBJ Models: 'assets/[yourpackid]/objmodels/[###]/[===]/component .obj'</li>" + "<li>OBJ Textures: 'assets/[yourpackid]/textures/[###]/[===]/component .png'</li>" + "<li>Item Textures: 'assets/[yourpackid]/textures/items/[###]/[===]/component .png'</li>" + "<li>Item JSON: 'assets/[yourpackid]/models/item/[###]/[===]/component .json'</li>" + "</ul>"
            + "The modular structure is significantly different from the default or layered structure, in that it allows all the files for a component to be located in the same folder as that component's definition.  This includes the item texture and item json, which, to prevent name collision, must be named with the extension '_item.png' and '_item.json' respectively.  The only requirement still in effect are the component sub-folders, as MTS still needs to know what it's loading, and the folder placement is the only way for it to do that.  The modular structure, however, comes at the cost of having to duplicate components if an activator/blocker is used and there are common components between the two sub-sections.  As such, this structure is mainly designed for smaller packs that don't do dynamic loading, or packs that do dynamic loading, but don't use common assets or whose common assets would not cause a bloat in pack size.  The folder structure is as follows:<ul>"
            + "<li>Pack JSON: 'assets/[yourpackid]/[+++]/[###]/[===]/component .json'</li>" + "<li>OBJ Models: 'assets/[yourpackid]/[+++]/[###]/[===]/component .obj'</li>" + "<li>OBJ Textures: 'assets/[yourpackid]/[+++]/[###]/[===]/component .png'</li>" + "<li>Item Textures: 'assets/[yourpackid]/[+++]/[###]/[===]/component_item.png'</li>" + "<li>Item JSON: 'assets/[yourpackid]/[+++]/[###]/[===]/component_item.json'</li>" + "</ul>")
    public int fileStructure;

    @JSONDescription("If true, all skins that are added by other packs will appear in that pack's tab, not this pack.  For example, if you have a car, and another pack makes a skin for that car, then the item for that car's skin will be in that pack, not yours.")
    public boolean externalSkinsInOwnTab;

    @JSONDescription("If true, all skins that are added by this pack will appear in this pack's tab rather than the pack they are a skin for.  For example, if another pack has a car, and you make a skin for it, then the item for that car's skin will be in this pack's tab, not the one that has the car.")
    public boolean internalSkinsInOwnTab;

    @JSONDescription("A mapping of sub-directories to lists that tells MTS what packs or mods are required to load packs from the specified sub-directory.  This is a key-list mapping, where the key is the sub-directory, and the list is a list of one or more mods or packs that must be present to load files from this sub-directory.  A list with no entries implies that files from this sub-directory will always be loaded (unless blocked by a blocker).")
    public Map<String, List<String>> activators;

    @JSONDescription("Like activators, but instead MTS will block loading files from this directory if the a pack or mod in the list is loaded.  Allows for switching of loading if a mod is present from one directory to another.")
    public Map<String, List<String>> blockers;

    @JSONDescription("This list contains a list of dependents, of which one of them must be installed for your pack to be functional. If MTS detects that none of the packs in this list are present, it will block access to your creative tab and the crafting GUIs and warn the player that they are missing one of the packs.  Note that this list is a 'one of many', in that you can list multiple packs in here and as long as one is installed, the user can use your pack.  This is done to allow packs without core parts like engines or wheels to ensure that there is another pack that provides these parts, while not locking the player into a specific pack itself.")
    public List<String> dependents;
}
