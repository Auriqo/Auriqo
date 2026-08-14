

package com.auriqo.music.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auriqo.music.constants.AppleMusicLyricsBlurKey
import com.auriqo.music.lyrics.LyricsEntry
import com.auriqo.music.ui.screens.settings.LyricsPosition
import com.auriqo.music.utils.rememberPreference


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun echomusicLyricsLine(
    entry: LyricsEntry,
    nextEntryTime: Long?,
    effectivePlaybackPosition: Long,
    isSynced: Boolean,
    isActive: Boolean,
    distanceFromCurrent: Int,
    lyricsTextPosition: LyricsPosition,
    textColor: Color,
    showRomanized: Boolean,
    showTranslated: Boolean,
    textSize: Float,
    lineSpacing: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    isAutoScrollActive: Boolean,
    expressiveAccent: Color,
    skin: LyricsSkinSpec,
    modifier: Modifier = Modifier
) {
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)

    val targetBlur = if (!appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive) {
        0f
    } else {
        skin.distantLineBlur(distanceFromCurrent)
    }

    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 1000), label = "blur"
    )

    val effectiveTextSize = textSize * skin.fontSizeMultiplier

    val duration = remember(entry.time, nextEntryTime) {
        if (nextEntryTime != null) nextEntryTime - entry.time else 4000L
    }

    
    val activeDuration = remember(duration) {
        (duration * 0.95).toLong().coerceAtLeast(300L)
    }

    
    val wordData = remember(entry.text, entry.words, activeDuration) {
        val isHindiText = com.auriqo.music.lyrics.LyricsUtils.isHindi(entry.text)
        if (!isHindiText && entry.words != null && entry.words.isNotEmpty()) {
            
            entry.words.mapIndexed { index, word ->
                val wordStart = ((word.startTime * 1000).toLong() - entry.time).coerceAtLeast(0L)
                val wordEnd = ((word.endTime * 1000).toLong() - entry.time).coerceAtLeast(wordStart + 50L)
                Triple(word.text, wordStart, wordEnd)
            }
        } else {
            
            val words = entry.text.split(" ").filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                listOf(Triple(entry.text, 0L, activeDuration))
            } else {
                val totalChars = entry.text.length
                var accumulatedTime = 0L
                words.mapIndexed { index, word ->
                    val wordLength = word.length
                    val includeSpace = index < words.size - 1
                    val charCount = if (includeSpace) wordLength + 1 else wordLength
                    val wordStart = accumulatedTime
                    val wordDur = if (totalChars > 0) (activeDuration * charCount.toFloat() / totalChars).toLong() else activeDuration
                    val wordEnd = wordStart + wordDur
                    accumulatedTime += wordDur
                    Triple(word, wordStart, wordEnd)
                }
            }
        }
    }

    val targetAlpha = when {
        !isSynced || (isSelectionModeActive && isSelected) -> 1f
        isActive -> 1f
        distanceFromCurrent == 1 -> 0.65f 
        distanceFromCurrent == 2 -> 0.45f 
        else -> 0.35f 
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "lineAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive && skin.activeAnimation == LyricsSkinSpec.ActiveAnimation.SCALE) 1.05f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "lineScale"
    )

    val jumpTransition = rememberInfiniteTransition(label = "lineJump")
    val jumpPosition by jumpTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "jumpPos",
    )
    val morphColor = if (skin.activeLineColors.size >= 2) {
        val segment = jumpPosition * (skin.activeLineColors.size - 1)
        val index = segment.toInt().coerceAtMost(skin.activeLineColors.size - 2)
        val frac = segment - index
        androidx.compose.ui.graphics.lerp(
            skin.activeLineColors[index],
            skin.activeLineColors[index + 1],
            frac,
        )
    } else {
        textColor
    }
    val jumpOffset = if (isActive && skin.activeAnimation == LyricsSkinSpec.ActiveAnimation.JUMP) {
        (18f * kotlin.math.sin(jumpPosition * kotlin.math.PI.toFloat())).dp
    } else 0.dp

    val itemModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            this.alpha = animatedAlpha
            this.scaleX = scale
            this.scaleY = scale
            this.translationY = -jumpOffset.toPx()
        }
        .clip(RoundedCornerShape(16.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = (8 * lineSpacing).dp)
        .blur(animatedBlur.dp)

    
    val agentAlignment = when {
        entry.isBackground -> Alignment.CenterHorizontally
        entry.agent == "v1" -> Alignment.Start
        entry.agent == "v2" -> Alignment.End
        entry.agent == "v1000" -> Alignment.CenterHorizontally
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    }

    val agentTextAlign = when {
        entry.isBackground -> TextAlign.Center
        entry.agent == "v1" -> TextAlign.Left
        entry.agent == "v2" -> TextAlign.Right
        entry.agent == "v1000" -> TextAlign.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }
    }

    Column(
        modifier = itemModifier,
        horizontalAlignment = agentAlignment
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (agentTextAlign) {
                TextAlign.Center -> Arrangement.Center
                TextAlign.Right -> Arrangement.End
                else -> Arrangement.Start
            },
            verticalArrangement = Arrangement.spacedBy(
                
                with(LocalDensity.current) { (textSize * (lineSpacing.coerceAtMost(1.3f) - 1f)).sp.toDp() }
            )
        ) {
            wordData.forEachIndexed { index, (wordText, startRelative, endRelative) ->
                val lineRelTime = (effectivePlaybackPosition - entry.time).coerceAtLeast(0L)
                val wordDuration = (endRelative - startRelative).coerceAtLeast(1L)
                
                val progress by animateFloatAsState(
                    targetValue = when {
                        lineRelTime >= endRelative -> 1f
                        lineRelTime < startRelative -> 0f
                        else -> (lineRelTime - startRelative).toFloat() / wordDuration
                    },
                    animationSpec = tween(durationMillis = 150, easing = androidx.compose.animation.core.LinearEasing),
                    label = "wordProgress"
                )

                val finalFontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                val isSkinColored = skin.activeLineColors.isNotEmpty()
                val baseColor = if (isActive && isSkinColored && skin.activeAnimation == LyricsSkinSpec.ActiveAnimation.JUMP) {
                    morphColor
                } else {
                    textColor
                }
                val dimColor = baseColor.copy(alpha = skin.inactiveDimAlpha)

                val wordBrush = if (isSkinColored) {
                    val sungStops = if (progress > 0.08f) {
                        skin.activeLineColors.mapIndexed { i, color ->
                            val stop = (progress * i.toFloat() / (skin.activeLineColors.size - 1).coerceAtLeast(1)).coerceIn(0f, progress)
                            stop to color
                        }
                    } else {
                        emptyList()
                    }
                    Brush.horizontalGradient(
                        *(sungStops +
                            ((progress + 0.05f).coerceAtMost(1f) to dimColor) +
                            (1f to dimColor)).toTypedArray(),
                    )
                } else {
                    Brush.horizontalGradient(
                        0.0f to baseColor,
                        (progress - 0.05f).coerceAtLeast(0f) to baseColor,
                        (progress + 0.05f).coerceAtMost(1f) to dimColor,
                        1.0f to dimColor
                    )
                }

                
                
                Text(
                    text = wordText,
                    fontSize = effectiveTextSize.sp,
                    style = TextStyle(
                        brush = wordBrush,
                        fontWeight = finalFontWeight,

                        
                        lineHeight = (effectiveTextSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        textAlign = agentTextAlign,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = skin.glowColor.copy(alpha = 0.6f * progress),
                            offset = Offset.Zero,
                            blurRadius = (12f * progress * skin.glowStrength).coerceAtLeast(0.1f)
                        )
                    )
                )
                if (index != wordData.lastIndex) {
                    Text(
                        text = " ",
                        fontSize = effectiveTextSize.sp,
                        color = baseColor.copy(alpha = if (lineRelTime >= endRelative) 1f else skin.inactiveDimAlpha), 
                        lineHeight = (effectiveTextSize * lineSpacing.coerceAtMost(1.3f)).sp,
                        style = TextStyle(
                            shadow = if (lineRelTime >= endRelative) {
                                androidx.compose.ui.graphics.Shadow(
                                    color = skin.glowColor.copy(alpha = 0.3f),
                                    offset = Offset.Zero,
                                    blurRadius = 6f * skin.glowStrength
                                )
                            } else null
                        )
                    )
                }
            }
        }

        
        if (showRomanized) {
            val romanizedText by entry.romanizedTextFlow.collectAsState()
            romanizedText?.let { romanized ->
                Text(
                    text = romanized,
                    fontSize = (effectiveTextSize * 0.65f).sp,
                    color = textColor.copy(alpha = 0.6f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.SemiBold,

                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                    lineHeight = (effectiveTextSize * 0.65f * lineSpacing.coerceAtMost(1.3f)).sp
                )
            }
        }

        
        if (showTranslated) {
            val translatedText by entry.translatedTextFlow.collectAsState()
            translatedText?.let { translated ->
                Text(
                    text = translated,
                    fontSize = (effectiveTextSize * 0.7f).sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = agentTextAlign,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                    lineHeight = (effectiveTextSize * 0.7f * lineSpacing.coerceAtMost(1.3f)).sp
                )
            }
        }
    }
}
