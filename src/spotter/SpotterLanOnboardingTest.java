package spotter;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpotterLanOnboardingTest
{
    @TempDir
    Path files;

    @Test
    void previewsImagesAtDisplaySizeAndRejectsNonImages() throws IOException
    {
        Path image = files.resolve("picture.png");
        ImageIO.write(new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB), "png",
            image.toFile());
        SpotterLanOnboarding.Photo preview = SpotterLanOnboarding.readPhoto(image);
        assertEquals(160, preview.preview().getIconWidth());
        assertEquals(80, preview.preview().getIconHeight());
        assertArrayEquals(Files.readAllBytes(image), preview.bytes());

        Path invalid = files.resolve("invalid.jpg");
        Files.writeString(invalid, "not an image");
        assertThrows(IOException.class, () -> SpotterLanOnboarding.readPhoto(invalid));
    }

    @Test
    void rejectsLargeDimensionsBeforeDecoding() throws IOException
    {
        Path image = files.resolve("too-wide.png");
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png",
            image.toFile());
        assertThrows(IOException.class, () -> SpotterLanOnboarding.readPhoto(image));
    }
}
