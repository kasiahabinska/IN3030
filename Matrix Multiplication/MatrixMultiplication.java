import java.util.concurrent.CyclicBarrier;

public class MatrixMultiplication {
	double[][] a, b, c;
	int seed, n;
	int cores = Runtime.getRuntime().availableProcessors();
	CyclicBarrier cb = new CyclicBarrier(cores + 1);

	public static void main(String[] args) {
		MatrixMultiplication matrix = new MatrixMultiplication();
		System.out.println("n \tSEQ_NOT \tPARA_NOT \tspeedup \tSEQ_A \t\tPARA_A \t\tspeedup \tSEQ_B \t\tPARA_B \t\tspeedup");
		int[] values = {100, 200, 500, 1000};
		for (int n : values) 
			matrix.execute(42, n);
	}

	void execute(int seed, int n) {
		this.seed = seed; this.n = n;
		a = Oblig2Precode.generateMatrixA(seed, n);
		b = Oblig2Precode.generateMatrixB(seed, n);

		double[][] seq_not = new double[n][n];
		double[][] seq_a = new double[n][n];
		double[][] seq_b = new double[n][n];
		double[][] para_not = new double[n][n];
		double[][] para_a = new double[n][n];
		double[][] para_b = new double[n][n];

		double[] time_sn = new double[7];
		double[] time_sa = new double[7];
		double[] time_sb = new double[7];
		double[] time_pn = new double[7];
		double[] time_pa = new double[7];
		double[] time_pb = new double[7];

		for (int i = 0; i < 7; i++) {
			long t = System.nanoTime();
			seq_not = seq_not_transposed();
			time_sn[i] = (System.nanoTime() - t) / 1000000.0;

			t = System.nanoTime();
			seq_a = seq_a_transposed();
			time_sa[i] = (System.nanoTime() - t) / 1000000.0;

			t = System.nanoTime();
			seq_b = seq_b_transposed();
			time_sb[i] = (System.nanoTime() - t) / 1000000.0;

			t = System.nanoTime();
			para_not = para(Oblig2Precode.Mode.PARA_NOT_TRANSPOSED);
			time_pn[i] = (System.nanoTime() - t) / 1000000.0;

			t = System.nanoTime();
			para_a = para(Oblig2Precode.Mode.PARA_A_TRANSPOSED);
			time_pa[i] = (System.nanoTime() - t) / 1000000.0;

			t = System.nanoTime();
			para_b = para(Oblig2Precode.Mode.PARA_B_TRANSPOSED);
			time_pb[i] = (System.nanoTime() - t) / 1000000.0;
		}
		
		System.out.printf("%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\n", n, 
				time_sn[3], time_pn[3], time_sn[3] / time_pn[3], 
				time_sa[3], time_pa[3], time_sa[3] / time_pa[3], 
				time_sb[3], time_pb[3], time_sb[3] / time_pb[3]);

		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.SEQ_NOT_TRANSPOSED, seq_not);
		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.SEQ_A_TRANSPOSED, seq_a);
		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.SEQ_B_TRANSPOSED, seq_b);
		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.PARA_NOT_TRANSPOSED, para_not);
		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.PARA_A_TRANSPOSED, para_a);
		Oblig2Precode.saveResult(seed, Oblig2Precode.Mode.PARA_B_TRANSPOSED, para_b);

		if (!compare(seq_not, para_not) || 
		    !compare(seq_a, para_a) || 
			!compare(seq_b, para_b)) 
			System.out.println("error: seq != para");
	}

	boolean compare(double[][] a, double[][] b) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < b.length; j++) {
				if (Math.round(a[i][j]) != Math.round(b[i][j])) 
					return false;
			}
		}
		return true;
	} 


    double[][] seq_not_transposed() {
		c = new double[n][n];
		for (int i = 0; i < n; i++) 
			for (int j = 0; j < n; j++) 
				for (int k = 0; k < n; k++) 
					c[i][j] += a[i][k] * b[k][j]; 
		return c;
    }

	double[][] seq_a_transposed() {
		c = new double[n][n];
		for (int i = 0; i < n; i++) 
			for (int j = 0; j < n; j++) 
				for (int k = 0; k < n; k++) 
					c[i][j] += a[k][i] * b[k][j]; 
		return c;
	}

	double[][] seq_b_transposed() {
		c = new double[n][n];
		for (int i = 0; i < n; i++) 
			for (int j = 0; j < n; j++) 
				for (int k = 0; k < n; k++) 
					c[i][j] += a[i][k] * b[j][k]; 
		return c;
	}

	double[][] para(Oblig2Precode.Mode mode) {
		c = new double[n][n];

		int start, end;
		int segment = n / cores;

		Thread[] threads = new Thread[cores];

		for (int i = 0; i < cores; i++) {
			start = i * segment;
			if (i == cores - 1) 
				end = n;
			else end = start + segment;
			threads[i] = new Thread(new Worker(mode, start, end));
		}

		for (Thread t : threads) 
			t.start();
		try {
			cb.await();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return c;
	}	

	class Worker implements Runnable {
		int start, end;
		Oblig2Precode.Mode mode;

		Worker(Oblig2Precode.Mode m, int s, int e) {
			mode = m; 
			start = s; 
			end = e; 
		}

		public void run() {
			switch (mode) {
			case Oblig2Precode.Mode.PARA_NOT_TRANSPOSED:
				for (int i = start; i < end; i++) 
					for (int j = 0; j < n; j++) 
						for (int k = 0; k < n; k++)
							c[i][j] += a[i][k] * b[k][j]; 
				break;
			case Oblig2Precode.Mode.PARA_A_TRANSPOSED:
				for (int i = start; i < end; i++) 
					for (int j = 0; j < n; j++) 
						for (int k = 0; k < n; k++)
							c[i][j] += a[k][i] * b[k][j];
				break;
			case Oblig2Precode.Mode.PARA_B_TRANSPOSED:
				for (int i = start; i < end; i++) 
					for (int j = 0; j < n; j++) 
						for (int k = 0; k < n; k++)
							c[i][j] += a[i][k] * b[j][k]; 
				break;
			}	
				
			try {
				cb.await();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}