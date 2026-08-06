

package com.auriqo.music.listentogether

import com.auriqo.music.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    private val configuration = ListenTogetherConfiguration(
        configuredServerUrl = BuildConfig.LISTEN_TOGETHER_SERVER_URL,
        configuredShareBaseUrl = BuildConfig.LISTEN_TOGETHER_SHARE_BASE_URL,
    )

    private val _servers = MutableStateFlow(
        configuration.serverUrl?.let { serverUrl ->
            listOf(
                ListenTogetherServer(
                    name = "Configured server",
                    url = serverUrl,
                    location = "Configured at build time",
                    operator = "",
                )
            )
        }.orEmpty()
    )
    
    val serversFlow: StateFlow<List<ListenTogetherServer>> = _servers

    val servers: List<ListenTogetherServer>
        get() = _servers.value

    val defaultServerUrl: String
        get() = configuration.serverUrl.orEmpty()

    fun resolveServerUrl(storedServerUrl: String?): String? =
        configuration.resolveServerUrl(storedServerUrl)

    fun isAvailable(storedServerUrl: String?): Boolean =
        resolveServerUrl(storedServerUrl) != null

    fun inviteLink(roomCode: String): String? = configuration.inviteLink(roomCode)

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}
