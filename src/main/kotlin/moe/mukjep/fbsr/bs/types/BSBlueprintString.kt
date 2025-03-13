package moe.mukjep.fbsr.bs.types

import com.demod.fbsr.BSUtils
import moe.mukjep.fbsr.util.inflater
import moe.mukjep.fbsr.util.readJSONObject
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.jvm.optionals.getOrNull

class BSBlueprintString(json: JSONObject) {

    val index: OptionalInt = BSUtils.optInt(json, "index")

    val blueprint: Optional<BSBlueprint> = BSUtils.opt(json, "blueprint", ::BSBlueprint)
    val blueprintBook: Optional<BSBlueprintBook> = BSUtils.opt(json, "blueprint_book", ::BSBlueprintBook)
    val upgradePlanner: Optional<BSUpgradePlanner> = BSUtils.opt(json, "upgrade_planner", ::BSUpgradePlanner)
    val deconstructionPlanner: Optional<BSDeconstructionPlanner> = BSUtils.opt(json, "deconstruction_planner", ::BSDeconstructionPlanner)

    companion object {
        // Based on https://github.com/motlin/factorio-blueprint-playground/blob/main/src/parsing/types.ts
        // Thanks FactorioBlueprints/motlin!
        @Throws(IOException::class, JSONException::class)
        @JvmStatic
        fun decodeFromBase64(text: String): JSONObject {
            val blueprintString = text.trim()
            require(blueprintString[0] == '0') { "Malformed blueprint string!" }

            // Note: encoded base64 text doesn't need UTF-8 byte[]
            return Base64.getDecoder().decode(blueprintString.substring(1).toByteArray(Charsets.ISO_8859_1))
                .inputStream().inflater().bufferedReader().readJSONObject()
        }

        /**
         * @throws NoSuchElementException when index doesn't exist or none of [BSType] matches
         */
        @Throws(NoSuchElementException::class)
        @JvmStatic
        fun decodeWithIndex(text: String): IndexedValue<BSType> {
            val base = BSBlueprintString(decodeFromBase64(text))
            val index = base.index.asInt
            return when {
                base.blueprint.isPresent -> IndexedValue(index, base.blueprint.get())
                base.blueprintBook.isPresent -> IndexedValue(index, base.blueprintBook.get())
                base.upgradePlanner.isPresent -> IndexedValue(index, base.upgradePlanner.get())
                base.deconstructionPlanner.isPresent -> IndexedValue(index, base.upgradePlanner.get())
                else -> throw NoSuchElementException("No type matches!")
            }
        }

        /**
         * @throws NoSuchElementException when none of [BSType] matches
         */
        @Throws(NoSuchElementException::class)
        @JvmStatic
        fun decode(text: String): BSType {
            val base = BSBlueprintString(decodeFromBase64(text))
            return base.blueprint.getOrNull() ?: base.blueprintBook.getOrNull() ?:
                base.upgradePlanner.getOrNull() ?: base.deconstructionPlanner.getOrNull() ?: throw NoSuchElementException("No type matches!")
        }

    }

}
