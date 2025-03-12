@file:JvmName("-kt")

package moe.mukjep.fbsr

import com.demod.factorio.Config
import org.json.JSONObject
import java.util.*

fun FBSR.setGameDir(path: String) {
    val config = Config::class.java.getDeclaredField("config")
    config.isAccessible = true
    config[null] = JSONObject(Collections.singletonMap("factorio", path))
}
