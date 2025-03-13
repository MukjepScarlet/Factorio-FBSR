package moe.mukjep.fbsr.bs.types

import com.demod.fbsr.BSUtils
import com.demod.fbsr.bs.BSIcon
import moe.mukjep.fbsr.bs.base.MapVersion
import org.json.JSONObject
import java.util.*

class BSBlueprintBook(json: JSONObject) : BSType {
    val label: Optional<String> = BSUtils.optString(json, "label")
    val version: MapVersion = MapVersion(json.getInt("version").toLong())
    val description: Optional<String> = BSUtils.optString(json, "description")
    val icons: Optional<List<BSIcon>> = BSUtils.optList(json, "icons", ::BSIcon)
    val blueprints: List<BSBlueprintString> = BSUtils.list(json, "blueprints", ::BSBlueprintString)
    val activeIndex: OptionalInt = BSUtils.optInt(json, "active_index")

    val allBlueprints: List<BSBlueprint>
        get() = blueprints.flatMap {
            when {
                it.blueprint.isPresent -> listOf(it.blueprint.get())
                it.blueprintBook.isPresent -> it.blueprintBook.get().allBlueprints
                else -> emptyList()
            }
        }
}