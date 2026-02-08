package com.antechrist.adherentsapp.core.pdf

import android.content.Context

object TextTemplateRenderer {

    fun renderAssetTemplate(
        context: Context,
        assetPath: String,
        values: Map<String, String>
    ): String {
        val raw = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        var out = raw
        values.forEach { (k, v) ->
            out = out.replace("{{${k}}}", v)
        }
        // placeholders restants -> vide
        out = out.replace(Regex("\\{\\{[^}]+\\}\\}"), "")
        return out
    }
}
