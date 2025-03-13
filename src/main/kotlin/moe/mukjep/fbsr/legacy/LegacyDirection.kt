package moe.mukjep.fbsr.legacy

import moe.mukjep.fbsr.bs.base.Direction
import org.json.JSONObject

enum class LegacyDirection {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST;

    fun toNewDirection(): Direction {
        return Direction.valueOf(name)
    }

    companion object {
        @JvmStatic
        fun fromEntityJSON(entityJson: JSONObject): LegacyDirection {
            val dir = entityJson.optInt("direction", 0)
            return entries[dir]
        }
    }
}