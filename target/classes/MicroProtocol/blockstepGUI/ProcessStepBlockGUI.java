/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package MicroProtocol.blockstepGUI;

import MicroProtocol.ProtocolManagerMulti;
import MicroProtocol.UtilityMethods;
import MicroProtocol.listeners.DeleteBlockListener;
import MicroProtocol.listeners.MicroBlockSetupListener;
import MicroProtocol.listeners.RebuildPanelListener;
import MicroProtocol.setup.MicroBlockProcessSetup;
import MicroProtocol.setup.MicroBlockSetup;
import ij.CompositeImage;
import ij.IJ;
import static ij.IJ.COLOR;
import ij.ImagePlus;
import ij.plugin.frame.ContrastAdjuster;
import ij.process.LUT;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import vteaimageprocessing.MicroProtocolPreProcessing;

/**
 *
 * @author vinfrais
 */
public class ProcessStepBlockGUI extends Object implements Cloneable, MicroBlockSetupListener{

        protected JPanel step = new JPanel();
        Font PositionFont = new Font("Arial", Font.PLAIN, 18);
        Font ProcessFont = new Font("Arial", Font.BOLD, 12);
        Font CommentFont = new Font("Arial", Font.ITALIC, 14);
        JLabel Position = new JLabel();
        public JLabel Comment = new JLabel("Block by Block");
        JLabel Process = new JLabel("First things first");
        String ProcessString;
        int processChannel;
        boolean ProcessTypeSet = false;
        int position;
        int type;
        ArrayList<String> Channels;
        Color BlockColor;
        //boolean multiple;
        
        JWindow thumb = new JWindow();
        
        ImagePlus ThumbnailImage;
        ImagePlus OriginalImage;
        ImagePlus PreviewThumbnailImage;
        
        Boolean updatePreviewImage = true;
        
        MicroBlockSetup mbs;

        private ArrayList settings;
        
        private ArrayList ProtocolAll;
        
         public ArrayList <RebuildPanelListener> rebuildpanelisteners = new ArrayList <RebuildPanelListener>();
         public ArrayList <DeleteBlockListener> deleteblocklisteners = new ArrayList <DeleteBlockListener>();
   

        public ProcessStepBlockGUI() {
           //BuildStepBlock("Empty Step", "", Color.LIGHT_GRAY, false, );
        }

        public ProcessStepBlockGUI(String ProcessText, String CommentText, Color BlockColor, boolean multiple, ImagePlus ThumbnailImage, ImagePlus OriginalImage, ArrayList<String> Channels, int type, ArrayList<ProcessStepBlockGUI> protocol, int position) {
            BuildStepBlock(ProcessText, CommentText, BlockColor, multiple, ThumbnailImage, OriginalImage, Channels, type, protocol, position);
        }

