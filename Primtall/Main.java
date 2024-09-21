import java.util.ArrayList;

class Main {
    public static void main(String[] args) {
        int n, k;

        if (args.length != 2 || Integer.parseInt(args[0]) < 16) {
            System.out.println("Correct use of program is: " + "java ParallelSoE <n> <#threads>");
            System.exit(-1);
        }

        n = Integer.parseInt(args[0]);
        k = Integer.parseInt(args[1]);
        if (k == 0) k = Runtime.getRuntime().availableProcessors();

        System.out.println("n = " + n);
        SieveOfEratosthenes sieve_seq = new SieveOfEratosthenes(n);
        ParallelSoE sieve_para = new ParallelSoE(n, k);
        Factorization factor;
        int[] primes_seq, primes_para;
        ArrayList<Long> factors_seq, factors_para;

        double[] sieve_seq_time = new double[7];
        double[] sieve_para_time = new double[7];
        double[] factors_seq_time = new double[7];
        double[] factors_para_time = new double[7];

        long n_squared = (long)n * n;

        for (int i = 0; i < 7; i++) {
            long t = System.nanoTime();
            primes_seq = sieve_seq.getPrimes();
            sieve_seq_time[i] = (System.nanoTime() - t) / 1000000.0;

            t = System.nanoTime();
            primes_para = sieve_para.getPrimes();
            sieve_para_time[i] = (System.nanoTime() - t) / 1000000.0;

            checkPrimes(primes_seq, primes_para);
            /*if (i == 0) {
                sieve_seq.printPrimes(primes_seq);
                sieve_para.printPrimes(primes_para);*/

            Oblig3Precode precode_seq = new Oblig3Precode(n);
            Oblig3Precode precode_para = new Oblig3Precode(n);

            for (long j = 1; j < 101; j++) {
                long tmp = n_squared - j;

                t = System.nanoTime();
                factor = new Factorization(tmp, k, primes_seq);
                factors_seq = factor.seq();
                factors_seq_time[i] = (System.nanoTime() - t) / 1000000.0;

                t = System.nanoTime();
                factor = new Factorization(tmp, k, primes_seq);
                factors_para = factor.para();
                factors_para_time[i] = (System.nanoTime() - t) / 1000000.0;
                
                if (i == 0) {
                    for (long f : factors_seq) 
                        precode_seq.addFactor(tmp, f);
                    precode_seq.writeFactors();

                    for (long f : factors_para)
                        precode_para.addFactor(tmp, f);
                    precode_para.writeFactors();
                }
            }
        }

        System.out.println("Sieve of Eratosthenes");
        System.out.println("seq: " + sieve_seq_time[3] + " ms, para: " + sieve_para_time[3] + " ms, speedup: " + sieve_seq_time[3] / sieve_para_time[3]);
        System.out.println("\nFactorization");
        System.out.println("seq: " + factors_seq_time[3]  + " ms, para: " + factors_para_time[3] + " ms, speedup: " + factors_seq_time[3] / factors_para_time[3]);
    }

    static void checkPrimes(int[] seq, int[] para) {
        for (int i = 0; i < seq.length; i++) 
            if (seq[i] != para[i])
                System.out.println("Primes are not equal");
    }
}
