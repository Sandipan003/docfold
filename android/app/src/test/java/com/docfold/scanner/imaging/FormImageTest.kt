package com.docfold.scanner.imaging

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FormImageTest {
    @Test fun cropsAWidePageToTheCentre() = assertArrayEquals(intArrayOf(250, 0, 500, 1000), FormImage.centerCrop(1000, 1000, 100, 200))
    @Test fun cropsATallPageToTheCentre() = assertArrayEquals(intArrayOf(0, 375, 1000, 250), FormImage.centerCrop(1000, 1000, 400, 100))
    @Test fun picksTheHighestQualityThatFits() = assertEquals(50, FormImage.bestQuality(5000) { it * 100L })
    @Test fun usesTheBestQualityWhenEverythingFits() = assertEquals(95, FormImage.bestQuality(1_000_000) { it * 100L })
    @Test fun fallsBackToTheLowestWhenNothingFits() = assertEquals(10, FormImage.bestQuality(1) { it * 100L })

    private fun jpeg() = ByteArrayOutputStream().also { javax.imageio.ImageIO.write(BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "jpeg", it) }.toByteArray()

    @Test fun paddingReachesTheMinimumAndTheImageStillOpens() {
        val padded = FormImage.padTo(jpeg(), 200_000) // more than one comment block
        assertTrue(padded.size in 200_000..200_003)
        val image = javax.imageio.ImageIO.read(ByteArrayInputStream(padded))
        assertEquals(40, image.width)
        assertEquals(30, image.height)
    }

    @Test fun aFileAlreadyBigEnoughIsLeftAlone() {
        val original = jpeg()
        assertSame(original, FormImage.padTo(original, 10))
    }
}
