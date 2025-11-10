package com.non_breath.finlitrush.game.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.non_breath.finlitrush.game.data.MapData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Loads map JSON from assets. Supports a simple custom JSON and Tiled JSON.
 */
public final class MapLoader {
    private MapLoader() {}

    public static MapData load(Context ctx, String assetPath) throws Exception {
        try (InputStream is = ctx.getAssets().open(assetPath)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String json = sb.toString();
            JSONObject root = new JSONObject(json);
            if (root.has("layers") && root.has("tilesets")) {
                return fromTiled(ctx, root);
            }
            return fromCustom(ctx, root);
        }
    }

    private static MapData fromCustom(Context ctx, JSONObject root) throws Exception {
        MapData md = new MapData();
        md.cols = root.getInt("cols");
        md.rows = root.getInt("rows");
        // tiles (single layer)
        JSONArray tiles = root.getJSONArray("tiles");
        int[][] layer0 = new int[md.rows][md.cols];
        for (int r = 0; r < md.rows; r++) {
            JSONArray rowArr = tiles.getJSONArray(r);
            for (int c = 0; c < md.cols; c++) layer0[r][c] = rowArr.getInt(c);
        }
        md.tileLayers.add(layer0);
        // default collision from non-zero tiles or explicit collision
        md.collision = new boolean[md.rows][md.cols];
        for (int r = 0; r < md.rows; r++) for (int c = 0; c < md.cols; c++) md.collision[r][c] = layer0[r][c] != 0;
        if (root.has("collision")) {
            JSONArray coll = root.getJSONArray("collision");
            for (int r = 0; r < md.rows; r++) {
                JSONArray rowArr = coll.getJSONArray(r);
                for (int c = 0; c < md.cols; c++) md.collision[r][c] = rowArr.getInt(c) != 0;
            }
        }
        // player
        if (root.has("player")) {
            JSONObject p = root.getJSONObject("player");
            md.playerCol = (float) p.optDouble("col", -1);
            md.playerRow = (float) p.optDouble("row", -1);
        }
        // npcs
        if (root.has("npcs")) {
            JSONArray arr = root.getJSONArray("npcs");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject n = arr.getJSONObject(i);
                MapData.Npc npc = new MapData.Npc();
                npc.col = n.getInt("col"); npc.row = n.getInt("row");
                npc.name = n.optString("name", "NPC");
                JSONArray linesArr = n.optJSONArray("lines");
                if (linesArr != null) {
                    npc.lines = new String[linesArr.length()];
                    for (int j = 0; j < linesArr.length(); j++) npc.lines[j] = linesArr.getString(j);
                } else npc.lines = new String[]{"..."};
                md.npcs.add(npc);
            }
        }
        // optional custom image layers (array of { image, offsetX, offsetY })
        if (root.has("imageLayers")) {
            JSONArray imgLayers = root.getJSONArray("imageLayers");
            for (int i = 0; i < imgLayers.length(); i++) {
                JSONObject obj = imgLayers.getJSONObject(i);
                String imagePath = obj.getString("image");
                MapData.ImageLayer il = new MapData.ImageLayer();
                il.offsetX = obj.optInt("offsetX", 0);
                il.offsetY = obj.optInt("offsetY", 0);
                il.bitmap = decodeBitmap(ctx, imagePath, Bitmap.Config.ARGB_8888);
                if (il.bitmap != null) md.imageLayers.add(il);
            }
        }

