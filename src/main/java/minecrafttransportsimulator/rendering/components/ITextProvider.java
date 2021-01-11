package minecrafttransportsimulator.rendering.components;

import java.util.LinkedHashMap;

import minecrafttransportsimulator.jsondefs.JSONText;

/**Interface for classes that need to have text rendered on them and their models.
 * Designed to be server-safe and only query the class implementing this interface for information
 * about the text and state rather than having that class feed it all back to a non-existent system.
 *
 * @author don_bruce
 */
public interface ITextProvider{
    
    /**
   	 *  Returns a map of textObjects corresponding to the strings that are the
   	 *  current states of those objects.  If there are no textObjects on this
   	 *  provider, return null;
   	 */
    public LinkedHashMap<JSONText, String> getText();
    
    /**
   	 *  Returns a string that represents this provider's secondary text color.  If this color is set,
   	 *  and text is told to render from this provider, and that text is told to use this color, then it will.
   	 *  Otherwise, the text will use its default color.
   	 */
    public String getSecondaryTextColor();
    
    /**
   	 *  Returns true if this provider is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit();
}
