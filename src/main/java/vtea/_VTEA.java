package vtea;

import MicroProtocol.ProtocolManagerMulti;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.lang.Runnable;
import java.lang.Thread;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtea.services.ImageProcessingService;
import vtea.services.SegmentationService;


public class _VTEA implements PlugIn, ImageListener, ActionListener {

    public static Color BACKGROUND = new Color(204, 204, 204);
    public static Color BUTTONBACKGROUND = new Color(200, 200, 200);
    public static Color ACTIONPANELBACKGROUND = new Color(240, 240, 240);
    public static Color INACTIVETEXT = new Color(153,153,153);
    public static Color ACTIVETEXT = new Color(0,0,0);
    public static Dimension SMALLBUTTONSIZE = new Dimension(32, 32);
    public static Dimension BLOCKSETUP = new Dimension(370, 350);
    public static Dimension BLOCKSETUPPANEL = new Dimension(340, 100);
    public static String VERSION = new String("0.3");
    
    public static String[] SEGMENTATIONOPTIONS;
    public static String[] PROCESSINGOPTIONS;
    
    //public static Color ButtonBackground = new java.awt.Color(102, 102, 102);

    public static void main(String[] args) {
         
        Class<?> clazz = _VTEA.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);


        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {new ImageJ(); }
        });
    }
    

    public ProtocolManagerMulti protocolWindow;

    public void setup(String arg, ImagePlus imp1) {
      
    }

    // @Override
    @Override
public void run(String arg) {
    ImagePlus.addImageListener(this);
      
    Thread VTEA = new Thread(new Runnable(){
        
        @Override
        public void run() {
            
              protocolWindow = new ProtocolManagerMulti(); 
              protocolWindow.setVisible(true);
              SegmentationService ss = new SegmentationService();
              ImageProcessingService ips = new ImageProcessingService();
              
              List<String> ips_names = ips.getNames();
              List<String> ips_qualifiedNames = ips.getQualifiedName();
               
              List<String> ss_names = ss.getNames();
              
              System.out.println("Image Processing Plugins loaded: ");
              for(int i = 0; i < ips_names.size(); i++){                 
                  System.out.println("Found class: " + ips_names.get(i));
                  try {
                      Object o = Class.forName(ips_qualifiedNames.get(i)).newInstance();
                      System.out.println("Instantiated: " + o.getClass().getName());
                  } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                      Logger.getLogger(_VTEA.class.getName()).log(Level.SEVERE, null, ex);
                  }
              }
              
              SEGMENTATIONOPTIONS = ips_names.toArray(new String[ips_names.size()]);
              
              System.out.println("Segementation Plugins loaded: ");
              for(int i = 0; i < ss_names.size(); i++){                 
                  System.out.println(ss_names.get(i));
              }
              
              SEGMENTATIONOPTIONS = ss_names.toArray(new String[ss_names.size()]);
              
        }
    });
    
    VTEA.setPriority(8);
    System.out.println("New VTEA thread in: "+VTEA.getThreadGroup()+ " at priority: " + VTEA.getPriority());
    VTEA.start();
    

  
}   

    @Override
    public void actionPerformed(java.awt.event.ActionEvent evt) {

    }

    @Override
    public void imageOpened(ImagePlus imp) {
        protocolWindow.UpdateImageList();     
    }

    @Override
    public void imageClosed(ImagePlus imp) {
        protocolWindow.UpdateImageList();
      
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        protocolWindow.UpdateImageList();
        
    }
}
