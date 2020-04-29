import org.junit.Test;

public class MathExp {

    @Test
    public void printInverseFinitesTest() {
        printInverseFinites(7);
    }

    private void printInverseFinites(int decimalPrecision) {
        double ub = Math.pow(10, decimalPrecision);
        int i = 0;
        while(i < ub) {
            i += 1;
            double test = (ub/i) * Math.pow(10, decimalPrecision);
            if (((int) test) == test) {
                System.out.println(i/ub + " " + ub/i);
            }
        }
    }
}
