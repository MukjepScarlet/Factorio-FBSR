package moe.mukjep.fbsr.gui.part

import moe.mukjep.fbsr.gui.GUIBox
import moe.mukjep.fbsr.gui.feature.GUISliceFeature
import moe.mukjep.fbsr.gui.feature.GUIStaticFeature
import java.awt.Graphics2D
import java.util.*

class GUIPanel : GUIPart {
    val outer: Optional<GUISliceFeature>
    val inner: Optional<GUISliceFeature>
    val stat: Optional<GUIStaticFeature>

    constructor(box: GUIBox, inner: GUISliceFeature) : super(box) {
        this.outer = Optional.empty()
        this.inner = Optional.of(inner)
        this.stat = Optional.empty()
    }

    constructor(box: GUIBox, inner: GUISliceFeature, outer: GUISliceFeature) : super(box) {
        this.outer = Optional.of(outer)
        this.inner = Optional.of(inner)
        this.stat = Optional.empty()
    }

    constructor(box: GUIBox, stat: GUIStaticFeature) : super(box) {
        this.outer = Optional.empty()
        this.inner = Optional.empty()
        this.stat = Optional.of(stat)
    }

    override fun render(g: Graphics2D) {
        if (inner.isPresent) {
            inner.get().render(g, box)
        }
        if (outer.isPresent) {
            outer.get().render(g, box)
        }
        if (stat.isPresent) {
            stat.get().render(g, box)
        }
    }
}
