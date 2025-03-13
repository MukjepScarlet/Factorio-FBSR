package moe.mukjep.fbsr.gui;

data class GUIBox(val x: Int, val y: Int, val width: Int, val height: Int) {

    fun cutBottom(bottom: Int): GUIBox {
        return GUIBox(x, y + (height - bottom), width, bottom)
    }

    fun cutLeft(left: Int): GUIBox {
        return GUIBox(x, y, left, height)
    }

    fun cutRight(right: Int): GUIBox {
        return GUIBox(x + (width - right), y, right, height)
    }

    fun cutTop(top: Int): GUIBox {
        return GUIBox(x, y, width, top)
    }

    fun expand(s: GUISpacing): GUIBox {
        return GUIBox(
            x - s.left,
            y - s.top,
            width + s.left + s.right,
            height + s.top + s.bottom
        )
    }

    fun expand(top: Int, left: Int, bottom: Int, right: Int): GUIBox {
        return GUIBox(x - left, y - top, width + left + right, height + top + bottom)
    }

    fun expandBottom(bottom: Int): GUIBox {
        return GUIBox(x, y, width, height + bottom)
    }

    fun expandLeft(left: Int): GUIBox {
        return GUIBox(x - left, y, width + left, height)
    }

    fun expandRight(right: Int): GUIBox {
        return GUIBox(x, y, width + right, height)
    }

    fun expandTop(top: Int): GUIBox {
        return GUIBox(x, y - top, width, height + top)
    }

    fun indexed(row: Int, col: Int): GUIBox {
        return GUIBox(x + width * col, y + height * row, width, height)
    }

    fun shrink(s: GUISpacing): GUIBox {
        return GUIBox(
            x + s.left,
            y + s.top,
            width - s.left - s.right,
            height - s.top - s.bottom
        )
    }

    fun shrink(top: Int, left: Int, bottom: Int, right: Int): GUIBox {
        return GUIBox(x + left, y + top, width - left - right, height - top - bottom)
    }

    fun shrinkBottom(bottom: Int): GUIBox {
        return GUIBox(x, y, width, height - bottom)
    }

    fun shrinkLeft(left: Int): GUIBox {
        return GUIBox(x + left, y, width - left, height)
    }

    fun shrinkRight(right: Int): GUIBox {
        return GUIBox(x, y, width - right, height)
    }

    fun shrinkTop(top: Int): GUIBox {
        return GUIBox(x, y + top, width, height - top)
    }
}