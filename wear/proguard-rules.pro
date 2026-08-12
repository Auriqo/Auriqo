# Wear tile service and proto classes must be kept
-keep class com.auriqa.music.wear.tile.** { *; }
-keep class com.auriqa.music.wear.media.** { *; }

# Tiles use protobuf-generated code
-keep class androidx.wear.tiles.** { *; }
-keep class com.google.protobuf.** { *; }
