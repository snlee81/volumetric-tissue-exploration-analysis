/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vteaimageprocessing.builtin;

import ij.ImagePlus;
import java.util.ArrayList;
import net.imglib2.img.Img;
import org.scijava.plugin.Plugin;
import vteaimageprocessing.AbstractImageProcessing;
import vteaimageprocessing.ImageProcessing;

/**
 *
 * @author sethwinfree
 */
@Plugin (type = ImageProcessing.class)
public class BackgroundSubtraction extends AbstractImageProcessing {
    
    
    public BackgroundSubtraction(){
        VERSION = "0.1";
        AUTHOR = "Seth Winfree";
        COMMENT = "Implements the plugin from ImageJ";
        NAME = "Background Subtraction";
        KEY = "BackgroundSubtraction";
    }

    

    @Override
    public Img getResult() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
