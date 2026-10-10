package com.pcbrowser.chrome.search

import java.net.URLEncoder

object SearchEngineManager {

    val ENGINES = listOf(
        SearchEngineModel(
            id = "google",
            name = "Google",
            queryUrl = "https://www.google.com/search?q=%s",
            homepageUrl = "https://www.google.com"
        ),
        SearchEngineModel(
            id = "bing",
            name = "Microsoft Bing",
            queryUrl = "https://www.bing.com/search?q=%s",
            homepageUrl = "https://www.bing.com"
        ),
        SearchEngineModel(
            id = "duckduckgo",
            name = "DuckDuckGo",
            queryUrl = "https://duckduckgo.com/?q=%s",
            homepageUrl = "https://duckduckgo.com"
        ),
        SearchEngineModel(
            id = "brave",
            name = "Brave Search",
            queryUrl = "https://search.brave.com/search?q=%s",
            homepageUrl = "https://search.brave.com"
        ),
        SearchEngineModel(
            id = "yahoo",
            name = "Yahoo!",
            queryUrl = "https://search.yahoo.com/search?p=%s",
            homepageUrl = "https://www.yahoo.com"
        ),
        SearchEngineModel(
            id = "ecosia",
            name = "Ecosia",
            queryUrl = "https://www.ecosia.org/search?q=%s",
            homepageUrl = "https://www.ecosia.org"
        ),
        SearchEngineModel(
            id = "yandex",
            name = "Yandex",
            queryUrl = "https://yandex.com/search/?text=%s",
            homepageUrl = "https://yandex.com"
        )
    )

    fun getEngine(id: String): SearchEngineModel {
        return ENGINES.find { it.id.equals(id, ignoreCase = true) } ?: ENGINES[0]
    }

    fun buildSearchUrl(engineId: String, query: String): String {
        val engine = getEngine(engineId)
        return try {
            engine.queryUrl.replace("%s", URLEncoder.encode(query, "UTF-8"))
        } catch (_: Exception) {
            engine.queryUrl.replace("%s", query)
        }
    }
}
