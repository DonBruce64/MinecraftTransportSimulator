package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

public class GUIBooklet extends AGUIBase {
    //Buttons and text.
    private GUIComponentButton leftButton;
    private GUIComponentButton rightButton;
    private GUIComponentButton contentsButton;
    private final List<List<GUIComponentLabel>> pageTextLabels = new ArrayList<>();
    private final List<GUIComponentButton> contentsButtons = new ArrayList<>();

    //Item properties.
    private final ItemItem booklet;
    private final int totalPages;

    public GUIBooklet(ItemItem booklet) {
        super();
        this.booklet = booklet;
        this.totalPages = booklet.definition.booklet.disableTOC ? 1 + booklet.definition.booklet.pages.size() : 2 + booklet.definition.booklet.pages.size();
    }

    @Override
    public void setupComponents() {
        super.setupComponents();
        pageTextLabels.clear();

        //Page navigation buttons.
        //We auto-calculate the texture size from here based on the GUI size.
        //This is needed to tell the buttons what texture size they are using.
        addComponent(leftButton = new GUIComponentButton(guiLeft + 20, guiTop + 150, 20, 20, 0, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                --booklet.pageNumber;
            }
        });
        addComponent(rightButton = new GUIComponentButton(guiLeft + booklet.definition.booklet.textureWidth - 40, guiTop + 150, 20, 20, 20, 196, 20, 20) {
            @Override
            public void onClicked(boolean leftSide) {
                ++booklet.pageNumber;
            }
        });

        //Title text labels.
        List<GUIComponentLabel> titleLabels = new ArrayList<>();
        for (JSONText text : booklet.definition.booklet.titleText) {
            GUIComponentLabel titleLabel = new GUIComponentLabel(guiLeft + (int) text.pos.x, guiTop + (int) text.pos.y, text.color, text.defaultText, TextAlignment.values()[text.renderPosition], text.scale, text.wrapWidth, text.fontName, text.autoScale);
            titleLabels.add(titleLabel);
            addComponent(titleLabel);
        }
        pageTextLabels.add(titleLabels);

        //Contents text labels and buttons.
        if (!booklet.definition.booklet.disableTOC) {
            //TOC page label.
            GUIComponentLabel contentsLabel = new GUIComponentLabel(guiLeft + booklet.definition.booklet.textureWidth / 4 - 20, guiTop + 25, ColorRGB.BLACK, "CONTENTS");
            addComponent(contentsLabel);
            List<GUIComponentLabel> contentsLabels = new ArrayList<>();
            contentsLabels.add(contentsLabel);
            pageTextLabels.add(contentsLabels);

            //TOC buttons with text for pages.
            contentsButtons.clear();
            int leftSideOffset = guiLeft + 20;
            int rightSideOffset = guiLeft + booklet.definition.booklet.textureWidth / 2 + 20;
            for (int i = 0; i < booklet.definition.booklet.pages.size(); ++i) {
                GUIComponentButton contentsHyperlink = new GUIComponentButton(i < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10 * (i % 10), 110, 10, (i + 1) + ": " + booklet.definition.booklet.pages.get(i).title, false, booklet.definition.booklet.pages.get(i).pageText.get(0).color, false) {
                    @Override
                    public void onClicked(boolean leftSide) {
                        booklet.pageNumber = contentsButtons.indexOf(this) + 2;
                    }
                };
                contentsButtons.add(contentsHyperlink);
                addComponent(contentsHyperlink);
            }

            //Button on other pages to go back to TOC.
            addComponent(contentsButton = new GUIComponentButton(leftButton.constructedX + leftButton.width, guiTop + 150, 20, 20, 40, 196, 20, 20) {
                @Override
                public void onClicked(boolean leftSide) {
                    booklet.pageNumber = 1;
                }
            });
        }

        //Regular page labels.
        for (BookletPage page : booklet.definition.booklet.pages) {
            List<GUIComponentLabel> pageLabels = new ArrayList<>();
            for (JSONText text : page.pageText) {
                try {
                    GUIComponentLabel pageLabel = new GUIComponentLabel(guiLeft + (int) text.pos.x, guiTop + (int) text.pos.y, text.color, text.defaultText, TextAlignment.values()[text.renderPosition], text.scale, text.wrapWidth, text.fontName, text.autoScale);
                    pageLabels.add(pageLabel);
                    addComponent(pageLabel);
                } catch (Exception e) {
                    int pageNumber = -1;
                    for (byte i = 0; i < booklet.definition.booklet.pages.size(); ++i) {
                        if (booklet.definition.booklet.pages.get(i).equals(page)) {
                            pageNumber = i + 1;
                        }
                    }
                    InterfaceManager.coreInterface.logError("An error was encountered when creating booklet page #" + pageNumber);
                    InterfaceManager.coreInterface.logError(e.getMessage());
                }
            }
            pageTextLabels.add(pageLabels);
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        //Set the navigation button states.
        leftButton.visible = booklet.pageNumber > 0;
        rightButton.visible = booklet.pageNumber + 1 < totalPages;

        //Check the mouse to see if it updated and we need to change pages.
        int wheelMovement = InterfaceManager.inputInterface.getTrackedMouseWheel();
        if (wheelMovement < 0 && rightButton.visible) {
            ++booklet.pageNumber;
        } else if (wheelMovement > 0 && leftButton.visible) {
            --booklet.pageNumber;
        }

        //Set the visible labels based on the current page.
        for (int i = 0; i < pageTextLabels.size(); ++i) {
            for (GUIComponentLabel label : pageTextLabels.get(i)) {
                label.visible = booklet.pageNumber == i;
            }
        }

        //Set the TOC buttons visible if we're on the TOC page.
        for (GUIComponentButton button : contentsButtons) {
            button.visible = booklet.pageNumber == 1 && !booklet.definition.booklet.disableTOC;
        }

        //Set the TOC button to be visible on other pages.
        if (contentsButton != null) {
            contentsButton.visible = booklet.pageNumber > 1;
        }
    }

    @Override
    public int getWidth() {
        return booklet.definition.booklet.textureWidth;
    }

    @Override
    public int getHeight() {
        return booklet.definition.booklet.textureHeight;
    }

    @Override
    protected String getTexture() {
        if (booklet.pageNumber == 0) {
            return booklet.definition.booklet.coverTexture;
        } else if (!booklet.definition.booklet.disableTOC) {
            if (booklet.pageNumber == 1) {
                return booklet.definition.booklet.pages.get(0).pageTexture;
            } else {
                return booklet.definition.booklet.pages.get(booklet.pageNumber - 2).pageTexture;
            }
        } else {
            return booklet.definition.booklet.pages.get(booklet.pageNumber - 1).pageTexture;
        }
    }
}