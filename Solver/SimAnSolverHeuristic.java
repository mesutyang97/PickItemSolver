package Solver;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.*;
import java.util.Comparator;

/**
 * Created by yxiaocheng1997 on 4/20/17.
 */
public class SimAnSolverHeuristic {

    //private final double confidence = 0.8;
    //private final double blockingPerc = 0.9;

    private boolean bucketStarted;
    private int bucketbit;
    private int bucketTicker;
    private int bucketsize;

    /** how many item has been put into buckets. */

    private final boolean bumpingItem = false;
    private long bucketedItem;
    private final long TIMEOUTSEC = 200;
    private int alarmLmt;
    private final double tempChange = 0.9999;
    private int MAXITER;
    private final int MAXOUTER = 200;
    private final double percentKicked = 0.1;
    private final double percentKickedCls = 0.1;
    private long initTemp = 92247758L;



    /*
    private final long TIMEOUTSEC = 30;
    private final int alarmLmt = 80;
    private final double tempChange = 0.95;
    private final int MAXITER = 400;
    private final int MAXOUTER = 600;
    private final double percentKicked = 0.3;
    private double initTemp;
    */


    private class SolInstance {
        public SolInstance() {
            tVal = M;
            p_r = P;
            m_r = M;
            choosenArr = new boolean[N];
            containedArr = new int[N];
            blockedClsArr = new int[N];
            classTrack = new int[N];
            numContained = 0;
        }

        public SolInstance(SolInstance SolI) {
            tVal = SolI.tVal;
            p_r = SolI.p_r;
            m_r = SolI.m_r;
            choosenArr = new boolean[N];
            System.arraycopy(SolI.choosenArr, 0, choosenArr, 0, N);
            containedArr = new int[N];

            int n_c = 0;
            for (int i = 0; i < N; i += 1) {
                if (SolI.choosenArr[i]) {
                    containedArr[n_c] = i;
                    n_c += 1;
                }
            }

            numContained = SolI.numContained;

            blockedClsArr = new int[N];
            System.arraycopy(SolI.blockedClsArr, 0, blockedClsArr, 0, N);

            classTrack = new int[N];
            System.arraycopy(SolI.classTrack, 0, classTrack, 0, N);

        }

        public long p_r;
        public long m_r;
        private long tVal;
        public int numContained;
        public boolean[] choosenArr;
        public int[] blockedClsArr;
        public int[] containedArr;
        public int[] classTrack;

    }

    public SimAnSolverHeuristic(long p, long m, int n,
                                int[] ClsArr, long[]WArr, long[]CArr,
                                long[]RArr, HashSet<Integer>[] classIncArr, String fn) {
        System.out.println("Using Heuristic");


        alarmLmt = n/2;
        MAXITER = n/50;

        P = p;
        M = m;
        N = n;
        ClassArr = ClsArr;
        WeightArr = WArr;
        CostArr = CArr;
        RevArr = RArr;
        ClassIncArr = classIncArr;
        F = fn;

        initTemp = p;
        bucketStarted = false;

        //FillRevRatioPQ();
        //temp = initTemp;
        try {
            timingSolve();
        } catch (Exception e) {
            System.out.println("Timer Fail :(");
        }

    }



