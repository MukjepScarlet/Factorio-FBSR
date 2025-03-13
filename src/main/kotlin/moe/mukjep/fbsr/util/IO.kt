@file:JvmName("-io")
@file:Suppress("NOTHING_TO_INLINE")

package moe.mukjep.fbsr.util

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.io.Reader
import java.util.zip.InflaterInputStream

inline fun InputStream.inflater(): InflaterInputStream = InflaterInputStream(this)

@Throws(JSONException::class)
inline fun Reader.readJSONObject(): JSONObject = use { JSONObject(JSONTokener(it)) }