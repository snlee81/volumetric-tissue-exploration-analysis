package vteaobjects.layercake;

import ij.*;
import ij.process.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import org.scijava.plugin.Plugin;
import vteaobjects.MicroObject;
import vteaobjects.Segmentation.Segmentation;


@Plugin (type = Segmentation.class)
public class LayerCake3D implements Segmentation {

    /**
     * Constants
     */
    /**
     * Variables
     */
    private ImagePlus imageOriginal;
    private ImagePlus imageResult;
    private ImageStack stackOriginal;
    protected ImageStack stackResult;

    private boolean watershedImageJ = true;
    
    private List<microRegion> alRegions = Collections.synchronizedList(new ArrayList<microRegion>());
    private List<microVolume> alVolumes = Collections.synchronizedList(new ArrayList<microVolume>());

    private List<microRegion> alRegionsProcessed = Collections.synchronizedList(new ArrayList<microRegion>());

    private int[] minConstants; // 0: minObjectSize, 1: maxObjectSize, 2: minOverlap, 3: minThreshold



//derivedRegionType[][], [Channel][0, type, 1, subtype];
    /**
     * Constructors
     */
//empty cosntructor
    public LayerCake3D() {
    }

//constructor for volume building
    public LayerCake3D(List<microRegion> Regions, int[] minConstants, ImageStack orig) {
        this.stackOriginal = orig;
        this.minConstants = minConstants;
        this.alRegions = Regions;
        //this.nVolumes = 0;

        Collections.sort(alRegions, new ZComparator());

        VolumeForkPool vf = new VolumeForkPool(alRegions, minConstants, 0, alRegions.size() - 1);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(vf);
        cleanupVolumes();
    }

//constructor for region building
    public LayerCake3D(ImageStack stack, int[] min, boolean imageOptimize) {

        minConstants = min;
        stackOriginal = stack;
        imageOriginal = new ImagePlus("Mask", stack);

        stackResult = stack.duplicate();

        System.out.println("PROFILING: parsing stack of dimensions: z, " + stackResult.getSize() + " for a threshold of " + minConstants[3]);

        for (int n = 0; n < stackResult.getSize(); n++) {
            for (int x = 0; x < stackResult.getWidth(); x++) {
                for (int y = 0; y < stackResult.getHeight(); y++) {
                    if (stackResult.getVoxel(x, y, n) <= minConstants[3]) {
                        stackResult.setVoxel(x, y, n, (Math.pow(2, stack.getBitDepth())) - 1);
                    } else {
                        stackResult.setVoxel(x, y, n, 0);
                    }
                }
            }
        }

        ImagePlus impShow = new ImagePlus("Orginal", stackOriginal);
        //impShow.show();

        imageResult = new ImagePlus("Mask Result", stackResult);
        IJ.run(imageResult, "8-bit", "");
        if (watershedImageJ) {
            IJ.run(imageResult, "Watershed", "stack");
        }
        IJ.run(imageResult, "Invert", "stack");

        //imageResult.show();

        makeRegionsPool(imageResult.getStack(), stackOriginal);
    }

//constructor for region building with algorithmic threshold setting  
    public LayerCake3D(ImageStack stack, ArrayList<String> threshold) {
        this.minConstants = minConstants;
        stackOriginal = new ImageStack();
        stackOriginal = stack;
        ImagePlus imp = new ImagePlus("Mask", stack);
        imageOriginal = imp;
    }

