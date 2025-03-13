@file:JvmName("ImageUtils")

package moe.mukjep.fbsr.util

import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.ImageOutputStream

fun BufferedImage.scale(scaleX: Double, scaleY: Double): BufferedImage {
    val transform = AffineTransform()
    transform.scale(scaleX, scaleY)
    val scaleOp = AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR)
    return scaleOp.filter(this, null)
}

fun BufferedImage.convertToRGB(): BufferedImage {
    val rgbImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g2d = rgbImage.createGraphics()
    g2d.drawImage(this, 0, 0, Color.WHITE, null) // fill transparent part with WHITE
    g2d.dispose()
    return rgbImage
}

fun BufferedImage.saveCompressedJpeg(out: OutputStream, quality: Float = 1.0F) {
    require(quality in 0.0F..1.0F) { "quality must be between 0.0 and 1.0" }

    val writers = ImageIO.getImageWritersByFormatName("jpg")
    if (!writers.hasNext()) {
        throw IllegalStateException("No writers found for JPEG format")
    }
    val writer = writers.next()
    writer.output = if (out is ImageOutputStream) out else ImageIO.createImageOutputStream(out)

    val param: ImageWriteParam = writer.defaultWriteParam
    if (param.canWriteCompressed()) {
        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        param.compressionQuality = quality
    }

    writer.write(null, IIOImage(this.convertToRGB(), null, null), param)

    writer.dispose()
}
