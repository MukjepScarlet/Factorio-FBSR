package moe.mukjep.fbsr.bs.types

import com.demod.fbsr.BSUtils
import com.demod.fbsr.bs.BSIcon
import com.demod.fbsr.bs.BSUpgradeMapping
import moe.mukjep.fbsr.bs.base.MapVersion
import org.json.JSONObject
import java.util.*

class BSUpgradePlanner(json: JSONObject) {
    val label: Optional<String> = BSUtils.optString(json, "label")
    val version: MapVersion = MapVersion(json.getInt("version").toLong())
    val description: Optional<String>
    val icons: Optional<List<BSIcon>>
    val mappers: List<BSUpgradeMapping>

    init {
        val jsonSettings = json.optJSONObject("settings")
        if (jsonSettings != null) {
            description = BSUtils.optString(jsonSettings, "description")
            icons = BSUtils.optList(jsonSettings, "icons", ::BSIcon)
            mappers = BSUtils.list(jsonSettings, "mappers", ::BSUpgradeMapping)
        } else {
            description = Optional.empty()
            icons = Optional.empty()
            mappers = emptyList()
        }
    }
}