    /**Timer method. */
    public void timingSolve() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Task());

        try {
            //System.out.println("Started..");
            System.out.println(future.get(TIMEOUTSEC, TimeUnit.SECONDS));
            //System.out.println("Finished!");
        } catch (TimeoutException e) {
            future.cancel(true);
            //System.out.println("Terminated!");
        }

        executor.shutdownNow();
    }



    /** Wrapping Solve() */
    class Task implements Callable<String> {
        @Override
        public String call() throws Exception {
            Solve();
            return "Ready!";
        }
    }


    public void Solve() {
        System.out.println("Entering Solve....");
        overallTemp = 1;
        BestSol = CreateInitialSolution();
        for (int out = 0; out < MAXOUTER; out += 1) {
            if (out == MAXOUTER/10) {
                System.out.println("10% done");
            } else if (out == MAXOUTER/4) {
                System.out.println("25% done.");
            } else if (out == MAXOUTER/2) {
                System.out.println("50% done.");
            } else if (out == 3*MAXOUTER/4) {
                System.out.println("75% done.");
            }


            SolInstance CurSol = CreateInitialSolution();
            //SolInstance CurBestSol = CurSol;


/*            if (bucketStarted) {
                int bucketIdx = (int) (CurSol.tVal >>> bucketbit);
                edBuckets[bucketIdx] += 1;
                bucketedItem += 1;
                if (edBuckets[bucketIdx]> (2* bucketedItem/ (long)bucketsize)){
                    System.out.println("Discarting " + CurSol.tVal + ".");
                    continue;
                }

            }*/


            double temp = 1;
            for (int i = 0; i < MAXITER; i += 1) {
                if (temp < 0.5) {
                    break;
                }
                
                SolInstance Sol_i = CreateNeighborSolution(CurSol);
                //temp = temp * tempChange;
                long val_i = Sol_i.tVal;
                long val_cur = CurSol.tVal;
                if (val_i > val_cur) {
                    //System.out.println("Local improvement from " + val_cur + " to " + val_i);
                    CurSol = Sol_i;
                    //System.out.println(F + " Semi-global improvement: " + Sol_i.tVal);
                    if (val_i > BestSol.tVal) {
                        temp = 1;
                        BestSol = Sol_i;
                        System.out.println(F + " Actual global improvement: " + CurSol.tVal);
                    }
                    Sol_i = null;

/*                    if (val_i > CurSol.tVal) {
                        CurSol = Sol_i;
                        System.out.println(F + " Semi-global improvement: " + Sol_i.tVal);
                        if (val_i > BestSol.tVal) {
                            BestSol = Sol_i;
                            System.out.println(F + " Actual global improvement: " + CurSol.tVal);
                        }
                        bucketTicker += 1;
                        if (bucketTicker == 4) {
                            System.out.println("Ticking bucket with value " + Sol_i.tVal);
                            initBuckets(Sol_i.tVal);
                        }

                    }*/

                } else {
                    temp = temp*tempChange;
                }
                /*else if (temp > Math.random()) {
                    CurSol = Sol_i;
                    Sol_i = null;
                    //System.out.println("Random re-shuffle" + val_cur + " to " + val_i);
                }*/



                //System.out.println("Finish an OuterLoop with" + CurSol.tVal);

            }
            CurSol = null;
/*            if (CurBestSol.tVal > BestSol.tVal) {
                BestSol = CurBestSol;
                System.out.println(F + " Actual global improvement: " + CurSol.tVal);
                      bucketTicker += 1;
                        if (bucketTicker == 4) {
                            System.out.println("Ticking bucket with value " + Sol_i.tVal);
                            initBuckets(Sol_i.tVal);
                        }

            }
            if (CurBestSol.tVal == BestSol.tVal) {
                overallTemp *= tempChange;
                System.out.println("-------------------------------!");
            }*/
        }

        System.out.println(BestSol.tVal);
    }




/*    private void addGreedyOneItem(SolInstance S_in) {
        int eleAdd = RevRatioPQ.remove();
        if (!S_in.choosenArr[eleAdd]) {
            S_in.choosenArr[eleAdd] = true;
            S_in.numContained += 1;

            S_in.p_r -= WeightArr[eleAdd];
            S_in.m_r -= CostArr[eleAdd];
            S_in.tVal += RevArr[eleAdd];

            int clsAdd = ClassArr[eleAdd];
            S_in.classTrack[clsAdd] += 1;

            for (int e_block : ClassIncArr[clsAdd]) {
                S_in.blockedClsArr[e_block] += 1;
            }
        }

    }*/

