package com.example.plugins.planner.data

object ImageEventParser {

    private const val SEPARATOR = ":::"

    fun encode(uri: String, description: String?): String =
        if (description != null && description.isNotBlank()) {
            "$uri$SEPARATOR$description"
        } else {
            uri
        }

    fun decode(payload: String?): ImageEventData {
        if (payload == null || payload.isBlank()) {
            return ImageEventData(uri = "", description = null)
        }
        val index = payload.indexOf(SEPARATOR)
        if (index < 0) {
            return ImageEventData(uri = payload, description = null)
        }
        val uri = payload.substring(0, index)
        val desc = payload.substring(index + SEPARATOR.length)
        return ImageEventData(uri = uri, description = if (desc.isBlank()) null else desc)
    }
}