package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityTrafficSignalController;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.tileentities.PacketTrafficSignalControllerChange;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class GUITrafficSignalController extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/wide_blank.png");	
	
	//Global variables.
	private int guiLeft;
	private int guiTop;
	private boolean orientedOnX = false;
	private boolean triggerMode = false;
	private final List<BlockPos> trafficSignalLocations = new ArrayList<BlockPos>();
	private final List<GuiTextField> textList = new ArrayList<GuiTextField>();
	
	//Buttons.
	private GuiButton scanButton;
	private GuiButton orientationButton;
	private GuiButton modeButton;
	private GuiButton confirmButton;
	
	//Input boxes
	private GuiTextField scanDistanceText;
	private GuiTextField greenMainTimeText;
	private GuiTextField greenCrossTimeText;
	private GuiTextField yellowTimeText;
	private GuiTextField allRedTimeText;
	
	private final TileEntityTrafficSignalController signalController;
	
	public GUITrafficSignalController(TileEntityTrafficSignalController clicked){
		this.signalController = clicked;
		this.allowUserInput=true;
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 192)/2;
		
		buttonList.add(scanButton = new GuiButton(0, guiLeft + 25, guiTop + 30, 200, 20, I18n.format("gui.trafficsignalcontroller.scan")));
		buttonList.add(orientationButton = new GuiButton(0, guiLeft + 125, guiTop + 75, 100, 20, ""));
		buttonList.add(modeButton = new GuiButton(0, guiLeft + 125, guiTop + 95, 100, 20, ""));
		buttonList.add(confirmButton = new GuiButton(0, guiLeft + 25, guiTop + 165, 80, 20, I18n.format("gui.trafficsignalcontroller.confirm")));
		
		textList.add(scanDistanceText = new GuiTextField(0, fontRenderer, guiLeft + 180, guiTop + 15, 40, 10));
		scanDistanceText.setText("25");
		scanDistanceText.setMaxStringLength(2);
		
		textList.add(greenMainTimeText = new GuiTextField(0, fontRenderer, guiLeft + 180, guiTop + 120, 40, 10));
		greenMainTimeText.setText("20");
		greenMainTimeText.setMaxStringLength(3);
		
		textList.add(greenCrossTimeText = new GuiTextField(0, fontRenderer, guiLeft + 180, guiTop + 130, 40, 10));
		greenCrossTimeText.setText("10");
		greenCrossTimeText.setMaxStringLength(3);
		
		textList.add(yellowTimeText = new GuiTextField(0, fontRenderer, guiLeft + 180, guiTop + 140, 40, 10));
		yellowTimeText.setText("2");
		yellowTimeText.setMaxStringLength(1);
		
		textList.add(allRedTimeText = new GuiTextField(0, fontRenderer, guiLeft + 180, guiTop + 150, 40, 10));
		allRedTimeText.setText("1");
		allRedTimeText.setMaxStringLength(1);
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		//Background.
		this.mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 192);
		
		//Scan system.
		scanButton.enabled = true;
		scanButton.drawButton(mc, mouseX, mouseY, 0);
		fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.scandistance"), guiLeft + 30, guiTop + 15, Color.WHITE.getRGB());
		scanDistanceText.setVisible(true);
		scanDistanceText.drawTextBox();
		
		
		//Scan results 
		fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.scanfound"), guiLeft + 30, guiTop + 60, Color.WHITE.getRGB());
		RenderHelper.enableGUIStandardItemLighting();
		itemRender.renderItemAndEffectIntoGUI(new ItemStack(MTSRegistry.trafficSignal), guiLeft + 120, guiTop + 55);
		fontRenderer.drawString(" X " + trafficSignalLocations.size(), guiLeft + 135, guiTop + 60, trafficSignalLocations.isEmpty() ? Color.RED.getRGB() : Color.WHITE.getRGB());
		
		//Controls
		if(!trafficSignalLocations.isEmpty()){
			//Orientation
			fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.primary"), guiLeft + 30, guiTop + 80, Color.WHITE.getRGB());
			orientationButton.enabled = true;
			orientationButton.displayString = orientedOnX ? "X" : "Z";
			orientationButton.drawButton(mc, mouseX, mouseY, 0);
			
			//Mode
			fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.signalmode"), guiLeft + 30, guiTop + 100, Color.WHITE.getRGB());
			modeButton.enabled = true;
			modeButton.displayString = I18n.format("gui.trafficsignalcontroller." + (triggerMode ? "modetrigger" : "modetime"));
			modeButton.drawButton(mc, mouseX, mouseY, 0);
			
			//Green time
			if(!triggerMode){
				fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.greenmaintime"), guiLeft + 30, guiTop + 120, Color.WHITE.getRGB());
				greenMainTimeText.setVisible(true);
				greenMainTimeText.drawTextBox();
			}else{
				greenMainTimeText.setVisible(false);
			}
			fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.greencrosstime"), guiLeft + 30, guiTop + 130, Color.WHITE.getRGB());
			greenCrossTimeText.setVisible(true);
			greenCrossTimeText.drawTextBox();
			
			//Yellow time
			fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.yellowtime"), guiLeft + 30, guiTop + 140, Color.WHITE.getRGB());
			yellowTimeText.setVisible(true);
			yellowTimeText.drawTextBox();
			
			//Red time
			fontRenderer.drawStringWithShadow(I18n.format("gui.trafficsignalcontroller.allredtime"), guiLeft + 30, guiTop + 150, Color.WHITE.getRGB());
			allRedTimeText.setVisible(true);
			allRedTimeText.drawTextBox();
			
			//Confirm
			confirmButton.enabled = true;
			confirmButton.drawButton(mc, mouseX, mouseY, 0);
		}else{
			orientationButton.enabled = false;
			modeButton.enabled = false;
			greenMainTimeText.setVisible(false);
			greenCrossTimeText.setVisible(false);
			yellowTimeText.setVisible(false);
			allRedTimeText.setVisible(false);
			confirmButton.enabled = false;
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(scanButton)){
			trafficSignalLocations.clear();
			int scanDistance = Integer.valueOf(scanDistanceText.getText());
			for(int i=signalController.getPos().getX()-scanDistance; i<=signalController.getPos().getX()+scanDistance; ++i){
				for(int j=signalController.getPos().getY()-scanDistance; j<=signalController.getPos().getY()+scanDistance; ++j){
					for(int k=signalController.getPos().getZ()-scanDistance; k<=signalController.getPos().getZ()+scanDistance; ++k){
						BlockPos pos = new BlockPos(i, j, k);
						Block block = signalController.getWorld().getBlockState(pos).getBlock();
						if(block.equals(MTSRegistry.trafficSignal)){
							trafficSignalLocations.add(pos);
						}
					}
				}
			}
		}else if(buttonClicked.equals(orientationButton)){
			orientedOnX = !orientedOnX;
		}else if(buttonClicked.equals(modeButton)){
			triggerMode = !triggerMode;
		}else if(buttonClicked.equals(confirmButton)){
			signalController.orientedOnX = this.orientedOnX;
			signalController.triggerMode = this.triggerMode;
			//Convert from seconds to ticks for the render system.
			signalController.greenMainTime = Integer.valueOf(this.greenMainTimeText.getText())*20;
			signalController.greenCrossTime = Integer.valueOf(this.greenCrossTimeText.getText())*20;
			signalController.yellowTime = Integer.valueOf(this.yellowTimeText.getText())*20;
			signalController.allRedTime = Integer.valueOf(this.allRedTimeText.getText())*20;
			signalController.trafficSignalLocations.clear();
			signalController.trafficSignalLocations.addAll(this.trafficSignalLocations);
			MTS.MTSNet.sendToServer(new PacketTrafficSignalControllerChange(signalController));
			mc.player.closeScreen();
		}
	}
	
	
    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException{
    	super.mouseClicked(x, y, button);
    	for(GuiTextField box : textList){
    		if(box.getVisible()){
    			box.mouseClicked(x, y, button);
    		}
    	}
    }
	
    @Override
    protected void keyTyped(char key, int bytecode) throws IOException {
    	super.keyTyped(key, bytecode);
    	//Keys 2-11 are numbers per LWJGL.
    	//Key 14 is backspace, 211 is delete
    	if(bytecode!=1 && (bytecode >=2 && bytecode <= 11) || bytecode == 14 || bytecode == 211){
    		for(GuiTextField box : textList){
        		if(box.isFocused()){
        			box.textboxKeyTyped(key, bytecode);
        		}
        	}
    	}else if(bytecode == 18){
    		//Pressed e, exit GUI.
    		super.keyTyped(key, 1);
    	}
    }
}
