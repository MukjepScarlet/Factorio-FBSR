package moe.mukjep.fbsr.bs.types

import com.demod.fbsr.BSUtils
import com.demod.fbsr.bs.*
import moe.mukjep.fbsr.bs.base.MapVersion
import moe.mukjep.fbsr.legacy.LegacyBlueprint
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class BSBlueprint(json: JSONObject) : BSBase() {
    val label: Optional<String>
    val version: MapVersion = MapVersion(json.getLong("version"))
    val description: Optional<String>
    val icons: List<BSIcon>
    val entities: List<BSMetaEntity>
    val tiles: List<BSTile>
    val schedules: List<BSSchedule>
    val parameters: List<BSParameter>
    val snapToGrid: Optional<BSPosition>
    val isAbsoluteSnapping: Boolean
    val wires: List<BSWire>

    init {
        if (version < MapVersion(2, 0, 0, 0)) {
            val legacyBlueprint = LegacyBlueprint(json)

            // TODO look at older blueprints and see if I can extract more info
            label = legacyBlueprint.label
            description = Optional.empty()
            icons = emptyList()
            entities = legacyBlueprint.entities.map(::BSMetaEntity)
            tiles = legacyBlueprint.tiles.map(::BSTile)
            schedules = emptyList()
            parameters = emptyList()
            snapToGrid = Optional.empty()
            isAbsoluteSnapping = false
            wires = emptyList() // TODO
        } else {
            label = BSUtils.optString(json, "label")
            description = BSUtils.optString(json, "description")
            icons = BSUtils.list(json, "icons", ::BSIcon)
            entities = BSUtils.list(json, "entities", ::BSMetaEntity)
            tiles = BSUtils.list(json, "tiles", ::BSTile)
            schedules = BSUtils.list(json, "schedules", ::BSSchedule)
            parameters = BSUtils.list(json, "parameters", ::BSParameter)
            snapToGrid = BSUtils.optPosition(json, "snap_to_grid")
            isAbsoluteSnapping = json.optBoolean("absolute_snapping")

            wires = if (json.has("wires")) {
                json.getJSONArray("wires").map { BSWire(it as JSONArray) }
            } else {
                emptyList()
            }
        }
    }
}