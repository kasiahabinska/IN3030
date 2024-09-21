import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

class ParallelSoE {
    int n, root, cores, numOfPrimes;
    byte[] oddNumbers; // | 15 | 13 | 11 | 9 | 7 | 5 | 3 | 1 | <-- The first byte
    int[] primes;
    ArrayList<Integer> rootPrimes = new ArrayList<>();

    Semaphore lock = new Semaphore(1);
    CyclicBarrier cb;

    ParallelSoE(int n, int k) {
        this.n = n;
        root = (int) Math.sqrt(n);
        oddNumbers = new byte[(n / 16) + 1];
        cores = k;
        cb = new CyclicBarrier(cores + 1);
    }

    int[] getPrimes() {
        if (n <= 16)
            return primes;

        sieve();
        
        return collectPrimes();
    }

    private int[] collectPrimes() {
        primes = new int[numOfPrimes];
        primes[0] = 2;
        int j = 1;

        for (int i = 3; i <= n; i += 2) 
            if (isPrime(i))
                primes[j++] = i;

        return primes;
    }

    private void sieve() {
        // Sequentially find primes <= sqrt(n)
        mark(1);
        numOfPrimes = 1;
        int p = nextPrime(1);

        while (p != -1) {
            traverse(p);
            rootPrimes.add(p);
            numOfPrimes++;
            p = nextPrime(p);
        }
       
        // Parallely traverse non-primes
        int segment = (oddNumbers.length - root / 16) / cores;
        
        Worker[] threads = new Worker[cores];
        for (int i = 0; i < cores; i++) 
            threads[i] = new Worker(i, segment);
        
        for (Worker t : threads)
            new Thread(t).start();

        try {
            cb.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Worker t : threads)
            numOfPrimes += t.num;
    }

    class Worker implements Runnable {
        int id, segment, num;
        int low, high; 

        Worker(int id, int segment) {
            this.id = id;
            this.segment = segment;
            
            if (id == 0) low = root;
            else low = 16 * (oddNumbers.length - (cores - id) * segment);
            
            if (id == cores - 1) high = n;   
            else high = 16 * (oddNumbers.length - 1 - (cores - 1 - id) * segment) + 15;

            num = (high - low + (high - low) % 2) / 2;
        }

        public void run() {
            for (int prime : rootPrimes) 
                traverse(prime);
            
            try {
                cb.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void traverse(int prime) {
            if (prime * prime > high) return; 

            int j;
            if (low != prime && low % prime == 0) j = low;
            else j = low + (prime - (low % prime));
            
            if (j % 2 == 0) j += prime; 
            
            for (int i = j; i <= high; i += 2 * prime) {
                if (!isPrime(i)) continue;
                mark(i); num--;
            }
        }
    }

    private boolean isPrime(int num) {
        int bitIndex = (num % 16) / 2;
        int byteIndex = num / 16;
    
        return (oddNumbers[byteIndex] & (1 << bitIndex)) == 0;
    }

    private void mark(int num) {
        int bitIndex = (num % 16) / 2;
        int byteIndex = num / 16;
        oddNumbers[byteIndex] |= (1 << bitIndex);
    }

    private int nextPrime(int prev) {
        for (int i = prev + 2; i <= root; i += 2)
          if (isPrime(i))
            return i;
    
        return -1;
    }

    private void traverse(int prime) {
        for (int i = prime * prime; i <= root; i += prime * 2)
            mark(i);
    }

    static void printPrimes(int[] primes) {
        for (int prime : primes) 
            System.out.println(prime);
    }
}
