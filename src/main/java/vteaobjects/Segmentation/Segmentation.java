/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vteaobjects.Segmentation;

import ij.ImagePlus;
import ij.ImageStack;
import java.util.ArrayList;
import java.util.List;
import vtea.VTEAModule;
import vteaobjects.MicroObject;

/**
 *
 * @author winfrees
 */
public interface Segmentation extends VTEAModule {
    
    public ImagePlus getSegmentation();
    
    public ArrayList<MicroObject> getObjects();
    
    public void process(ImageStack[] is, List details, boolean calculate);
    
    public String runImageJMacroCommand(String str);
    
    public String getVersion();
    
    public String getAuthor();
    
    public String getComment();
    
    public void sendProgressComment();
    
    public String getProgressComment();
    
    public boolean setOptions(ArrayList al);
    
    public ArrayList getOptions();
    
    public void setBuildTool(ArrayList al);
}
