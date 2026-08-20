package com.music.innertube.pages

import com.music.innertube.models.Album
import com.music.innertube.models.Artist
import com.music.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.music.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.music.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.music.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_USER_CHANNEL
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.Run
import com.music.innertube.models.SongItem
import com.music.innertube.models.extractViewCountText
import com.music.innertube.models.oddElements
import com.music.innertube.models.splitBySeparator
import com.music.innertube.utils.parseTime
import com.music.innertube.models.YTItem

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
    val related: List<YTItem>? = null,
    val isCollaborative: Boolean = false,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(
            renderer: MusicResponsiveListItemRenderer,
            isCollaborative: Boolean = false,
        ): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            // Collaborative playlists insert the contributor between the title and artists.
            // Its run points to MUSIC_PAGE_TYPE_USER_CHANNEL; regular playlists do not have
            // this extra column. Keep the old positional fallback for older responses.
            val detectedContributorColumnIndex = renderer.flexColumns.indexOfFirst { column ->
                columnRuns(column).any { it.musicPageType() == MUSIC_PAGE_TYPE_USER_CHANNEL }
            }.takeIf { it >= 0 }
            val contributorColumnIndex = detectedContributorColumnIndex
                ?: if (isCollaborative) 1 else null
            val hasContributorColumn = contributorColumnIndex != null
            val contributorRun = contributorColumnIndex?.let { index ->
                columnRuns(renderer.flexColumns.getOrNull(index)).firstOrNull { it.musicPageType() == MUSIC_PAGE_TYPE_USER_CHANNEL }
                    ?: columnRuns(renderer.flexColumns.getOrNull(index)).firstOrNull { it.text.isNotBlank() }
            }
            val contributor = contributorRun?.let {
                Artist(
                    name = it.text.removePrefix("by ").trim(),
                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                )
            }

            val artistColumnIndex = renderer.flexColumns.indices.firstOrNull { index ->
                index > 0 && index != contributorColumnIndex && columnRuns(renderer.flexColumns[index]).any {
                    it.musicPageType() == MUSIC_PAGE_TYPE_ARTIST || it.musicPageType() == "MUSIC_PAGE_TYPE_UNKNOWN"
                }
            } ?: if (hasContributorColumn) 2 else 1
            val secondaryLineRuns = columnRuns(renderer.flexColumns.getOrNull(artistColumnIndex))
                .splitBySeparator()
            val albumColumnIndex = renderer.flexColumns.indices.firstOrNull { index ->
                index > artistColumnIndex && columnRuns(renderer.flexColumns[index]).any {
                    it.musicPageType() == MUSIC_PAGE_TYPE_ALBUM || it.musicPageType() == MUSIC_PAGE_TYPE_AUDIOBOOK
                }
            } ?: if (hasContributorColumn) 3 else 2

            return SongItem(
                id = renderer.playlistItemData?.videoId ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint?.watchEndpoint?.videoId
                ?: renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.videoId
                ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text
                    ?.runs?.firstOrNull()?.text ?: return null,
                artists = secondaryLineRuns.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                }.orEmpty(),
                album = renderer.flexColumns.getOrNull(albumColumnIndex)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                setVideoId = renderer.playlistItemData?.playlistSetVideoId ?: renderer.navigationEndpoint?.watchEndpoint?.playlistSetVideoId
                ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                    ?.content?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint?.watchEndpoint?.playlistSetVideoId
                ?: renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()
                    ?.navigationEndpoint?.watchEndpoint?.playlistSetVideoId,
                viewCountText = renderer.flexColumns.getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                    ?.extractViewCountText(),
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken,
                playlistContributor = contributor,
            )
        }

        private fun columnRuns(column: MusicResponsiveListItemRenderer.FlexColumn?): List<Run> =
            column?.musicResponsiveListItemFlexColumnRenderer?.text?.runs.orEmpty()

        private fun Run.musicPageType(): String? =
            navigationEndpoint?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType
    }
}
