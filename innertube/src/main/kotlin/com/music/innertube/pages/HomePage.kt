package com.music.innertube.pages

import com.music.innertube.models.Album
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.Artist
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.MusicCardShelfRenderer
import com.music.innertube.models.MusicCarouselShelfRenderer
import com.music.innertube.models.MusicResponsiveListItemRenderer
import com.music.innertube.models.MusicShelfRenderer
import com.music.innertube.models.MusicTwoRowItemRenderer
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SectionListRenderer
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.models.getItems
import com.music.innertube.models.oddElements
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.utils.parseTime

data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title = renderer.chipCloudChipRenderer.text?.runs?.firstOrNull()?.text ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                )
            }
        }
    }

    data class Section(
        val title: String,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    ) {
        companion object {
            fun fromContent(content: SectionListRenderer.Content): Section? {
                content.musicCarouselShelfRenderer?.let { return fromMusicCarouselShelfRenderer(it) }
                content.musicShelfRenderer?.let { return fromMusicShelfRenderer(it) }
                content.musicCardShelfRenderer?.let { return fromMusicCardShelfRenderer(it) }
                return null
            }

            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section? {
                return Section(
                    title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return null,
                    label = renderer.header.musicCarouselShelfBasicHeaderRenderer.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header.musicCarouselShelfBasicHeaderRenderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.header.musicCarouselShelfBasicHeaderRenderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = renderer.contents.mapNotNull {
                        it.musicTwoRowItemRenderer?.let(::fromMusicTwoRowItemRenderer)
                            ?: it.musicResponsiveListItemRenderer?.let(::fromMusicResponsiveListItemRenderer)
                    }.ifEmpty {
                        return null
                    }
                )
            }

            fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): Section? {
                return Section(
                    title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
                    label = null,
                    thumbnail = null,
                    endpoint = renderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint
                        ?: renderer.bottomEndpoint?.browseEndpoint,
                    items = renderer.contents?.getItems()
                        ?.mapNotNull(::fromMusicResponsiveListItemRenderer)
                        .orEmpty().ifEmpty {
                            return null
                        }
                )
            }

            fun fromMusicCardShelfRenderer(renderer: MusicCardShelfRenderer): Section? {
                return Section(
                    title = renderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.firstOrNull()?.text
                        ?: renderer.title.runs?.firstOrNull()?.text ?: return null,
                    label = renderer.subtitle.runs?.firstOrNull()?.text,
                    thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.onTap.browseEndpoint,
                    items = renderer.contents
                        ?.mapNotNull { it.musicResponsiveListItemRenderer }
                        ?.mapNotNull(::fromMusicResponsiveListItemRenderer)
                        .orEmpty().ifEmpty {
                            return null
                        }
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs?.oddElements() ?: return null
                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = subtitleRuns.filter { run ->
                                run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true ||
                                (run.navigationEndpoint?.browseEndpoint != null && 
                                 run.navigationEndpoint.browseEndpoint.browseId.startsWith("MPREb_") != true)
                            }.map { run ->
                                Artist(
                                    name = run.text,
                                    id = run.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            }.ifEmpty {
                                subtitleRuns.firstOrNull()?.let { run -> 
                                    listOf(Artist(name = run.text, id = null)) 
                                } ?: emptyList()
                            },
                            album = subtitleRuns.firstOrNull { 
                                it.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true 
                            }?.let {
                                Album(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                                )
                            },
                            duration = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }
                    renderer.isAlbum -> {
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            },
                            year = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.firstOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                            radioEndpoint = renderer.menu.menuRenderer.items.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        )
                    }

                    else -> null
                }
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
                val videoId = renderer.playlistItemData?.videoId
                    ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()
                        ?.navigationEndpoint?.watchEndpoint?.videoId
                    ?: return null

                val title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text
                    ?: renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()?.text
                    ?: return null

                val artists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }

                val album = renderer.flexColumns.getOrNull(2)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                        )
                    }

                return SongItem(
                    id = videoId,
                    title = title,
                    artists = artists,
                    album = album,
                    duration = renderer.fixedColumns?.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()
                        ?.text?.parseTime(),
                    musicVideoType = renderer.musicVideoType,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                        ?: return null,
                    explicit = renderer.badges?.any {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } == true,
                    libraryAddToken = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items).addToken,
                    libraryRemoveToken = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items).removeToken
                )
            }
        }
    }

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterExplicit())
            })
        } else this

    fun filterVideoSongs(disableVideos: Boolean = false) =
        if (disableVideos) {
            copy(sections = sections.map { section ->
                section.copy(items = section.items.filterVideoSongs(true))
            })
        } else this
}
