import java.util.concurrent.*; 
import java.util.ArrayList;
import java.lang.Math.*;

class WaitAndSwap {

    static Semaphore s1 = new Semaphore(1, true);
    static Semaphore s2 = new Semaphore(0, true);
    static ArrayList<Integer> enterOrder = new ArrayList<>();
    static ArrayList<Integer> exitOrder = new ArrayList<>();
    static int counter = 0; 

    static int N = 4; // number of iterations
    static int debuglevel = 9;  // 4: varispeed resumption; 3: varispeed delay time; 2: sems values; 1, : (not implemented) 
    static boolean variableSpeedThreads = true;
    static double variableSpeedRate = 0.0; // threads sleep for a random time between 0 and this rate in milliseconds
    static int extraSlowThreads = 0; // number of threads that sleep 10x variableSpeedRate

    public static void printSems(int id, int iteration) {
        if (debuglevel > 1) System.out.println("Thread " + id + ", " + iteration + "    S1 "+ s1.availablePermits() + 
              " Q: " + s1.getQueueLength() +
          "; S2 " + s2.availablePermits() + " Q: " + s2.getQueueLength());
    }
  
    public static void variSpeed(int id, int iteration) { // let the calling thread sleep a random time
        long myWait = (long) (Math.random() * variableSpeedRate);
        if (variableSpeedRate == 0.0) return;
        if (id < extraSlowThreads) myWait = (long) (variableSpeedRate * 10.0); 
        // make the first <extraSlowThreads> always wait 10xvariableSpeedRate
        debugPrintln(id, iteration, 3, "         variSpeed delay: " + myWait + " ms");
        if (variableSpeedThreads) 
           try {
              TimeUnit.MILLISECONDS.sleep(myWait);
           } catch (Exception e) { return;}; 
        debugPrintln(id, iteration, 4, "         resuming after variSpeed delay");
    }
  
    public static void debugPrintln(int id, int iteration, int buglevel, String msg) {
        if (debuglevel >= buglevel) {  // then print the message
           System.out.println("Thread " + id + ", " + iteration + msg); printSems(id, iteration); 
        }     
    }

    static void waitAndSwap(int id, int iteration) {
        try {  
                                    variSpeed(id, iteration); 
                                    debugPrintln(id, iteration, 1, " START waitAndSwap"); 
            s1.acquire();
            enterOrder.add(id);
            counter++;
            s1.release();

            if (counter % 2 != 0) {
                s2.acquire();
                                    //debugPrintln(id, iteration, 1, " WAIT for the next thread to RELEASE"); 
            } else {
                s2.release();  
                                    //debugPrintln(id, iteration, 1, " RELEASE the previous thread"); 
            }
            
            s1.acquire();
            exitOrder.add(id);
            counter++;
            s1.release();
                                    debugPrintln(id, iteration, 1, " END waitAndSwap");
                                    variSpeed(id, iteration); 
                                    
        } catch (InterruptedException e) { return; }
    }

    public static void main(String[] args) {
        int numberofthreads = 3;

        if ( args.length < 1 )	{
            System.out.println("use: java WaitAndSwap <number of threads> <num iterations> <debug level> <varispeed> <num of extra slow threads>");
            System.out.println("   only the first argument, number of threads, is required; defaults are:");
            System.out.println("   iterations: " + N);
            System.out.println("   debugLevel: " + debuglevel);
            System.out.println("   variableSpeedRate: " + variableSpeedRate);
            System.out.println("   extraSlowThreads: " + extraSlowThreads);
            System.exit(0);
        }

        if ( args.length >= 1) 
            numberofthreads = Integer.parseInt(args[0]);
        
        if ( args.length >= 2) 
            N = Integer.parseInt(args[1]);
        
        if ( args.length >= 3) 
            debuglevel = Integer.parseInt(args[2]);
        
        if ( args.length >= 4) 
            variableSpeedRate = (double) Integer.parseInt(args[3]);
        
        if ( args.length >= 5) 
            extraSlowThreads = Integer.parseInt(args[4]);
        
        if (variableSpeedRate <= 0.0)   
            variableSpeedThreads = false;

        System.out.println("Number of threads: " + numberofthreads + ";  iterations: " + N + ";  debug: " + debuglevel + ";  varispeed: " + ((long) variableSpeedRate) + " ms;  extra slow: " + extraSlowThreads);

        Thread[] threads = new Thread[numberofthreads];
        for (int i = 0; i < numberofthreads; i++) 
            (threads[i] = new Thread( new Worker(i) )).start();
        
        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) { return; }

        String s = "\nEnter order: ";
        for (int i = 0; i < enterOrder.size(); i++)
            s += enterOrder.get(i) + " ";
        s += "\n Exit order: ";
        for (int i = 0; i < exitOrder.size(); i++)
            s += exitOrder.get(i) + " ";
        System.out.println(s);
    }

    static class Worker implements Runnable {
        int id;

        public Worker(int i) {
            id = i;
        }

        public void run() {
                                            debugPrintln(id, 0, 1, " START thread");   
            for (int i = 0; i < N ; i++) {
                                            debugPrintln(id, i, 2, " START iteration");  
                waitAndSwap(id, 0);
                                            debugPrintln(id, i, 2, " END iteration");
            }
                                            debugPrintln(id, 0, 1, " END thread"); 
        }                  
    }
} 