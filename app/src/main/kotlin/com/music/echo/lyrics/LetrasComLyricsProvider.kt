package iad1tya.echo.music.lyrics

import android.content.Context
import iad1tya.echo.music.constants.EnableLetrasComKey
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import com.music.echo.letras.LetrasCom

object LetrasComLyricsProvider : LyricsProvider {
    override val name = "LetrasCom"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableLetrasComKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = LetrasCom.getLyrics(title, artist)
}
