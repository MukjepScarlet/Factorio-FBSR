package moe.mukjep.fbsr.gui;

data class GUISize(val width: Int, val height: Int) {

	fun toBox(x: Int, y: Int): GUIBox {
		return GUIBox(x, y, width, height)
	}
}
