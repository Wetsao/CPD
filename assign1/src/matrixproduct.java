import java.util.Scanner;

public class matrixproduct {

    public static void onMult(int m_ar, int m_br) {
        double temp;

        double[] pha = new double[m_ar * m_ar];
        double[] phb = new double[m_ar * m_ar];
        double[] phc = new double[m_ar * m_ar];
        
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i * m_ar + j] = 1.0;
            }
        }

        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i * m_br + j] = i + 1;
            }
        }
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                temp = 0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i * m_ar + k] * phb[k * m_br + j];
                }
                phc[i * m_ar + j] = temp;
            }
        }
        
        long endTime = System.nanoTime();
        System.out.printf("Time: %.3f seconds\n", (endTime - startTime) / 1.0e9);
        
        System.out.println("Result matrix:");
        for(int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[j] + " ");
            }
        }
        System.out.println();
    }

    public static void onMultLine(int m_ar, int m_br) {
        // Implement line-by-line multiplication here
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int op, lin, col;
        op = 1;
        while(op != 0) {
            System.out.println("\n1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.print("Selection?: ");
            op = scanner.nextInt();

            System.out.print("Dimensions (size x size)?: ");
            lin = scanner.nextInt();
            col = lin;

            switch (op) {
                case 1:
                    onMult(lin, col);
                    break;
                case 2:
                    onMultLine(lin, col);
                    break;
            }
        }
        scanner.close();
    }
}
