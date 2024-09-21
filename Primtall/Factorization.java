import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

class Factorization {
    long n;
    int cores;
    int[] primes;
    ArrayList<Long> factors = new ArrayList<>();
    Semaphore lock = new Semaphore(1);
    CyclicBarrier cb;
    long tmp;

    Factorization(long n, int k, int[] primes) {
        this.n = n;
        this.primes = primes;
        cores = k;
        cb = new CyclicBarrier(cores + 1);
        tmp = n;
    }

    ArrayList<Long> seq() {
        for (int prime : primes) {
            while (tmp % prime == 0) {
                factors.add((long)prime);
                tmp /= prime;
            }
        }
        if (tmp > 1) factors.add(tmp);
        
        return factors;
    }

    ArrayList<Long> para() {
        for (int i = 0; i < cores; i++) 
            new Thread(new Worker(i)).start();

        try {
            cb.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tmp > 1) factors.add(tmp);

        return factors;
    }

    class Worker implements Runnable {
        int id;

        Worker(int id) {
            this.id = id;
        }

        public void run() {
            for (int i = id; i < primes.length; i += cores) {
                int prime = primes[i];
                
                if (tmp % prime == 0) {
                    try {
                        lock.acquire();
                        while (tmp % prime == 0) {
                            factors.add((long)prime);
                            tmp /= prime;
                        }
                    }
                    catch (InterruptedException e) {
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

    static void printFactors(ArrayList<Long> factors) {
        for (long f : factors) 
            System.out.println(f);
    }
}
