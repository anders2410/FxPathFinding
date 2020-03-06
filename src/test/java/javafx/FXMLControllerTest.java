package javafx;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class FXMLControllerTest {

    FXMLController fxmlController;
    Random random;

    @Before
    public void setUp() {
        fxmlController = new FXMLController();
        random = new Random(3);
    }

    @Test
    public void testMercatorX() {
        for (int i = 0; i < 100; i++) {
            double cord = random.nextDouble();
            double cordCopy = fxmlController.invMercatorX(fxmlController.mercatorX(cord));
            assertEquals(cordCopy, cord, 0.000000000000001);
        }
    }

    @Test
    public void testMercatorY() {
        for (int i = 0; i < 100; i++) {
            double cord = random.nextDouble();
            double cordCopy = fxmlController.invMercatorY(fxmlController.mercatorY(cord));
            assertEquals(cordCopy, cord, 0.00000000000001);
        }
    }
}
