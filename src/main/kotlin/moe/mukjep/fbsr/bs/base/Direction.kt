package moe.mukjep.fbsr.bs.base

import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

enum class Direction(private val symbol: String, val dx: Int, val dy: Int) {
    NORTH("N", 0, -1),  //
    NORTHEAST("NE", 1, -1),  //
    EAST("E", 1, 0),  //
    SOUTHEAST("SE", 1, 1),  //
    SOUTH("S", 0, 1),  //
    SOUTHWEST("SW", -1, 1),  //
    WEST("W", -1, 0),  //
    NORTHWEST("NW", -1, -1);

    fun adjCode(): Int {
        return 1 shl ordinal
    }

    fun back(): Direction {
        return rotate(4)
    }

    fun backLeft(): Direction {
        return rotate(-3)
    }

    fun backRight(): Direction {
        return rotate(3)
    }

    fun cardinal(): Int {
        return ordinal / 2
    }

    fun frontLeft(): Direction {
        return rotate(-1)
    }

    fun frontRight(): Direction {
        return rotate(1)
    }

    val orientation: Double
        get() = ordinal / 8.0

    val isCardinal: Boolean
        get() = (ordinal % 2) == 0

    val isHorizontal: Boolean
        get() = this == EAST || this == WEST

    val isVertical: Boolean
        get() = this == NORTH || this == SOUTH

    fun left(): Direction {
        return rotate(-2)
    }

    fun offset(): Point2D.Double {
        return Point2D.Double(dx.toDouble(), dy.toDouble())
    }

    fun offset(distance: Double): Point2D.Double {
        return Point2D.Double(distance * dx, distance * dy)
    }

    fun offset(pos: Point2D.Double): Point2D.Double {
        return Point2D.Double(pos.x + dx, pos.y + dy)
    }

    fun offset(pos: Point2D.Double, distance: Double): Point2D.Double {
        return Point2D.Double(pos.x + distance * dx, pos.y + distance * dy)
    }

    fun offset(pos: Point2D.Double, offset: Point2D.Double): Point2D.Double {
        return offset(right().offset(pos, offset.y), offset.x)
    }

    fun offset(rect: Rectangle2D.Double, distance: Double): Rectangle2D.Double {
        return Rectangle2D.Double(rect.x + distance * dx, rect.y + distance * dy, rect.width, rect.height)
    }

    fun right(): Direction {
        return rotate(2)
    }

    fun rotate(dir: Direction): Direction {
        return rotate(dir.ordinal)
    }

    fun rotate(deltaIndex: Int): Direction {
        val values = entries.toTypedArray()
        return values[(((ordinal + deltaIndex) % values.size) + values.size) % values.size]
    }

    fun rotateBounds(bounds: Rectangle2D?): Rectangle2D {
        val at = AffineTransform()
        at.rotate(Math.PI * 2.0 * ordinal / 8.0)
        return at.createTransformedShape(bounds).bounds2D
    }

    fun rotatePoint(point: Point2D.Double?): Point2D.Double {
        val at = AffineTransform()
        at.rotate(Math.PI * 2.0 * ordinal / 8.0)
        val ret = Point2D.Double()
        at.deltaTransform(point, ret)
        return ret
    }

    companion object {
        @JvmStatic
        fun fromCardinal(cardinal: Int): Direction {
            return entries[cardinal * 2]
        }

        @JvmStatic
        fun fromSymbol(symbol: String): Direction {
            return entries.firstOrNull { it.symbol == symbol } ?: throw IllegalArgumentException("Unknown symbol \"$symbol\"")
        }
    }
}