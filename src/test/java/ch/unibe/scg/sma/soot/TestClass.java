package ch.unibe.scg.sma.soot;

public class TestClass {

	/**
	 * @param(a, b) the values used to calculate the divisor
	 * 
	 * @return the greatest common divisor of a and b
	 **/
	int gcd(int a, int b) {
		int c = a;
		int d = b;
		int e = b;
		if (c == 0) {
			return d;
		}
		while (d != 0) {
			if (c > d) {
				c = c - d;
			} else {
				d = d - c;
			}
		}
		return c;
	}

	int fibonacciRecursive(int n) {
		if (n <= 1) {
			return n;
		}
		return fibonacciRecursive(n - 2) + fibonacciRecursive(n - 1);
	}

	int fibonacciIterative(int n) {
		if (n <= 1) {
			return n;
		}
		int next = 1;
		int previous = 1;
		for (int i = 2; i < n; i++) {
			int tmp = next;
			next += previous;
			previous = tmp;
		}
		return next;
	}

}