        // tileset metadata
        if (root.has("tileset")) {
            JSONObject ts = root.getJSONObject("tileset");
            String type = ts.optString("type", "");
            if ("android-drawables".equals(type) && ts.has("drawables")) {
                JSONArray arr = ts.getJSONArray("drawables");
                md.tileDrawableNames = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) md.tileDrawableNames[i] = arr.getString(i);
            } else if (ts.has("palette")) {
                JSONArray pal = ts.getJSONArray("palette");
                md.tilePaletteColors = new int[pal.length()];
                for (int i = 0; i < pal.length(); i++) md.tilePaletteColors[i] = android.graphics.Color.parseColor(pal.getString(i));
            } else if (ts.has("atlas")) {
                JSONObject a = ts.getJSONObject("atlas");
                String imagePath = a.getString("image");
                md.atlasTileW = a.getInt("tileW");
                md.atlasTileH = a.getInt("tileH");
                md.atlasColumns = a.getInt("columns");
                md.atlasFirstGid = a.optInt("firstgid", 1);
                md.atlasBitmap = decodeBitmap(ctx, imagePath, Bitmap.Config.ARGB_8888);
            }
        }
        parseWarps(root, md);
        return md;
    }

    private static MapData fromTiled(Context ctx, JSONObject root) throws Exception {
        MapData md = new MapData();
        md.cols = root.getInt("width");
        md.rows = root.getInt("height");
        int tw = root.getInt("tilewidth");
        int th = root.getInt("tileheight");
        // layers
        JSONArray layers = root.getJSONArray("layers");
        boolean[][] coll = new boolean[md.rows][md.cols];
        for (int i = 0; i < layers.length(); i++) {
            JSONObject layer = layers.getJSONObject(i);
            String type = layer.optString("type");
            if ("tilelayer".equals(type)) {
                JSONArray data = layer.getJSONArray("data");
                int[][] grid = new int[md.rows][md.cols];
                for (int r = 0; r < md.rows; r++) for (int c = 0; c < md.cols; c++) grid[r][c] = data.getInt(r * md.cols + c);
                String name = layer.optString("name", "");
                boolean isCollision = name.equalsIgnoreCase("collision") || name.equalsIgnoreCase("collide");
                if (isCollision) {
                    for (int r = 0; r < md.rows; r++) for (int c = 0; c < md.cols; c++) coll[r][c] = coll[r][c] || grid[r][c] != 0;
                } else {
                    md.tileLayers.add(grid);
                }
            } else if ("imagelayer".equals(type) && layer.has("image")) {
                MapData.ImageLayer il = new MapData.ImageLayer();
                il.bitmap = decodeBitmap(ctx, layer.getString("image"), Bitmap.Config.ARGB_8888);
                il.offsetX = (int) Math.round(layer.optDouble("offsetx", 0));
                il.offsetY = (int) Math.round(layer.optDouble("offsety", 0));
                if (il.bitmap != null) md.imageLayers.add(il);
            }
        }
        md.collision = coll;

        // tileset
        JSONArray tilesets = root.getJSONArray("tilesets");
        JSONObject ts0 = tilesets.getJSONObject(0);
        md.atlasFirstGid = ts0.getInt("firstgid");
        md.atlasColumns = ts0.optInt("columns", 0);
        md.atlasTileW = ts0.optInt("tilewidth", tw);
        md.atlasTileH = ts0.optInt("tileheight", th);
        md.tileDrawableNames = null; md.tilePaletteColors = null; md.atlasBitmap = null;
        if (ts0.has("imageBase64")) {
            String b64 = ts0.getString("imageBase64");
            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            md.atlasBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else if (ts0.has("image")) {
            md.atlasBitmap = decodeBitmap(ctx, ts0.getString("image"), Bitmap.Config.ARGB_8888);
        } else if (ts0.has("colors")) {
            JSONArray pal = ts0.getJSONArray("colors");
            md.tilePaletteColors = new int[pal.length()];
            for (int i = 0; i < pal.length(); i++) md.tilePaletteColors[i] = android.graphics.Color.parseColor(pal.getString(i));
        }
        parseWarps(root, md);
        return md;
    }

    private static void parseWarps(JSONObject root, MapData md) throws Exception {
        if (!root.has("warps")) return;
        JSONArray arrW = root.getJSONArray("warps");
        for (int i = 0; i < arrW.length(); i++) {
            JSONObject w = arrW.getJSONObject(i);
            MapData.Warp wp = new MapData.Warp();
            wp.col = w.getInt("col"); wp.row = w.getInt("row");
            wp.target = w.optString("target", null);
            wp.targetCol = w.optInt("targetCol", wp.col);
            wp.targetRow = w.optInt("targetRow", wp.row);
            md.warps.add(wp);
        }
    }

    private static Bitmap decodeBitmap(Context ctx, String assetPath, Bitmap.Config config) throws Exception {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = config;
        try (InputStream ais = ctx.getAssets().open(assetPath)) {
            return BitmapFactory.decodeStream(ais, null, opts);
        }
    }
}
