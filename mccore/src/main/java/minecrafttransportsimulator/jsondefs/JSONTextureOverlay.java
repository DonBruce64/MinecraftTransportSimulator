package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

/**
 * Definition for an animated texture layer rendered over a model's normal texture.
 */
public class JSONTextureOverlay {
    @JSONRequired
    @JSONDescription("The texture to render over the model.  This may be a pack-relative texture name (without the .png suffix), or a full texture location in the format [packID:path/to/texture.png].  On OBJ models, the overlay's native PNG dimensions are aligned to the normal texture's pixel grid, so a smaller image acts as a smaller decal rather than being stretched over the full canvas.  Pixels translated beyond a texture edge become transparent rather than wrapping to the opposite edge.")
    public String texture;

    @JSONRequired
    @JSONDescription("The initial position of the overlay's top-left corner in normal-texture pixels.  [0, 0, 0] is the normal texture's top-left corner.  Positive X moves the overlay right and positive Y moves it down.  Z is reserved for future use and is currently ignored.")
    public Point3D centerPoint;

    @JSONRequired
    @JSONDescription("Animations for this overlay.  Translation axes are measured in texture pixels, visibility animations show or hide the layer, and inhibitor/activator animations may control the sequence.  Rotation and scaling animations are currently ignored.  Use an empty list for an always-visible static overlay.")
    public List<JSONAnimationDefinition> animations;

    @JSONDescription("If true, this overlay ignores world and directional lighting and is rendered at full brightness.")
    public boolean isBright;

    @JSONDescription("If true, visibility-animation values between clampMin and clampMax smoothly change overlay opacity instead of switching visibility abruptly.")
    public boolean blendedAnimations;
}