    private synchronized void cleanupVolumes() {
        //loop through all volumes
        List<microVolume> alVolumesTrim = Collections.synchronizedList(new ArrayList<microVolume>());
     
        Collections.sort(alVolumes, new ZObjectComparator());

        microVolume[] mvs = new microVolume[alVolumes.size()];

        mvs = alVolumes.toArray(mvs);
        microVolume testVolume;
        List<microRegion> testRegions;
        microRegion testRegion;
        microVolume referenceVolume;
        List<microRegion> referenceRegions;
        microRegion referenceRegion;
        int referenceZ;
        int[] assigned = new int[alVolumes.size()];

        for (int i = 0; i < assigned.length; i++) {
            assigned[i] = 0;
        }
        microVolume resultVolume = new microVolume();
        double testCentroid[] = new double[2];
        double referenceCentroid[] = new double[2];

        for (int i = 0; i < mvs.length; i++) {
            if (assigned[i] == 0) {
                referenceVolume = mvs[i];

                referenceRegions = referenceVolume.getRegions();
                referenceRegion = referenceRegions.get(referenceRegions.size() - 1);
                referenceCentroid[0] = referenceRegion.getBoundCenterX();
                referenceCentroid[1] = referenceRegion.getBoundCenterY();
                referenceZ = referenceRegion.getZPosition();

                //System.out.println("PROFILING-volume cleanup, on: " + mvs[i].getName() + " at: " + referenceZ + " and " + mvs[i].getNRegions() + " regions.");
                //System.out.println("PROFILING-checking for neighboring volumes: " + i);
                for (int j = 0; j < mvs.length; j++) {

                    testVolume = mvs[j];
                    testRegions = testVolume.getRegions();
                    //Collections.sort(testRegions, new ZComparator());
                    testRegion = testRegions.get(0);
                    testCentroid[0] = testRegion.getBoundCenterX();
                    testCentroid[1] = testRegion.getBoundCenterY();
                    if (i != j && assigned[j] != 1 && lengthCart(testCentroid, referenceCentroid) < minConstants[2] && testRegion.getZPosition() - referenceZ == 1) {
                        ListIterator<microRegion> testItr = testRegions.listIterator();
                        while (testItr.hasNext()) {
                            microRegion reg = testItr.next();
                            resultVolume.addRegion(new microRegion(reg.getPixelsX(), reg.getPixelsY(), reg.getPixelCount(), reg.getZPosition(), stackOriginal));
                        }
                        resultVolume.addRegions(referenceRegions);
                        resultVolume.addRegions(testRegions);
                        resultVolume.setName(referenceVolume.getName() + "_" + testVolume.getName());
                        assigned[i] = 1;
                        assigned[j] = 1;

                        //System.out.println("PROFILING-found a partner: " + mvs[j].getName() + " at z: " + testRegion.getZPosition() + " at, " + lengthCart(testCentroid, referenceCentroid) + " pixels.");
                    }
                    testVolume = new microVolume();
                }

                if (assigned[i] == 1) {
                    resultVolume.calculateVolumeMeasurements();
                    //System.out.println("PROFILING-calculated volume measures: " + resultVolume.getName() + ". Giving derived: " + resultVolume.getAnalysisResultsVolume()[0][2] + " for "+ resultVolume.getNRegions() + " regions.");
                    //System.out.println("PROFILING-calculated volume measures: " + resultVolume.getName() + ".  Giving region: " + resultVolume.getAnalysisMaskVolume()[2] + " for "+ resultVolume.getNRegions() + " regions.");
                    alVolumesTrim.add(resultVolume);
                    //System.out.println("PROFILING: Adding to list: " + resultVolume.getName());
                    resultVolume = new microVolume();
                    resultVolume.setName("");
                    referenceVolume = new microVolume();
                }
            }

        }

        for (int k = 0; k < mvs.length; k++) {
            if (assigned[k] == 0) {
                microVolume mv = new microVolume();
                mv = mvs[k];
                mv.calculateVolumeMeasurements();
                alVolumesTrim.add(mv);
                //System.out.println("PROFILING: Adding to list: " + mv.getName());
            }
        }

        alVolumes.clear();
        //System.out.println("PROFILING: Volumes found: " + alVolumesTrim.size());
        alVolumes.addAll(alVolumesTrim);
    }

    private void findConnectedRegions(int volumeNumber, double[] startRegion, int z) {

        double[] testRegion = new double[2];

        microRegion test = new microRegion();
        for (int i = 0; i < alRegions.size(); i++) {
            test = alRegions.get(i);
            testRegion[0] = test.getBoundCenterX();
            testRegion[1] = test.getBoundCenterY();
            double comparator = lengthCart(startRegion, testRegion);
            if (!test.isAMember()) {
                if (comparator <= minConstants[2] && ((test.getZPosition() - z) < 3)) {
                    test.setMembership(volumeNumber);
                    test.setAMember(true);
                    z = test.getZPosition();
                    testRegion[0] = (testRegion[0] + startRegion[0]) / 2;
                    testRegion[1] = (testRegion[1] + startRegion[1]) / 2;
                    alRegionsProcessed.add(test);
                    //alRegions.remove(i); 
                    findConnectedRegions(volumeNumber, testRegion, z);
                    //System.out.println("PROFILING: Adding regions: " + i);
                }
            }

        }
    }


    /**
     * Methods
     */
    private void makeRegionsPool(ImageStack stack, ImageStack original) {
        RegionForkPool rrf = new RegionForkPool(stack, original, 0, stack.getSize());
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(rrf);
    }

