package com.non_breath.finlitrush.game.data;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight container for map data loaded from assets (custom JSON or Tiled JSON).
 */
public class MapData {
    public int cols;
    public int rows;

    // Draw layers (tile indices)
    public List<int[][]> tileLayers = new ArrayList<>();

    // Collision mask (true = blocked)
    public boolean[][] collision;

    // Optional player start
    public float playerCol = -1;
    public float playerRow = -1;

    // NPCs parsed from map (optional)
    public static class Npc {
        public int col, row;
        public String name;
        public String[] lines;
    }
    public final List<Npc> npcs = new ArrayList<>();

    // Warps (doors/portals)
    public static class Warp { public int col, row; public String target; public int targetCol, targetRow; }
    public final List<Warp> warps = new ArrayList<>();

    // Image layers (Tiled image layers or custom backgrounds)
    public static class ImageLayer {
        public Bitmap bitmap;
        public int offsetX;
        public int offsetY;
    }
    public final List<ImageLayer> imageLayers = new ArrayList<>();

    // Tileset metadata
    public String[] tileDrawableNames = null; // android-drawables tileset (index -> name)
    public int[] tilePaletteColors = null;    // palette tileset (index -> color)

    // Atlas (Tiled) metadata
    public Bitmap atlasBitmap = null;         // decoded atlas (optional)
    public int atlasTileW = 0, atlasTileH = 0, atlasColumns = 0, atlasFirstGid = 1;
}