/*    private void initBuckets(long bucketRef) {
        bucketedItem = 0;

        bucketbit = 0;
        while(bucketRef > (long)16283) {
            bucketbit += 1;
            bucketRef = bucketRef >>> 1;
        }
        bucketsize = (int) (bucketRef >>> bucketbit) << 2;
        System.out.println("The bucketsize is destined to be: " + bucketsize);
        edBuckets = new long[bucketsize];

        bucketStarted = true;
    }*/


    private void addItem(SolInstance S_in) {

        rd = new Random(System.currentTimeMillis());
        // Alarm to prevent overflow;
        int alarm1 = 0;
        //int blockBound = (int) blockingPerc * ClsN;

        OuterLoop:
        while (alarm1 < alarmLmt && S_in.p_r >= 0 && S_in.m_r >= 0) {
            int alarm2 = 0;
            int eleAdd = rd.nextInt(N);
            while (S_in.blockedClsArr[ClassArr[eleAdd]] > 0 || WeightArr[eleAdd] > S_in.p_r
                    || CostArr[eleAdd] > S_in.m_r || S_in.choosenArr[eleAdd]) {
                if (alarm2 > alarmLmt) {
                    break OuterLoop;
                }
                eleAdd = rd.nextInt(N);
                alarm2 += 1;
            }

            S_in.choosenArr[eleAdd] = true;
            S_in.numContained += 1;

            S_in.p_r -= WeightArr[eleAdd];

            S_in.m_r -= CostArr[eleAdd];
            S_in.tVal += RevArr[eleAdd];

            int clsAdd = ClassArr[eleAdd];
            S_in.classTrack[clsAdd] += 1;

            for (int e_block : ClassIncArr[clsAdd]) {
                S_in.blockedClsArr[e_block] += 1;
            }
        }


    }



    private SolInstance CreateInitialSolution() {
        SolInstance initSolution;
        initSolution = new SolInstance();
        addItem(initSolution);
        return initSolution;
    }


    private SolInstance CreateNeighborSolution(SolInstance S_cur) {
        rd = new Random(System.currentTimeMillis());

        SolInstance S_new = new SolInstance(S_cur);

        if (bumpingItem) {
            bumpItem(S_new);
        } else {
            bumpClass(S_new);
        }


        addItem(S_new);

        return S_new;
    }


    /** Bump S_new by item. */
    private void bumpItem(SolInstance S_new) {
        boolean[] result = S_new.choosenArr;
        int[] contArr = S_new.containedArr;

        int n_c = S_new.numContained;
        if (n_c > 0) {
            int bumping = Math.max(1, (int)(percentKicked* (double) n_c));

            while(bumping > 0){
                int bump = (int)(Math.random() * n_c);
                int bumpIdx = contArr[bump];
                //int bump = rd.nextInt(n_c);
                if (result[bumpIdx]) {
                    result[bumpIdx] = false;
                    S_new.numContained -= 1;

                    S_new.p_r += WeightArr[bumpIdx];
                    if (S_new.p_r > P) {
                        System.out.println("To the negative when adding item with weight" + WeightArr[bumpIdx]);
                    }
                    S_new.m_r += CostArr[bumpIdx];
                    S_new.tVal -= RevArr[bumpIdx];

                    int clsBump = ClassArr[bumpIdx];
                    S_new.classTrack[clsBump] -= 1;
                    if (S_new.classTrack[clsBump] == 0) {
                        kickRestriction(clsBump, S_new.blockedClsArr);
                    }
                    bumping -= 1;
                }
            }
        }
    }

    /** Bump S_new by class. */
    private void bumpClass(SolInstance S_new) {
        boolean[] result = S_new.choosenArr;
        //int[] classChosen = new int[N];
        boolean[] classCn = new boolean[N];
        ArrayList<Integer> clsExistList = new ArrayList<>();

        int[] contArr = S_new.containedArr;

        for (int i = 0; i < contArr.length; i += 1) {
            int cls_i = ClassArr[contArr[i]];
            //classChosen[n_i] = cls_i;
            if (!classCn[cls_i]) {
                classCn[cls_i] = true;
                clsExistList.add(cls_i);
            }
        }

        int numCls = clsExistList.size();

        if (numCls > 0) {
            int bumpingCls = Math.max(1, (int)(percentKickedCls* (double) numCls));

            while(bumpingCls > 0){
                int bumpCIndex = (int)(Math.random() * numCls);
                int bumpC = clsExistList.get(bumpCIndex);
                if (classCn[bumpC]) {
                    for (int i = 0; i < contArr.length; i += 1) {
                        if (ClassArr[i] == bumpC) {
                            result[i] = false;
                            S_new.numContained -= 1;

                            S_new.p_r += WeightArr[i];
                            S_new.m_r += CostArr[i];
                            S_new.tVal -= RevArr[i];
                        }
                    }

                    S_new.classTrack[bumpC] = 0;
                    kickRestriction(bumpC, S_new.blockedClsArr);

                    classCn[bumpC] = false;

                    bumpingCls -= 1;
                }

            }
        }

    }


    private void kickRestriction(int clsKicked, int[] blockedClsArr) {
        for (int e_release : ClassIncArr[clsKicked]) {
            blockedClsArr[e_release] -= 1;
        }
    }



    /** Return the Optimal Solution, marked by TRUE if the item is in. */
    public boolean[] getOptSolution() {
        return BestSol.choosenArr;
    }

    public long getOptVal() {return BestSol.tVal;}


    /** Comparator for revenue/cost ratio. */
    class RevComparator implements Comparator<Integer> {
        // Return 1 if obj1 is more profitable than obj2
        @Override
        public int compare(Integer obj1, Integer obj2) {
            double revRatio1 = (RevArr[obj1] / ((double) CostArr[obj1] + 1));
            double revRatio2 = (RevArr[obj2] / ((double) CostArr[obj2] + 1));
            if (revRatio1 > revRatio2) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /** Fill the Rev Ratio PQ for initial processing,
     * which will gurantee the most profitable items are at least
     * considered once.
     */
    /*
    private void FillRevRatioPQ() {
        RevRatioPQ = new PriorityQueue<>(N, new RevComparator());
        for (int i = 0; i < N; i+= 1) {
            if (RevArr[i] != 0) {
                RevRatioPQ.add(i);
            }
        }
        while (!RevRatioPQ.isEmpty()) {
            Integer ej = RevRatioPQ.poll();
            long c = CostArr[ej];
            long r = RevArr[ej];
            double rat = r/((double) c + 0.01);
            //System.out.println("Ejecting item" + ej + " with revenue ratio" + rat );
        }


    }
    */







    private long P;

    private long M;

    private int N;



    /** Mapping from item index to its class. */
    private int[] ClassArr;

    /** Mapping from item index to its weight. */
    private long[] WeightArr;


    /** Mapping from item index to its weight. */
    private long[] CostArr;


    /** Mapping from item index to its weight. */
    private long[] RevArr;


    /** PriorityQueue of Profits. */
    PriorityQueue<Integer> RevRatioPQ;


    /** The Best Choosen Solution Instance. */
    private SolInstance BestSol;


    /** Mapping from class index to HSet of class index that is incompatible. */
    private HashSet<Integer>[] ClassIncArr;

    /** The random number generator of the random functions. */
    private Random rd;


    /** The Temperature. */
    private double overallTemp;

    /** Input File Name. */
    private String F;

    /** Educated buckets. */
    private long[] edBuckets;


}
