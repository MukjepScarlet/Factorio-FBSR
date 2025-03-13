package moe.mukjep.fbsr.render

import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.jvm.Throws

abstract class Renderer(
    val layer: Layer,
    val bounds: Rectangle2D.Double,
    val isIgnoreBoundsCalculation: Boolean
) {
    constructor(layer: Layer, position: Point2D.Double, ignoreBoundsCalculation: Boolean) : this(
        layer,
        Rectangle2D.Double(position.x, position.y, 0.0, 0.0),
        ignoreBoundsCalculation
    )

    @Throws(Exception::class)
    abstract fun render(g: Graphics2D)
}
