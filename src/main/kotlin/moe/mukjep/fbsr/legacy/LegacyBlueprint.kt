package moe.mukjep.fbsr.legacy

import com.demod.fbsr.BSUtils
import org.json.JSONObject
import java.util.*

class LegacyBlueprint(val json: JSONObject) {
    val entities: List<LegacyBlueprintEntity> = BSUtils.list(json, "entities", ::LegacyBlueprintEntity)
    val tiles: List<LegacyBlueprintTile> = BSUtils.list(json, "tiles", ::LegacyBlueprintTile)
    val label: Optional<String> = BSUtils.optString(json, "label")
}