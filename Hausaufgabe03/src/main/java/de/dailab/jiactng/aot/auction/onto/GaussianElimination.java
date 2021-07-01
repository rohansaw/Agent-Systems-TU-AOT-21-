package de.dailab.jiactng.aot.auction.onto;

import java.util.HashMap;
import java.util.List;

public class GaussianElimination {
    private static final double EPSILON = 1e-10;

    //https://introcs.cs.princeton.edu/java/95linear/GaussianElimination.java.html
    // Gaussian elimination with partial pivoting
    public static double[] lsolve(double[][] A, double[] b) {
        int n = b.length;

        for (int p = 0; p < n; p++) {

            // find pivot row and swap
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }
            double[] temp = A[p];
            A[p] = A[max];
            A[max] = temp;
            double t = b[p];
            b[p] = b[max];
            b[max] = t;

            // singular or nearly singular
            if (Math.abs(A[p][p]) <= EPSILON) {
                throw new ArithmeticException("Matrix is singular or nearly singular");
            }

            // pivot within A and b
            for (int i = p + 1; i < n; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < n; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }

        // back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }
        return x;
    }

    /* WC(Resource) := weightedCost of single Resource
     * solves sum(WC(res)) = prize, with
     * WC(A) * P(A) = WC(B) * P(B) = ... = WC(K) * P(K)
     * Returns Array of WC(Resource)
     */

    public static double[] weightResources(List<Resource> res, double[] probabilities, double prize) {
        HashMap<Integer, Integer> indexMap = new HashMap<>();
        int[] count = new int[9];
        int size = 0;
        for (Resource r : res) {
            if (count[r.ordinal()] == 0) {
                indexMap.put(size, r.ordinal());
                size++;
            }
            count[r.ordinal()]++;
        }

        double[][] matrix = new double[size + 1][size + 1];
        double[] b = new double[size + 1];

        for (int row = 0; row < size; row++) {
            int row_res = indexMap.get(row);
            double coef = probabilities[row_res] / count[row_res];

            for (int col = 0; col < size; col++) {
                if (col == row) continue;
                matrix[row][col] = coef * count[indexMap.get(col)];
            }

            matrix[row][size] = 1;
            b[row] = coef * prize;
        }
        for (int i = 0; i < size; i++) {
            matrix[size][i] = count[indexMap.get(i)];
        }
        b[size] = prize;
        double[] solution = lsolve(matrix, b);
        double[] ret = new double[9];
        for (int i = 0; i < size; i++) {
            ret[indexMap.get(i)] = solution[i];
        }
        return ret;
    }
}