        ;

        
        protected void BuildStepBlock(String ProcessText, String CommentText, Color BlockColor, boolean multiple, ImagePlus ThumbnailImage, ImagePlus OriginalImage, ArrayList<String> Channels, final int type, ArrayList<ProcessStepBlockGUI> protocol, final int position) {

            this.ThumbnailImage = ThumbnailImage;
            this.OriginalImage = OriginalImage;
            //this.PreviewThumbnailImage = ThumbnailImage.createImagePlus();
            this.Channels = (ArrayList)Channels.clone();
            this.Channels.add("All");
            this.position = position;
            this.type = type;
            this.BlockColor = BlockColor;
            
            this.ProcessString = ProcessText;
            
            this.ProtocolAll = protocol;
            
            if(ProcessString.length() > 30){
              Process.setText(ProcessString.substring(0, 40)+"...");
              Process.setToolTipText(ProcessString);
            }else{
              Process.setText(ProcessText);  
            }

            Comment.setText(CommentText);
           
            step.setBackground(BlockColor);

            //need max size set here
            Position.setText(position + ".");

            Position.setFont(PositionFont);

            if (Process.getText().length() > 12) {
                ProcessFont = new Font("Arial", Font.BOLD, 10);
            }
            if (Comment.getText().length() < 12) {
                CommentFont = new Font("Arial", Font.BOLD, 12);
            }

            Process.setFont(ProcessFont);
            Comment.setFont(CommentFont);

            mbs = new MicroProtocol.setup.MicroBlockProcessSetup(position, Channels, protocol, OriginalImage);

            mbs.setVisible(false);
            mbs.addMicroBlockSetupListener(this);
            
            //MicroProtocolPreProcessing previewEngine;

            JButton DeleteButton = new JButton();
            DeleteButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    //deleteProcessStep(position);
                    deleteStep(type, position);
                    

                }
            });

            JButton EditButton = new JButton();
            EditButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    mbs.setVisible(true);
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            DeleteButton.setSize(20, 20);
            DeleteButton.setBackground(vtea._VTEA.BUTTONBACKGROUND);
            DeleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit-delete-6_16.png")));

            EditButton.setSize(20, 20);
            EditButton.setBackground(vtea._VTEA.BUTTONBACKGROUND);
            EditButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit-4.png")));

            step.setSize(205, 20);
            step.setBorder(javax.swing.BorderFactory.createEtchedBorder());

            step.setLayout(new GridBagLayout());
            GridBagConstraints layoutConstraints = new GridBagConstraints();

            layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
            layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
            layoutConstraints.gridx = 0;
            layoutConstraints.gridy = 0;
            layoutConstraints.weightx = 1;
            layoutConstraints.weighty = 1;
            layoutConstraints.ipadx = 10;

            step.add(Position, layoutConstraints);

            layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
            layoutConstraints.anchor = GridBagConstraints.CENTER;
            layoutConstraints.gridx = 1;
            layoutConstraints.gridy = 0;
            layoutConstraints.weightx = 20;
            layoutConstraints.weighty = 20;
            step.add(Process, layoutConstraints);

            layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
            layoutConstraints.gridx = 1;
            layoutConstraints.gridy = 1;
            layoutConstraints.weightx = 20;
            layoutConstraints.weighty = 20;
            step.add(Comment, layoutConstraints);

            if (position > 1) {

                layoutConstraints.fill = GridBagConstraints.BOTH;
                layoutConstraints.anchor = GridBagConstraints.EAST;
                layoutConstraints.gridx = 2;
                layoutConstraints.gridy = 0;
                layoutConstraints.weightx = -1;
                layoutConstraints.weighty = -1;
                layoutConstraints.ipadx = -1;
                layoutConstraints.ipady = -1;
                step.add(DeleteButton, layoutConstraints);
                layoutConstraints.fill = GridBagConstraints.BOTH;
                layoutConstraints.anchor = GridBagConstraints.EAST;
                layoutConstraints.gridx = 2;
                layoutConstraints.gridy = 1;
                layoutConstraints.weightx = -1;
                layoutConstraints.weighty = -1;
                layoutConstraints.ipadx = -1;
                layoutConstraints.ipady = -1;
                step.add(EditButton, layoutConstraints);

            }

            step.addMouseListener(new java.awt.event.MouseListener() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                }

                ;
            @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    //thumb.setVisible(false);
                }

                ;
            @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    thumb.setVisible(false);
                }
                
                ;
            @Override
                public void mousePressed(java.awt.event.MouseEvent evt) {
                    if(!SwingUtilities.isRightMouseButton(evt)){
                        showThumbnail(evt.getXOnScreen(), evt.getYOnScreen());
                }
                }
                ;
            @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
//                    if(SwingUtilities.isRightMouseButton(evt) && position == 1) { 
//                        IJ.log("Rightclick detected on step: " + position);       
//                        mfm.show(evt.getComponent(), evt.getX(), evt.getY());
//                          }
                }
            ;

        }

        );

        //step.
            


        }
        
