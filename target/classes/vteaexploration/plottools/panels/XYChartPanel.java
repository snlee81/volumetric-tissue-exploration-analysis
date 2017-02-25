/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vteaexploration.plottools.panels;

import ij.ImagePlus;
import ij.gui.RoiListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.JFrame;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import vteaexploration.listeners.UpdatePlotWindowListener;
import vteaobjects.MicroObject;
import vteaobjects.MicroObjectModel;

/**
 *
 * @author vinfrais
 */
public class XYChartPanel implements RoiListener {

    private static final String title = "XY Chart";
    private static boolean ImageGate = false;
    
    public static double xMin = -1;
    public static double xMax = -1;
    public static double yMin = -1;
    public static double yMax = -1;
    
    public static boolean globalAxes = false;


    public static int XAXIS = 1;
    public static int YAXIS = 2;

    static Color ZEROPERCENT = new Color(0, 0, 0);
    static Color TENPERCENT = new Color(0, 0, 82);
    static Color TWENTYPERCENT = new Color(61, 0, 178);
    static Color THIRTYPERCENT = new Color(122, 0, 227);
    static Color FORTYPERCENT = new Color(178, 0, 136);
    static Color FIFTYPERCENT = new Color(213, 27, 45);
    static Color SIXTYPERCENT = new Color(249, 95, 0);
    static Color SEVENTYPERCENT = new Color(255, 140, 0);
    static Color EIGHTYPERCENT = new Color(255, 175, 0);
    static Color NINETYPERCENT = new Color(255, 190, 0);
    static Color ALLPERCENT = new Color(255, 250, 50);
    static Color IMAGEGATE = new Color(0, 255, 0);
    private ChartPanel chartPanel;
    private List plotValues = new ArrayList();
    private ArrayList<MicroObject> ImageGateOverlay = new ArrayList<MicroObject>();
    private ArrayList<UpdatePlotWindowListener> UpdatePlotWindowListeners = new ArrayList<UpdatePlotWindowListener>();
    private ImagePlus impoverlay;

    private int size = 4;
    private int xValues;
    private int yValues;
    private int lValues;
    private String xValuesText;
    private String yValuesText;
    private String lValuesText;
    private boolean imageGate;
    
    private Color imageGateOutline;
    public XYChartPanel() {

    }

    public XYChartPanel(List li, int x, int y, int l, String xText, String yText, String lText, int size, ImagePlus ip, boolean imageGate, Color imageGateColor) {
        //Roi.addRoiListener(this);

        impoverlay = ip;
        this.imageGate = imageGate;
        imageGateOutline = imageGateColor;
        plotValues = li;
        this.size = size;
        xValues = x;
        yValues = y;
        lValues = l;
        xValuesText = xText;
        yValuesText = yText;
        lValuesText = lText;

        process(xValues, yValues, lValues, xValuesText, yValuesText, lValuesText);
    }

    public void process(int x, int y, int l, String xText, String yText, String lText) {

        chartPanel = createChart(x, y, l, xText, yText, lText, imageGateOutline);
        JFrame f = new JFrame(title);
        f.setTitle(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout(0, 5));
        f.add(chartPanel, BorderLayout.CENTER);

        chartPanel.setOpaque(false);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setPreferredSize(new Dimension(550, 485));
        chartPanel.setBackground(new Color(0, 0, 0, 0));
        chartPanel.revalidate();
        chartPanel.repaint();
        //chartPanel.set

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {
                chartPanel.getParent().repaint();
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });

        chartPanel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                chartPanel.getParent().repaint();

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                chartPanel.getParent().repaint();
            }
        });

        chartPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void mouseExited(MouseEvent e) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        });
        f.pack();
    }

    private ChartPanel createChart(int x, int y, int l, String xText, String yText, String lText, Color imageGateColor) {

        XYShapeRenderer renderer = new XYShapeRenderer();
        XYShapeRenderer rendererGate = new XYShapeRenderer();
        
        PaintScaleLegend psl = new PaintScaleLegend(new LookupPaintScale(0, 100, new Color(0, 0, 0)), new NumberAxis(""));
        
        if(l > 0){
        double max = getMaximumOfData((ArrayList) plotValues.get(1), l);
        double min = this.getMinimumOfData((ArrayList) plotValues.get(1), l);
        double range = max - min;

        if (max == 0) {
            max = 1;
        }

        //System.out.println("PROFILING-DETAILS: Points to plot: " + ((ArrayList) plotValues.get(1)).size());
        LookupPaintScale ps = new LookupPaintScale(min, max + 100, new Color(0, 0, 0));

        renderer.setPaintScale(ps);
        
        ps.add(min, TENPERCENT);
        ps.add(min + (1 * (range / 10)), XYChartPanel.TENPERCENT);
        ps.add(min + (2 * (range / 10)), XYChartPanel.TWENTYPERCENT);
        ps.add(min + (3 * (range / 10)), XYChartPanel.THIRTYPERCENT);
        ps.add(min + (4 * (range / 10)), XYChartPanel.FORTYPERCENT);
        ps.add(min + (5 * (range / 10)), XYChartPanel.FIFTYPERCENT);
        ps.add(min + (6 * (range / 10)), XYChartPanel.SIXTYPERCENT);
        ps.add(min + (7 * (range / 10)), XYChartPanel.SEVENTYPERCENT);
        ps.add(min + (8 * (range / 10)), XYChartPanel.EIGHTYPERCENT);
        ps.add(min + (9 * (range / 10)), XYChartPanel.NINETYPERCENT);
        ps.add(max, XYChartPanel.ALLPERCENT);
        
        
        NumberAxis lAxis = new NumberAxis(lText);
        lAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        psl = new PaintScaleLegend(ps, lAxis);
        psl.setBackgroundPaint(vtea._VTEA.BACKGROUND);
        psl.setPosition(RectangleEdge.RIGHT);
        psl.setMargin(4, 4, 40, 4);
        psl.setAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
        
        } else {
            
            renderer.setBaseFillPaint(TENPERCENT);
        }
        
        Ellipse2D shape = new Ellipse2D.Double(0, 0, size, size);
        Ellipse2D shapeGate = new Ellipse2D.Double(-2, -2, size + 4, size + 4);
      

        renderer.setBaseShape(shape);
        
        
        
        rendererGate.setBaseShape(shapeGate);

        NumberAxis xAxis = new NumberAxis("");
        NumberAxis yAxis = new NumberAxis("");

        xAxis.setAutoRangeIncludesZero(false);
        yAxis.setAutoRangeIncludesZero(false);

        XYPlot plot = new XYPlot(createXYZDataset((ArrayList) plotValues.get(1), x, y, l), xAxis, yAxis, renderer);
        
        plot.getDomainAxis();
        plot.getRangeAxis();

        plot.setDomainPannable(false);
        plot.setRangePannable(false);

        plot.setRenderer(0, renderer);
        plot.setRenderer(1, rendererGate);

        plot.setDataset(0, createXYZDataset((ArrayList) plotValues.get(1), x, y, l));

        if (imageGate) {
            roiCreated(impoverlay);
            XYZDataset set = createXYZDataset(ImageGateOverlay, x, y, l);
            plot.setDataset(1, set);
           plot.setRenderer(1, new XYShapeRenderer() {
               @Override
               protected java.awt.Paint getPaint(XYDataset dataset, int series, int item){
                   return imageGateOutline;
               }

        @Override
        public Shape getItemShape(int row, int col) {
               return new Ellipse2D.Double(-2, -2, size + 4, size + 4);     
        }
    });

        }
        //System.out.println("PROFILING: Generating plot with " + plot.getDatasetCount() + " datasets.");
        //System.out.println("PROFILING: Generating plot with " + ImageGateOverlay.size() + " objects gated.");

        try {
            if (getRangeofData((ArrayList) plotValues.get(1), x) > 16384) {
                LogAxis logAxisX = new LogAxis();
                logAxisX.setAutoRange(true);
                plot.setDomainAxis(logAxisX);
            }

            if (getRangeofData((ArrayList) plotValues.get(1), y) > 16384) {
                LogAxis logAxisY = new LogAxis();
                logAxisY.setAutoRange(true);
                plot.setRangeAxis(logAxisY);
            }
        } catch (NullPointerException e) {
        };

        JFreeChart chart = new JFreeChart("Plot of " + xText + " vs. " + yText, plot);

        chart.removeLegend();


        
        //LUT 
        if(l > 0)chart.addSubtitle(psl);

        //notifiyUpdatePlotWindowListeners();
        return new ChartPanel(chart, true, true, false, false, true);
    }

    private double getRangeofData(ArrayList alVolumes, int x) {

        ListIterator litr = alVolumes.listIterator();

        ArrayList<Number> al = new ArrayList<Number>();

        Number low = 0;
        Number high = 0;
        Number test;

        while (litr.hasNext()) {
            try {
                MicroObjectModel volume = (MicroObjectModel) litr.next();
                Number Corrected = processPosition(x, volume);
                al.add(Corrected);

                if (Corrected.floatValue() < low.floatValue()) {
                    low = Corrected;
                }
                if (Corrected.floatValue() > high.floatValue()) {
                    high = Corrected;
                }
            } catch (NullPointerException e) {
            }
        }

        return high.longValue() - low.longValue();

    }
    
    

    private double getMaximumOfData(ArrayList alVolumes, int x) {

        ListIterator litr = alVolumes.listIterator();

        //ArrayList<Number> al = new ArrayList<Number>();
        Number high = 0;

        while (litr.hasNext()) {
            try {
                MicroObjectModel volume = (MicroObjectModel) litr.next();
                Number Corrected = processPosition(x, volume);
                if (Corrected.floatValue() > high.floatValue()) {
                    high = Corrected;
                }
            } catch (NullPointerException e) {
            }
        }
        return high.longValue();

    }

    private double getMinimumOfData(ArrayList alVolumes, int x) {

        ListIterator litr = alVolumes.listIterator();

        //ArrayList<Number> al = new ArrayList<Number>();
        Number low = getMaximumOfData(alVolumes, x);

        while (litr.hasNext()) {
            try {
                MicroObjectModel volume = (MicroObjectModel) litr.next();
                Number Corrected = processPosition(x, volume);
                if (Corrected.floatValue() < low.floatValue()) {
                    low = Corrected;
                }
            } catch (NullPointerException e) {
            }
        }
        return low.longValue();
    }

    private DefaultXYZDataset createXYZDataset(ArrayList alVolumes, int x, int y, int l) {
        //System.out.println("PROFILING: New dataset, for  " + alVolumes.size() + " objects.");
        DefaultXYZDataset result = new DefaultXYZDataset();
        int counter = 0;

        double[] xCorrected = new double[alVolumes.size()];
        double[] yCorrected = new double[alVolumes.size()];
        double[] lCorrected = new double[alVolumes.size()];

        ListIterator litr = alVolumes.listIterator();

        while (litr.hasNext()) {
//            try {
            MicroObjectModel volume = (MicroObjectModel) litr.next();
            xCorrected[counter] = processPosition(x, volume).doubleValue();
            yCorrected[counter] = processPosition(y, volume).doubleValue();
            if(l > 0){
            lCorrected[counter] = processPosition(l, volume).doubleValue();
            }else{
                lCorrected[counter] = 0;
            }

            counter++;
//            } 
//            catch (NullPointerException e) {
//                System.out.println("EXCEPTION:  Error building dataset.");
//            }
        }

        double[][] series = new double[][]{xCorrected, yCorrected, lCorrected};
        result.addSeries("first", series);

        return result;
    }

    private Number processPosition(int a, MicroObjectModel volume) {
//        ArrayList ResultsPointer = volume.getResultPointer();
//        int size = ResultsPointer.size();
        if (a <= 10) {
            //System.out.println("PROFILING: Object " + volume.getSerialID() + ", value:" + (Number) volume.getAnalysisMaskVolume()[a]);
            return (Number) volume.getAnalysisMaskVolume()[a];
        } else {
            int row = ((a) / 11) - 1;
            int column = a % 11;
            return (Number) volume.getAnalysisResultsVolume()[row][column];
        }
    }

    public void setPointSize(int size) {
        this.size = size;
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public void setImageGate(boolean gate) {
        imageGate = gate;
    }

    public ChartPanel getUpdatedChartPanel() {
        process(xValues, yValues, lValues, xValuesText, yValuesText, lValuesText);
        return chartPanel;
    }

    public void setChartPanelRanges(int axis, double low, double high) {
        XYPlot plot = (XYPlot) this.chartPanel.getChart().getPlot();
        ValueAxis newaxis = new NumberAxis();
        newaxis.setLowerBound(low);
        newaxis.setUpperBound(high);
        if (axis == XYChartPanel.XAXIS) {
            plot.setDomainAxis(newaxis);
        } else if (axis == XYChartPanel.YAXIS) {
            plot.setRangeAxis(newaxis);
        }
    }

    @Override
    public void roiModified(ImagePlus ip, int i) {
    }

    public void roiCreated(ImagePlus ip) {
        //if(impoverlay.equals(ip)){
        ImageGateOverlay.clear();
        ArrayList<MicroObject> volumes = (ArrayList) plotValues.get(1);
        ListIterator<MicroObject> itr = volumes.listIterator();
        while (itr.hasNext()) {
            MicroObject m = itr.next();
            int[] c = new int[2];
            c = m.getBoundsCenter();

            if (ip.getRoi().contains(c[0], c[1])) {
                ImageGateOverlay.add(m);
            }
            //}
        }
        //System.out.println("PROFILING: XYChartPanel... image gate, found " + ImageGateOverlay.size() + " volumes in region");
        //process(xValues,yValues,lValues,xValuesText,yValuesText,lValuesText);

        //System.out.println("PROFILING: XYChartPanel... image gate processed and updated.");
        notifiyUpdatePlotWindowListeners();
    }

    public void roiDeleted() {
        ImageGateOverlay.clear();
        process(xValues, yValues, lValues, xValuesText, yValuesText, lValuesText);
        //chartPanel.repaint();
    }

    public void setOverlayImage(ImagePlus ip) {
        impoverlay = ip;
    }

    public void addUpdatePlotWindowListener(UpdatePlotWindowListener listener) {
        //System.out.println("PROFILING: Added listener");
        UpdatePlotWindowListeners.add(listener);
    }

    public void notifiyUpdatePlotWindowListeners() {
        for (UpdatePlotWindowListener listener : UpdatePlotWindowListeners) {
            //System.out.println("Updating plot window... for image gate.");
            listener.onUpdatePlotWindow();
        }
    }
    
   

}
