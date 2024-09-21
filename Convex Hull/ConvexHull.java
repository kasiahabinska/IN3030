
public class ConvexHull {
    int n, MAX_X, MAX_Y, MIN_X;
    int x[], y[];
    IntList points, left, right, convexSet;
    
    ConvexHull(int n, int seed) {
        this.n = n; 
        x = new int[n]; 
        y = new int[n];

        NPunkter17 p = new NPunkter17(n, seed);
        p.fyllArrayer(x, y);
        points = p.lagIntList();

        for (int i = 0; i < n; i++) {
            if (x[i] > x[MAX_X]) 
                MAX_X = i;
            if (x[i] < x[MIN_X]) 
                MIN_X = i;
            if (y[i] > y[MAX_Y]) 
                MAX_Y = i;
        }

        left = findPoints(MAX_X, MIN_X, points);
        right = findPoints(MIN_X, MAX_X, points);
    }
  
    IntList seq() {    
        convexSet = new IntList();

        convexSet.add(MAX_X);
        rec(MAX_X, MIN_X, furthestPoint(MAX_X, MIN_X, left), left, convexSet);
        convexSet.add(MIN_X);
        rec(MIN_X, MAX_X, furthestPoint(MIN_X, MAX_X, right), right, convexSet);
    
        return convexSet;
    }
  
    void rec(int p1, int p2, int p3, IntList m, IntList coHull) {
        IntList left = findPoints(p1, p3, m);
        IntList right = findPoints(p3, p2, m);

        int p = furthestPoint(p1, p3, left);
        int q = furthestPoint(p3, p2, right);
    
        if (p != -1) rec(p1, p3, p, left, coHull);
        coHull.add(p3);
        if (q != -1) rec(p3, p2, q, right, coHull);        
    } 
    
    // fix: skips points on the same line

    IntList findPoints(int p1, int p2, IntList m) {
        IntList res = new IntList();
        for (int i = 0; i < m.size(); i++) {
            int p = m.get(i);    
            int d = distance(p1, p2, p);
            if (d <= 0) res.add(p);
        }
        return res;
    }
  
    int distance(int p1, int p2, int p3) {
        int a = y[p1] - y[p2];
        int b = x[p2] - x[p1];
        int c = y[p2] * x[p1] - y[p1] * x[p2];
        return a * x[p3] + b * y[p3] + c;
    }
  
    int furthestPoint(int p1, int p2, IntList m) {
        int maxP = -1, maxD = 0;
        for (int i = 0; i < m.size(); i++) {
            int p = m.get(i);      
            int d = distance(p1, p2, p);
            
            if (d < maxD) {
                maxD = d;
                maxP = p;
            }  
        }
        return maxP;
    }
  
    IntList para() { 
        convexSet = new IntList();

        int level = Runtime.getRuntime().availableProcessors() / 2 - 1;
        Worker w1 = new Worker(MAX_X, MIN_X, furthestPoint(MAX_X, MIN_X, left), left, level);
        Thread t1 = new Thread(w1);
        Worker w2 = new Worker(MIN_X, MAX_X, furthestPoint(MIN_X, MAX_X, right), right, level);
        Thread t2 = new Thread(w2);
        
        t1.start(); 
        t2.start();

        try {
            t1.join(); 
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        convexSet.add(MAX_X);
        convexSet.append(w1.res);
        convexSet.add(MIN_X);
        convexSet.append(w2.res);

        return convexSet;
    }
  
    private class Worker implements Runnable {
        int p1, p2, p3, level;
        IntList m, res = new IntList();

        public Worker(int p1, int p2, int p3, IntList m, int level) {
            this.p1 = p1; this.p2 = p2; this.p3 = p3;
            this.m = m; 
            this.level = level;
        }

        @Override
        public void run() {
            IntList left = findPoints(p1, p3, m);
            IntList right = findPoints(p3, p2, m);

            int p = furthestPoint(p1, p3, left);
            int q = furthestPoint(p3, p2, right);
            
            if (level > 0 && p != -1 && q != -1) {
                Worker w1 = new Worker(p1, p3, p, left, level-1);
                Thread t1 = new Thread(w1);
                Worker w2 = new Worker(p3, p2, q, right, level-1);
                Thread t2 = new Thread(w2);
                
                t1.start(); 
                t2.start();
                
                try {
                    t1.join(); 
                    t2.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                res.append(w1.res);
                res.add(p3); 
                res.append(w2.res);
            }

            else {
                if (p != -1) rec(p1, p3, p, left, res);
                res.add(p3);
                if (q != -1) rec(p3, p2, q, right, res); 
            }
        }
    }
    
    public static void main(String[] args) {
        double[] time_seq = new double[7];
        double[] time_para = new double[7];

        ConvexHull hull = null;
        Oblig4Precode precode = null;
        int seed = 99;

        for (int n = 100; n <= 10000000; n *= 10) {
            hull = new ConvexHull(n, seed);
            for (int i = 0; i < 7; i++) {
                double t = System.nanoTime();
                hull.seq();
                time_seq[i] = (System.nanoTime() - t) / 1000000.0;

                t = System.nanoTime();
                hull.para();
                time_para[i] = (System.nanoTime() - t) / 1000000.0;
            }
            System.out.println("\nn = " + n +"\nseq: " + String.format("%.2f", time_seq[3]) + "\npara: " + String.format("%.2f", time_para[3]) + "\nspeedup: " + String.format("%.2f", time_seq[3] / time_para[3]));

            precode = new Oblig4Precode(hull, hull.convexSet);
            precode.writeHullPoints();
        }

        //precode.drawGraph();
    }
}