    private double[] check8Neighbors(ImageStack stack, int[] point, double counter) {
        double[] result = new double[2];
        int x = point[0];
        int y = point[1];
        int z = point[2];
        double[] neighbors = new double[10];

        //N
        try {
            neighbors[0] = stack.getVoxel(x, y + 1, z);
        } catch (NullPointerException e) {
            neighbors[0] = 0;
        }
        //NE
        try {
            neighbors[1] = stack.getVoxel(x + 1, y + 1, z);
        } catch (NullPointerException e) {
            neighbors[1] = 0;
        }
        //E
        try {
            neighbors[2] = stack.getVoxel(x, y + 1, z);
        } catch (NullPointerException e) {
            neighbors[2] = 0;
        }
        //SE
        try {
            neighbors[3] = stack.getVoxel(x - 1, y + 1, z);
        } catch (NullPointerException e) {
            neighbors[3] = 0;
        }
        //S
        try {
            neighbors[4] = stack.getVoxel(x, y - 1, z);
        } catch (NullPointerException e) {
            neighbors[4] = 0;
        }
        //SW
        try {
            neighbors[5] = stack.getVoxel(x - 1, y - 1, z);
        } catch (NullPointerException e) {
            neighbors[5] = 0;
        }
        //W
        try {
            neighbors[6] = stack.getVoxel(x - 1, y, z);
        } catch (NullPointerException e) {
            neighbors[6] = 0;
        }
        //NW
        try {
            neighbors[7] = stack.getVoxel(x - 1, y + 1, z);
        } catch (NullPointerException e) {
            neighbors[7] = 0;
        }
        //up
        try {
            neighbors[8] = stack.getVoxel(x, y, z + 1);
        } catch (NullPointerException e) {
            neighbors[7] = 0;
        }
        //down
        try {
            neighbors[9] = stack.getVoxel(x, y, z - 1);
        } catch (NullPointerException e) {
            neighbors[7] = 0;
        }

        //parse neighbors array
        double tag = counter;
        for (int i = 0; i <= 10; i++) {
            if (neighbors[i] > 255) {
                tag = neighbors[i];
            }
            if (neighbors[i] == 255) {
                tag = counter++;
            }
        }

        result[0] = tag;
        result[1] = counter;

        return result;
    }

    public void makeDerivedRegions(int[][] localDerivedRegionTypes, int channels, ImageStack[] stack, ArrayList ResultsPointers) {
        ListIterator<microVolume> itr = alVolumes.listIterator();
        while (itr.hasNext()) {
            microVolume mv = new microVolume();
            mv = itr.next();
            mv.makeDerivedRegions(localDerivedRegionTypes, channels, stack, ResultsPointers);
        }
    }

    public void makeDerivedRegionsThreading(int[][] localDerivedRegionTypes, int channels, ImageStack[] Stack, ArrayList ResultsPointers) {

        int processors = Runtime.getRuntime().availableProcessors();
        int length = alVolumes.size() / processors;
        int remainder = alVolumes.size() % processors;

        int start = 0;
        int stop = start + length - 1;

        CopyOnWriteArrayList<DerivedRegionWorker> rw = new CopyOnWriteArrayList<DerivedRegionWorker>();

        for (int i = 0; i < processors; i++) {
            ArrayList<microVolume> volume = new ArrayList<microVolume>();
            if (i == processors - 1) {
                synchronized (alVolumes) {
                    //ListIterator<microVolume> itr = alVolumes.listIterator(start);
                    //DerivedRegionWorker region = new DerivedRegionWorker(localDerivedRegionTypes, channels, Stack, ResultsPointers, itr, stop);  
                    ArrayList<microVolume> process = new ArrayList<microVolume>();
                    process.addAll(alVolumes.subList(start, stop));

                    DerivedRegionWorker region = new DerivedRegionWorker(localDerivedRegionTypes, channels, Stack, ResultsPointers, process, stop);
                    rw.add(region);
                }
                //IJ.log("RegionFactory::makeDerivedRegion Created thread #"+i +" for volumes: " + start + " to " + stop + ", " + volume.size() + " total.");

                start = stop + 1;
                stop = stop + length + remainder;
            } else {
                synchronized (alVolumes) {
                    //ListIterator<microVolume> itr = alVolumes.listIterator(start);
                    //DerivedRegionWorker region = new DerivedRegionWorker(localDerivedRegionTypes, channels, Stack, ResultsPointers, itr, stop); 
                    ArrayList<microVolume> process = new ArrayList<microVolume>();
                    process.addAll(alVolumes.subList(start, stop));
                    DerivedRegionWorker region = new DerivedRegionWorker(localDerivedRegionTypes, channels, Stack, ResultsPointers, process, stop);
                    rw.add(region);
                }
                //IJ.log("RegionFactory::makeDerivedRegion Created thread #"+i +" for volumes: " + start + " to " + stop + ", " + volume.size() + " total.");

                start = stop + 1;
                stop = start + length;
            }
        }
        ListIterator<DerivedRegionWorker> itr = rw.listIterator();
        while (itr.hasNext()) {
            itr.next().start();
        }
    }

