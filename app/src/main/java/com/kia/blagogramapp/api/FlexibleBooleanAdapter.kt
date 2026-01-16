package com.kia.blagogramapp.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FlexibleBooleanAdapter : JsonDeserializer<Boolean> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Boolean {
        return when {
            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                if (primitive.isBoolean) primitive.asBoolean
                else if (primitive.isNumber) primitive.asInt != 0
                else if (primitive.isString) {
                    val s = primitive.asString
                    s == "1" || s.equals("true", ignoreCase = true)
                } else false
            }
            else -> false
        }
    }
}
