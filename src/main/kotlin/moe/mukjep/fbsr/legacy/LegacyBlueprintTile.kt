package moe.mukjep.fbsr.legacy

import com.demod.factorio.Utils
import org.json.JSONObject
import java.awt.geom.Point2D

class LegacyBlueprintTile(val json: JSONObject) {
    val name: String = json.getString("name")
    val position: Point2D.Double = Utils.parsePoint2D(json.getJSONObject("position"))
}