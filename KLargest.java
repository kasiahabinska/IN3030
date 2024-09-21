import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

public class KLargest {
    int n, k;
    int[] a1, a2; 
    static int cores = Runtime.getRuntime().availableProcessors();
    Semaphore lock = new Semaphore(1);
    CyclicBarrier cb = new CyclicBarrier(cores + 1);

    public static void main(String[] args) {
        System.out.println("Available processors: " + cores + "\n"); 

        KLargest kl = new KLargest();
        for (int i = 1000; i <= 100000000; i *= 10) {
            kl.execute(20, i);
            kl.execute(100, i);
        }
    }

    void execute(int kx, int nx) {
        k = kx; 
        n = nx;
        long t = 0;
        double[] naive = new double[7];
        double[] smart = new double[7];
        double[] parallel = new double[7];

        for (int i = 0; i < 7; i++) {
            a1 = random(n);                     
            t = System.nanoTime();
            Arrays.sort(a1);
            naive[i] = (System.nanoTime() - t) / 1000000.0;

            a2 = random(n); 
            t = System.nanoTime();
            insertSort(a2, k, n);
            smart[i] = (System.nanoTime() - t) / 1000000.0;

            a2 = random(n); 
            t = System.nanoTime();
            parallel();
            parallel[i] = (System.nanoTime() - t) / 1000000.0;
        }

        System.out.println("k = " + k + ", n = " + n + 
                        "\nnaive: " + naive[3] + " ms" +
                        "\nsmart: " + smart[3] + " ms" +
                        "\nparallel: " + parallel[3] + " ms\n");

        int[] ra1 = reverse(a1); 
        boolean b = true;
        for (int i = 0; i < k; i++) {
            if (a2[i] == ra1[i]) b = true;
            else b = false;
        }
        if (!b) System.out.println("ERROR: incorrect k order");
    }

    int[] reverse(int[] a) {
        for (int i = 0; i < a.length / 2; i++) {
            int t = a[i]; 
            a[i] = a[a.length - i - 1];
            a[a.length - i - 1] = t;
        }
        return a;
    }

    int[] random(int n) {
        Random r = new Random(7363);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            int next = r.nextInt(n);
            a[i] = next;
        }  
        return a;
    }

    void insertSort(int[] a, int k) {
        for (int j = 0; j < k - 1; j++) {
            int t = a[j + 1];
            int i = j;
            while (i >= 0 && t > a[i]) {
                a[i + 1] = a[i];
                i--;
            }
            a[i + 1] = t;
        } 
    }

    void insertSort(int[] a, int k, int n) {
        insertSort(a, k);
        for (int j = k; j < n; j++) {
            if (a[j] > a[k - 1]) {
                int t = a[k - 1];
                a[k - 1] = a[j];
                a[j] = t;
                insertSort(a, k);
            }
        }
    }

    void parallel() {
        int l = a2.length - k;
        int sizeOfSegment = l / cores;
        int start, end;
        Worker[] threads = new Worker[cores];

        for (int i = 0; i < cores; i++) {
            start = k + (i * sizeOfSegment);
            end = k + ((i == cores - 1) ? (l - k) : (i + 1) * sizeOfSegment);
            threads[i] = new Worker(start, end);
            new Thread(threads[i]).start();
        }

        try {
            cb.await();
        } catch (Exception e) {
           e.printStackTrace();
        }

        insertSort(a2, k);
        for (Worker w: threads) {
            for (int j = w.start; j < w.sk - 1; j++) {
                if (a2[j] > a2[k - 1]) {
                    int t = a2[k - 1];
                    a2[k - 1] = a2[j];
                    a2[j] = t;
                    insertSort(a2, k);
                }
            }
        }
    }

    class Worker implements Runnable {
        int start, end, sk;

        Worker(int s, int e) {
            start = s; end = e; 
            sk = start + k;
        }

        public void run() {
            for (int j = start; j < sk - 1; j++) {
                int t = a2[j + 1];
                int i = j;
                while (i >= 0 && t > a2[i]) {
                    a2[i + 1] = a2[i];
                    i--;
                }
                a2[i + 1] = t;
            }

            insertSort(a2, k);
            for (int j = sk; j < end; j++) {
                if (a2[j] > a2[k - 1]) {
                    try {
                        lock.acquire();
                        int t = a2[k - 1];
                        a2[k - 1] = a2[j];
                        a2[j] = t;
                        insertSort(a2, k);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.release();
                    }
                }
            }
            try {
                cb.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
