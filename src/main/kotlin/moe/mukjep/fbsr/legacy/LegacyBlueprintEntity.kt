package moe.mukjep.fbsr.legacy

import com.demod.factorio.Utils
import moe.mukjep.fbsr.legacy.LegacyDirection.Companion.fromEntityJSON
import org.json.JSONObject
import java.awt.geom.Point2D

class LegacyBlueprintEntity(val json: JSONObject) {
    val id: Int = json.getInt("entity_number")
    val name: String = json.getString("name")
    val position: Point2D.Double = Utils.parsePoint2D(json.getJSONObject("position"))
    val direction: LegacyDirection = fromEntityJSON(json)
}