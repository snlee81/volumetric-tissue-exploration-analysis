/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vteaimageprocessing.builtin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import java.util.ArrayList;
import javax.swing.JTextField;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;
import vteaimageprocessing.AbstractImageProcessing;
import vteaimageprocessing.ImageProcessing;

/**
 *
 * @author sethwinfree
 */
@Plugin (type = ImageProcessing.class)
public class Denoise extends AbstractImageProcessing {
       
    public Denoise(){
    VERSION = "0.1";
    AUTHOR = "Seth Winfree";
    COMMENT = "Implements the plugin from ImageJ";
    NAME = "Denoise";
    KEY = "Denoise";
    }
    
    @Override
    public boolean process(ArrayList al, ImagePlus imp) {
        
        int channel = (Integer)al.get(1);
        JTextField radius = (JTextField) al.get(3);
        
        
        ChannelSplitter cs = new ChannelSplitter();
        
        ImageStack is;
        
        
        //Img<RealType> wrapImg = ImageJFunctions.wrapReal(imp);
        
        is = cs.getChannel(imp, channel+1);
        
        //for(int n = 1; n <= is.getSize(); n++){
            IJ.run(new ImagePlus("",is), "Median...", "radius="+radius.getText()+" stack");
            
                imgResult = ImageJFunctions.wrapReal(imp);
            
        return true;
    }




    @Override
    public Img getPreview() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String runImageJMacroCommand(String str) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendProgressComment() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getProgressComment() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
