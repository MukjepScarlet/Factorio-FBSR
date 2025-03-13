package moe.mukjep.fbsr.render

import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.jvm.Throws

abstract class EntityRenderer : Renderer {
    constructor(layer: Layer, position: Point2D.Double, ignoreBoundsCalculation: Boolean) : super(
        layer, Rectangle2D.Double(position.x, position.y, 0.0, 0.0), ignoreBoundsCalculation
    )

    constructor(layer: Layer, bounds: Rectangle2D.Double, ignoreBoundsCalculation: Boolean) : super(
        layer, bounds, ignoreBoundsCalculation
    )

    @Throws(Exception::class)
    abstract fun renderShadows(g: Graphics2D)
}