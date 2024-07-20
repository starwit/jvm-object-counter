package de.starwit;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Test;

public class AppTest {

    @Test
    public void testParsing() throws Exception {
        App app = new App();

        String sampleFileName = "sample.txt";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(sampleFileName)) {
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                        String histogram = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                        app.parseHistogram(histogram);
            }
        }

        assertEquals(983L, (long) app.getCollectedStats().keySet().size());
    }
}
