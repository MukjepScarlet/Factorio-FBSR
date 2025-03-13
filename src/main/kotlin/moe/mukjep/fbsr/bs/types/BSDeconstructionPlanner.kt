package moe.mukjep.fbsr.bs.types

import com.demod.fbsr.BSUtils
import com.demod.fbsr.bs.BSFilter
import com.demod.fbsr.bs.BSIcon
import moe.mukjep.fbsr.bs.base.MapVersion
import org.json.JSONObject
import java.util.*

class BSDeconstructionPlanner(json: JSONObject) : BSType {
    val label: Optional<String> = BSUtils.optString(json, "label")
    val version: MapVersion = MapVersion(json.getInt("version").toLong())
    val description: Optional<String>
    val icons: List<BSIcon>
    val entityFilters: List<BSFilter>
    val entityFilterMode: Int // 0 allow, 1 deny
    val tileFilters: List<BSFilter>
    val tileSelectionMode: Int // 1 default, 2 never, 3 always, ??? only TODO
    val isTreesAndRocksOnly: Boolean

    // TODO recognize special entities/tiles
    // - Entity ghost
    // - Item on ground
    // - Item request slot
    // - Tile ghost
    init {
        // TODO what are the defaults?
        val jsonSettings = json.optJSONObject("settings")
        if (jsonSettings != null) {
            description = BSUtils.optString(jsonSettings, "description")
            icons = BSUtils.list(jsonSettings, "icons", ::BSIcon)
            entityFilters = BSUtils.list(jsonSettings, "entity_filters", ::BSFilter)
            entityFilterMode = jsonSettings.optInt("entity_filter_mode")
            tileFilters = BSUtils.list(jsonSettings, "tile_filters", ::BSFilter)
            tileSelectionMode = jsonSettings.optInt("tile_selection_mode")
            isTreesAndRocksOnly = jsonSettings.optBoolean("trees_and_rocks_only")
        } else {
            description = Optional.empty()
            icons = emptyList()
            entityFilters = emptyList()
            entityFilterMode = 0
            tileFilters = emptyList()
            tileSelectionMode = 0
            isTreesAndRocksOnly = false
        }
    }
}