//        private void deleteStep(int type, int position) {           
//            this.notifyDeleteBlockListeners(type, position);
//            this.notifyRebuildPanelListeners(type);
//            
//        }
        
        
        protected void showThumbnail(int x, int y) {
                   thumb.setSize(255, 255);
                   
                   if(this.OriginalImage.getWidth() < 255){
                       thumb.setSize(OriginalImage.getWidth(), OriginalImage.getHeight());
                   }

                   if(position > 1 && updatePreviewImage){
                       
                       ThumbnailImage = previewThumbnail(OriginalImage.duplicate());
                       
                       //ThumbnailImage.show();
                      // ThumbnailImage.setSlice(ThumbnailImage.getNSlices()/2);
                       
                       thumb.add(new ImagePanel(ThumbnailImage.getImage()));
                       //ThumbnailImage.hide();
//                       JLabel slice = new JLabel("Slice: " + ThumbnailImage.getSlice());
//                       slice.setBackground(Color.YELLOW);
//                       thumb.add(slice);
                        
                       updatePreviewImage = false;
                   }else{
                       
                       thumb.add(new ImagePanel(ThumbnailImage.getImage()));
                   }

                    thumb.setLocation(x, y);
                    thumb.setVisible(true);
        
        }         
                    
        
        private ImagePlus previewThumbnail(ImagePlus imp) {
            
        ArrayList options = new ArrayList();
        
        options.add(settings);
        
        System.out.println("imageProcessing: " + options);
        
        MicroProtocolPreProcessing previewEngine = new MicroProtocolPreProcessing(imp, options);
        previewEngine.ProcessPreviewImage();
        
        //previewEngine.getPreview().show();
        
        return previewEngine.getPreview();
        }

    protected void deleteStep(int type, int position) {
        this.notifyDeleteBlockListeners(type, position);
        this.notifyRebuildPanelListeners(type);

    }

      
    public void setPosition(int n) {
            position = n;
            Position.setText(position + ".");
        }

       
    public JPanel getPanel() {
            return step;
        }

       
    public int getPosition() {
            return position;
        }

      
    public ArrayList getVariables() {
            return settings;
        }
        
    public void addRebuildPanelListener(RebuildPanelListener listener) {
        rebuildpanelisteners.add(listener);
    }

    protected void notifyRebuildPanelListeners(int type) {
        for (RebuildPanelListener listener : rebuildpanelisteners) {
            listener.rebuildPanel(type);
        }
    }
    
    public void addDeleteBlockListener(DeleteBlockListener listener) {
        deleteblocklisteners.add(listener);
    }

    protected void notifyDeleteBlockListeners(int type, int position) {
        for (DeleteBlockListener listener : deleteblocklisteners) {
            listener.deleteBlock(type, position);
        }
    }
    
    public int getChannel(){
        
       return processChannel;
    }
    
    
    

    @Override
    public Object clone() throws CloneNotSupportedException {
        
        super.clone();
        
        ProcessStepBlockGUI Copy = new ProcessStepBlockGUI(Process.getText(), Comment.getText(), this.BlockColor, false, this.ThumbnailImage.duplicate(), OriginalImage.duplicate(), this.Channels, this.type, this.ProtocolAll, this.position);
        
        Copy.mbs = (MicroBlockProcessSetup)this.mbs.clone();
        Copy.mbs.MicroBlockSetupListeners.clear();
        Copy.mbs.addMicroBlockSetupListener(Copy);
        
        Copy.settings = this.settings;
        
        
         ArrayList <RebuildPanelListener> rebuildpanelisteners = new ArrayList <RebuildPanelListener> ();
         ArrayList <DeleteBlockListener> deleteblocklisteners = new ArrayList <DeleteBlockListener> (); 

         
        
         return Copy;
    }

      
        @Override
        public void onChangeSetup(ArrayList al) {

            Process.setText(al.get(0).toString());
            processChannel = (Integer)al.get(1);
            Comment.setText("On channel: " + ((Integer)al.get(1)+1));

            notifyRebuildPanelListeners(ProtocolManagerMulti.PROCESS);
            
            this.settings = al;
            updatePreviewImage = true;
            
        }
//
//        @Override
//        public void newBatchFileList(int[] batchfileindices) {
//           
//        }

    }