    public void makeDerivedRegionsPool(int[][] localDerivedRegionTypes, int channels, ImageStack[] Stack, ArrayList ResultsPointers) {

        DerivedRegionForkPool drf = new DerivedRegionForkPool(localDerivedRegionTypes, channels, Stack, ResultsPointers, 0, alVolumes.size());
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(drf);

    }

    private double lengthCart(double[] position, double[] reference_pt) {
        double distance;
        double part0 = position[0] - reference_pt[0];
        double part1 = position[1] - reference_pt[1];
        distance = Math.sqrt((part0 * part0) + (part1 * part1));
        return distance;
    }

    public List<microRegion> getRegions() {
        return alRegions;
    }

    public int getRegionsCount() {
        return this.alRegions.size();
    }

    public int getVolumesCount() {
        return this.alVolumes.size();
    }

    public ArrayList getVolumesAsList() {
        return new ArrayList(alVolumes);
    }

    @Override
    public ImagePlus getSegmentation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        return "LayerCake 3D";
    }

    @Override
    public String getKey() {
        return "LayerCake3D";
    }

    @Override
    public ArrayList<MicroObject> getObjects() {
        return new ArrayList(alVolumes);
    }
    
    private class DerivedRegionWorker implements Runnable {

        private int[][] derivedRegionType;
        int channels;
        ImageStack[] stack;
        ArrayList ResultsPointers;
        ArrayList<microVolume> Volumes;
        int stop;
        ListIterator<microVolume> itr;
        Thread t;
        private String threadName = "derivedregionmaker_" + System.nanoTime();

        DerivedRegionWorker(int[][] ldrt, int c, ImageStack[] st, ArrayList rp, ListIterator<microVolume> litr, int s) {
            this.derivedRegionType = ldrt;
            channels = c;
            stack = st;
            ResultsPointers = rp;
            stop = s;
            itr = litr;
        }

        DerivedRegionWorker(int[][] ldrt, int c, ImageStack[] st, ArrayList rp, ArrayList<microVolume> vols, int s) {
            this.derivedRegionType = ldrt;
            channels = c;
            stack = st;
            ResultsPointers = rp;
            Volumes = vols;
            stop = s;
            itr = vols.listIterator();
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            defineDerivedRegions();
            long end = System.nanoTime();
            System.out.println("PROFILING: Thread: " + threadName + " runtime: " + ((end - start) / 1000000) + " ms.");
        }

        public void start() {
            t = new Thread(this, threadName);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
            try {
                t.join();
            } catch (Exception e) {
                System.out.println("PROFILING: Thread " + threadName + " interrupted.");
            }

        }

        private void defineDerivedRegions() {
            while (itr.hasNext()) {
                microVolume mv = new microVolume();
                mv = itr.next();
                mv.makeDerivedRegions(derivedRegionType, channels, stack, ResultsPointers);
            }
        }
    }
    private class DerivedRegionForkPool extends RecursiveAction {

        //class splits it self into new classes...  start with largest start and stop and subdivided recursively until start-stop is the number for the number of cores or remaineder.
        private int[][] derivedRegionType;
        int channels;
        ImageStack[] stack;
        ArrayList ResultsPointers;
        int stop;
        int start;
        List<microVolume> volumes = Collections.synchronizedList(new ArrayList<microVolume>());

        //Thread t;
        //private String threadName = "derivedregionmaker_" + System.nanoTime();
        DerivedRegionForkPool(int[][] ldrt, int c, ImageStack[] st, ArrayList rp, int start, int stop) {
            derivedRegionType = ldrt;
            channels = c;
            stack = st;
            ResultsPointers = rp;
            this.stop = stop;
            this.start = start;

            //System.out.println("PROFILING-DETAILS: ForkJoin Start and Stop points:" + start + ", " + stop);
            //volumes = alVolumes.subList(start, stop);
        }

        private void defineDerivedRegions() {
            ListIterator<microVolume> itr = alVolumes.listIterator(start);
            int i = start;
            while (itr.hasNext() && i <= stop) {
                microVolume mv = new microVolume();
                mv = itr.next();

                mv.makeDerivedRegions(derivedRegionType, channels, stack, ResultsPointers);
                //System.out.println("PROFILING: making derived regions.  " + mv.getName() + ", getting " + mv.getNDRegions() + " derived regions and " + mv.getderivedConstants()[1][0]+ "  Giving: " + mv.getAnalysisResultsVolume()[0][2]);
                i++;
            }
        }

        @Override
        protected void compute() {

            int processors = Runtime.getRuntime().availableProcessors();
            int length = alVolumes.size() / processors;

            if (alVolumes.size() < processors) {
                length = alVolumes.size();
            }

            //System.out.println("PROFILING-DETAILS: Derived Regions Making ForkJoin Start and Stop points:" + start + ", " + stop + " for length: " + (stop-start) + " and target length: " + length);
            if (stop - start > length) {
                invokeAll(new DerivedRegionForkPool(derivedRegionType, channels, stack, ResultsPointers, start, start + ((stop - start) / 2)),
                        new DerivedRegionForkPool(derivedRegionType, channels, stack, ResultsPointers, start + ((stop - start) / 2) + 1, stop));
                //System.out.println("PROFILING-DETAILS: ForkJoin Splitting...");
            } else {
                //System.out.println("PROFILING-DETAILS: ForkJoin Computing...");
                defineDerivedRegions();
            }
        }
    }
    private class RegionForkPool extends RecursiveAction {

        private int maxsize = 1;
        //private microRegion[] Regions = new microRegion[(int) (maxsize / minConstants[0])];
        private ArrayList<microRegion> alResult = new ArrayList<microRegion>();
        private int[] start_pixel = new int[3];
        int x, y, z;
        //int[] x_positions = new int[(int) minConstants[1]];
        //int[] y_positions = new int[(int) minConstants[1]];

        ArrayList<Integer> x_positions = new ArrayList<Integer>();
        ArrayList<Integer> y_positions = new ArrayList<Integer>();

        int n_positions = 0;
        int[] BOL = new int[5000];  //start of line position
        int[] EOL = new int[5000];  //end of line position
        int[] row = new int[5000];  //line position

//        ArrayList<Integer> BOL = new ArrayList<Integer>();
//        ArrayList<Integer> EOL = new ArrayList<Integer>();
//        ArrayList<Integer> row = new ArrayList<Integer>();
        int count = 0;
        private ImageStack stack;
        private ImageStack original;
        private int start;
        private int stop;

        private Thread t;
        private String threadName = "regionfinder_" + System.nanoTime();

        RegionForkPool(ImageStack st, ImageStack orig, int start, int stop) {
            stackOriginal = stack = st;
            stackResult = original = orig;
            this.start = start;
            this.stop = stop;
            maxsize = stack.getSize() * stack.getWidth() * stack.getHeight();
        }

        private void defineRegions() {
            
            int color = 1;
            int region = 0;
            ArrayList<int[]> pixels = new ArrayList<int[]>();
            
            for (int n = this.start; n <= this.stop; n++) {		
                for (int p = 0; p < stack.getWidth(); p++) {
                    for (int q = 0; q < stack.getHeight(); q++) {
                        //start pixel selected if 255, new region
                        
                        if (getVoxelBounds(stack, p, q, n) == 255) {
                            //System.out.println("PROFILING: region start: " + region);
                            pixels = floodfill(stack, p, q, n, stack.getWidth(), stack.getHeight(), stack.getSize(), color, pixels);
                            
                            //System.out.println("PROFILING: region size: " + pixels.size());
                            //microRegion(int[] x, int[] y, int n, int z, ImageStack stack) 
                            
                            
                            int[] pixel = new int[3];
                            int[] xPixels = new int[pixels.size()];
                            int[] yPixels = new int[pixels.size()];
                            int j = 0;
                            
                            ListIterator<int[]> itr = pixels.listIterator();
                            //unpack the arraylist
                            while(itr.hasNext()){
                                pixel = itr.next();
                                xPixels[j] = pixel[0];
                                yPixels[j] = pixel[1];
                                j++;
                            }
                            
                            // 0: minObjectSize, 1: maxObjectSize, 2: minOverlap, 3: minThreshold
                            //add region to array
                            if (xPixels.length > (int) minConstants[0] && xPixels.length < (int) minConstants[1]) {
                                alResult.add(new microRegion(xPixels, yPixels, xPixels.length, n, original));
                            } 
                            
                            //reassign pixels from stack

//                            for (int i = 0; i <= xPixels.length - 1; i++) {
//                                stack.setVoxel(xPixels[i], yPixels[i], n, color);
//                            }
if (color < 253) {
    color++;
} else {
    color = 1;
}

//reset	arrays

//row.clear();
n_positions = 0;
count = 0;
region++;
pixels.clear();
                        }
                    }
                }
            }
            //this.Regions = Regions;
            //this.nRegions = nRegions;
            //this.alRegions =+ ;
            //this.nRegions =+ nRegions; 
            System.out.println("PROFILING: ...Regions found in thread:  " + alResult.size());

        }
        
        
        private ArrayList<int[]> floodfill(ImageStack stack, int x, int y, int z, int width, int height, int depth, int color, ArrayList<int[]> pixels){
            
            if(x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= depth || stack.getVoxel(x,y,z) < 255){
                return pixels;
            } else {
                //|| stack.getVoxel(x, y, z) == color
                //System.out.println("PROFILING: Adding point to object: " + color );
                stack.setVoxel(x, y, z, color);
                
                int[] pixel = new int[3];
                pixel[0] = x;
                pixel[1] = y;
                pixel[2] = z;
                
                pixels.add(pixel);
                
//        pixels = floodfill(stack, x, y, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x+1, y, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x, y+1, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x+1, y+1, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x-1, y, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x, y-1, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x-1, y-1, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x-1, y+1, z+1, width, height, depth, color, pixels);
//        pixels = floodfill(stack, x+1, y-1, z+1, width, height, depth, color, pixels);
//        floodfill3D(stack, x, y, z-1, width, height, depth, color);
//        floodfill3D(stack, x+1, y, z-1, width, height, depth, color);
//        floodfill3D(stack, x, y+1, z-1, width, height, depth, color);
//        floodfill3D(stack, x+1, y+1, z-1, width, height, depth, color);
//        floodfill3D(stack, x-1, y, z-1, width, height, depth, color);
//        floodfill3D(stack, x, y-1, z-1, width, height, depth, color);
//        floodfill3D(stack, x-1, y-1, z-1, width, height, depth, color);
//        floodfill3D(stack, x-1, y+1, z-1, width, height, depth, color);
//        floodfill3D(stack, x+1, y-1, z-1, width, height, depth, color);
pixels = floodfill(stack, x+1, y, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x, y+1, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x+1, y+1, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x-1, y, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x, y-1, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x-1, y-1, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x-1, y+1, z, width, height, depth, color, pixels);
pixels = floodfill(stack, x+1, y-1, z, width, height, depth, color, pixels);
            }
            return pixels;
            
        }
        
        
        
        
        private void defineRegions3() {
            //uses a line scan flood fill method with backfill
            int color = 1;
            for (int n = this.start; n <= this.stop; n++) {
                //IJ.showProgress(n + 1, this.stop);
                //loop through pixels to find starts for regions		
                for (int p = 0; p <= stack.getWidth() - 1; p++) {
                    for (int q = 0; q <= stack.getHeight() - 1; q++) {
                        //start pixel selected if 255, new region

                        if (getVoxelBounds(stack, p, q, n) == 255) {
                            start_pixel[0] = p;
                            start_pixel[1] = q;
                            start_pixel[2] = n;

                            //position being analyzed
                            x = start_pixel[0];
                            y = start_pixel[1];
                            z = start_pixel[2];

                            //052814 while(stack.getVoxel(x-1,y,z) == 255) {x--;}
                            //start pixel is left most in first row
                            //count is for the line in question
                            BOL[count] = x;
                            row[count] = y;
                            //BOL.add(x);
                            //row.add(y);
                            //run until 0, end of first row

                            while (getVoxelBounds(stack, x, y, z) == 255) {
                                x++;
                            }

                            EOL[count] = x - 1;
                            //EOL.add(x-1);
                            count++;
                            //second row start, search up
                            // x = BOL.get(count - 1);
                            x = BOL[count - 1];

                            while (getVoxelBounds(stack, x, y, z) == 255) {

                                if (getVoxelBounds(stack, x, y + 1, z) == 255) {

                                    while (getVoxelBounds(stack, x - 1, y + 1, z) == 255) {
                                        x--;
                                    }

                                    BOL[count] = x;
                                    row[count] = y + 1;

                                    while (getVoxelBounds(stack, x + 1, y + 1, z) == 255) {
                                        x++;
                                    }

                                    EOL[count] = x;
                                    y++;
                                    x = BOL[count];
                                    count++;
                                }

                                x++;
                            }

                            //reset start pixel and search down
                            x = BOL[0];
                            y = start_pixel[1];

                            while (getVoxelBounds(stack, x, y, z) == 255) {

                                if (getVoxelBounds(stack, x, y - 1, z) == 255) {

                                    while (getVoxelBounds(stack, x - 1, y - 1, z) == 255) {
                                        x--;
                                    }

                                    BOL[count] = x;
                                    row[count] = y - 1;

                                    while (getVoxelBounds(stack, x + 1, y - 1, z) == 255) {
                                        x++;
                                    }

                                    EOL[count] = x;
                                    y--;
                                    x = BOL[count];
                                    count++;
                                }

                                x++;
                            }

                            // }
                            //parse tables
                            //get whole region
                            for (int a = 0; a <= count - 1; a++) {				//loop rows
                                for (int c = BOL[a]; c <= EOL[a]; c++) {			//loop x or columns
                                    x_positions.add(c);
                                    y_positions.add(row[a]);
                                }
                            }
                            //add region to array
                            if (x_positions.size() > (int) minConstants[0] && x_positions.size() < (int) minConstants[1]) {
                                alResult.add(new microRegion(convertPixelArrayList(x_positions), convertPixelArrayList(y_positions), x_positions.size(), n, original));
                            }
                            //reassign pixels from stack

                            for (int i = 0; i <= x_positions.size() - 1; i++) {
                                stack.setVoxel(x_positions.get(i), y_positions.get(i), n, color);
                            }
                            if (color < 253) {
                                color++;
                            } else {
                                color = 1;
                            }

                            //reset	arrays
                            row = new int[5000];
                            //row.clear();
                            x_positions.clear();
                            y_positions.clear();
                            n_positions = 0;
                            count = 0;
                        }
                    }
                }
            }
            //this.Regions = Regions;
            //this.nRegions = nRegions;
            //this.alRegions =+ ;
            //this.nRegions =+ nRegions; 
            System.out.println("PROFILING: ...Regions found in thread:  " + alResult.size());

        }

        private double getVoxelBounds(ImageStack stack, int x, int y, int z) {

            try {
                return stack.getVoxel(x, y, z);
            } catch (IndexOutOfBoundsException e) {
                return -1;
            }
        }
 
        private void defineRegions2() {
//refining connected component labeling
ImageStack resultstack = stack;
double c = 256;
for (int n = 0; n <= stack.getSize() - 1; n++) {
    
    //loop through pixels to find starts
    for (int p = 0; p <= stack.getWidth() - 1; p++) {
        for (int q = 0; q <= stack.getHeight() - 1; q++) {
            //start pixel selected if 255
            if (stack.getVoxel(p, q, n) == 0) {
                resultstack.setVoxel(p, q, n, 0);
            }
            if (stack.getVoxel(p, q, n) == 255) {
                int[] point = new int[4];
                point[0] = p;
                point[1] = q;
                point[2] = n;
                check8Neighbors(stack, point, c);
                resultstack.setVoxel(p, q, n, c);
            }
        }
    }
}
        }
        
        private void setRegions() {
            alRegions.addAll(alResult);
        }

        public ArrayList<microRegion> getRegions() {
            return this.alResult;
        }

        public int[] convertPixelArrayList(List<Integer> integers) {
            int[] ret = new int[integers.size()];
            Iterator<Integer> iterator = integers.iterator();
            for (int i = 0; i < ret.length; i++) {
                ret[i] = iterator.next().intValue();
            }
            return ret;
        }

        @Override
        protected void compute() {

            int processors = Runtime.getRuntime().availableProcessors();
            int length = stack.getSize() / processors;

            if (stack.getSize() < processors) {
                length = stack.getSize();
            }
            
            //int remainder = alRegions.size()%processors; 
            //System.out.println("PROFILING-DETAILS: Region Making ForkJoin Start and Stop points:" + start + ", " + stop + " for length: " + (stop-start) + " and target length: " + length);
            if (stop - start > length) {
                // RegionForkPool(ImageStack st, ImageStack orig, int start, int stop)
                invokeAll(new RegionForkPool(stack, original, start, start + ((stop - start) / 2)),
                        new RegionForkPool(stack, original, start + ((stop - start) / 2) + 1, stop));
                //System.out.println("PROFILING-DETAILS: Region Making ForkJoin Splitting...");
            } else {
                defineRegions();
                setRegions();
            }
        }
    }
    private class VolumeForkPool extends RecursiveAction {

        private int start;
        private int stop;

        private List<microRegion> alRegionsLocal = Collections.synchronizedList(new ArrayList<microRegion>());
        private List<microRegion> alRegionsProcessedLocal = Collections.synchronizedList(new ArrayList<microRegion>());

        private int[] minConstantsLocal;

        private int nVolumesLocal;

        public VolumeForkPool(List<microRegion> Regions, int[] minConstants, int start, int stop) {
            this.minConstantsLocal = minConstants;
            this.alRegionsLocal = Regions.subList(start, stop);
            this.nVolumesLocal = 0;
            this.start = start;
            this.stop = stop;
        }

        private synchronized void defineVolumes() {
            int z;
            microVolume volume = new microVolume();
            double[] startRegion = new double[2];

            microRegion test = new microRegion();

            int i = start;

            while (i < stop) {
                test = alRegions.get(i);
                if (!test.isAMember()) {
                    nVolumesLocal++;
                    startRegion[0] = test.getBoundCenterX();
                    startRegion[1] = test.getBoundCenterY();
                    test.setMembership(nVolumesLocal);
                    test.setAMember(true);
                    z = test.getZPosition();
                    alRegionsProcessedLocal.add(test);
                    findConnectedRegions(nVolumesLocal, startRegion, z);
                }
                i++;
            }

            for (int j = 1; j <= this.nVolumesLocal; j++) {
                volume = new microVolume();
                volume.setName("vol_" + j);
                Iterator<microRegion> vol = alRegionsProcessedLocal.listIterator();
                microRegion region = new microRegion();
                while (vol.hasNext()) {
                    region = vol.next();
                    if (j == region.getMembership()) {
                        volume.addRegion(region);
                    }
                }
                if (volume.getNRegions() > 0) {
                    volume.calculateVolumeMeasurements();
                    if (volume.getPixelCount() >= minConstantsLocal[0]) {
                        alVolumes.add(volume);
                    }
                }
            }
        }

        @Override
        protected void compute() {
            //limited to four threads, odd unreproducible behaviour above 4 threads
            int processors = 4;
            int length = alRegions.size() / processors;

            if (alRegions.size() < processors) {
                length = alRegions.size();
            }

            //System.out.println("PROFILING-DETAILS: Volume Making ForkJoin Start and Stop points:" + start + ", " + stop + " for length: " + (stop-start) + " and target length: " + length);
            if (stop - start > length) {
                // RegionForkPool(ImageStack st, ImageStack orig, int start, int stop)
                invokeAll(new VolumeForkPool(alRegions, minConstantsLocal, start, start + ((stop - start) / 2)),
                        new VolumeForkPool(alRegions, minConstantsLocal, start + ((stop - start) / 2) + 1, stop));
                //System.out.println("PROFILING-DETAILS: Region Making ForkJoin Splitting...");
            } else {
                //System.out.println("PROFILING-DETAILS: Volume Making Fork Join Start and Stop points: " + start + ", " + stop + " for length: " + alRegionsLocal.size());
                defineVolumes();
            }
        }

        private synchronized void findConnectedRegions(int volumeNumber, double[] startRegion, int z) {

            double[] testRegion = new double[2];
            int i = start;
            while (i < stop - 1) {
                microRegion test = new microRegion();
                test = alRegions.get(i);
                testRegion[0] = test.getBoundCenterX();
                testRegion[1] = test.getBoundCenterY();
                double comparator = lengthCart(startRegion, testRegion);
                if (!test.isAMember()) {
                    if (comparator <= minConstants[2] && ((test.getZPosition() - z) == 1)) {
                        test.setMembership(volumeNumber);
                        test.setAMember(true);
                        z = test.getZPosition();
                        testRegion[0] = (testRegion[0] + startRegion[0]) / 2;
                        testRegion[1] = (testRegion[1] + startRegion[1]) / 2;
                        alRegionsProcessedLocal.add(test);
                        //alRegions.remove(i); 
                        findConnectedRegions(volumeNumber, testRegion, z);
                        //System.out.println("PROFILING: Adding regions: " + i);
                    }
                }
                i++;
            }
        }
    }
    private class ZComparator implements Comparator<microRegion> {

        @Override
        public int compare(microRegion o1, microRegion o2) {
            if (o1.getZPosition() > o2.getZPosition()) {
                return 1;
            } else if (o1.getZPosition() < o2.getZPosition()) {
                return -1;
            } else if (o2.getZPosition() > o1.getZPosition()) {
                return -1;
            } else if (o2.getZPosition() < o1.getZPosition()) {
                return 1;
            } else if (o1.getZPosition() == o2.getZPosition()) {
                return -1;
            } else {
                return 0;
            }
        }

    }
    private class XComparator implements Comparator<microRegion> {

        @Override
        public int compare(microRegion o1, microRegion o2) {
            if (o1.getCentroidX() > o2.getCentroidX()) {
                return 1;
            } else {
                return -1;
            }
        }

    }
    private class YComparator implements Comparator<microRegion> {

        @Override
        public int compare(microRegion o1, microRegion o2) {
            if (o1.getCentroidY() > o2.getCentroidY()) {
                return 1;
            } else {
                return -1;
            }
        }

    }
    private class ZObjectComparator implements Comparator<microVolume> {

        @Override
        public int compare(microVolume o1, microVolume o2) {
            if (o1.getCentroidZ() > o2.getCentroidZ()) {
                return 1;
            } else if (o1.getCentroidZ() < o2.getCentroidZ()) {
                return -1;
            } else if (o2.getCentroidZ() > o1.getCentroidZ()) {
                return -1;
            } else if (o2.getCentroidZ() < o2.getCentroidZ()) {
                return 1;
            } else if (o1.getCentroidZ() == o2.getCentroidZ()) {
                return 0;
            } else {
                return -1;
            }
        }

    }

}
