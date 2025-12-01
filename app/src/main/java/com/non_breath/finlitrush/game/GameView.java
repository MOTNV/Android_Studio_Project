package com.non_breath.finlitrush.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.non_breath.finlitrush.R;
import com.non_breath.finlitrush.game.data.MapData;
import com.non_breath.finlitrush.game.io.MapLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal 2D tile-based demo view.
 * - Player moves with on-screen D-pad
 * - Talk to NPC using "A" button when adjacent
 * - Simple placeholder tiles/entities, no external assets
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Thread gameThread;
    private volatile boolean running = false;

    // World / map
    private int cols = 20;
    private int rows = 12;
    private int tileSize = 48; // computed after surface created
    private int mapOffsetX = 0; // drawing offset X (camera)
    private int mapOffsetY = 0; // drawing offset Y (camera)
    private float cameraX = 0;  // camera top-left in pixels
    private float cameraY = 0;  // camera top-left in pixels

    private int[][] map; // 0: floor, 1: wall (legacy single layer)
    private java.util.List<int[][]> tileLayers = new java.util.ArrayList<>();
    private boolean[][] collisionMask = null; // true when blocked
    // Warps (door/portal)
    private static class Warp { int col, row; String target; int targetCol, targetRow; }
    private final java.util.List<Warp> warps = new java.util.ArrayList<>();
    private float warpCooldown = 0f;

    // Player
    private float playerX = 2; // tile coords
    private float playerY = 2;
    private float playerRadiusPx = 0; // computed from tileSize
    private float moveSpeedTilesPerSec = 5.0f;
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    // NPCs
    private static class Npc {
        int col, row;
        String name;
        String[] lines;
        Npc(int col, int row, String name, String[] lines) {
            this.col = col; this.row = row; this.name = name; this.lines = lines;
        }
    }
    private final List<Npc> npcs = new ArrayList<>();

    // UI buttons (virtual controls)
    private Rect btnUp = new Rect();
    private Rect btnDown = new Rect();
    private Rect btnLeft = new Rect();
    private Rect btnRight = new Rect();
    private Rect btnA = new Rect(); // talk

    // Virtual joystick (optional control scheme)
    private boolean useJoystick = true;
    private PointF joyCenter = new PointF();
    private float joyBaseRadius = 0f;
    private float joyKnobRadius = 0f;
    private int joyPointerId = -1;
    private float joyVecX = 0f; // -1..1
    private float joyVecY = 0f; // -1..1 (down positive)

    // Dialog state (line-by-line)
    private boolean dialogOpen = false;
    private String dialogSpeaker = "";
    private Npc dialogNpc = null;
    private String[] dialogLines = null;
    private int dialogIndex = 0;

    // Paints
    private final Paint paintTile = new Paint();
    private final Paint paintWall = new Paint();
    private final Paint paintPlayer = new Paint();
    private final Paint paintNpc = new Paint();
    private final Paint paintUi = new Paint();
    private final Paint paintUiStroke = new Paint();
    private final Paint paintText = new Paint();
    private final Paint paintDialogBg = new Paint();

    private long lastFrameNanos = 0;
    private String lastError = null; // for on-screen debug overlay
    private boolean debugHud = false; private int debugTapCount = 0; private long debugLastTapMs = 0;

    // Bitmaps (generated from drawable resources at runtime)
    private Bitmap bmpFloor, bmpWall, bmpNpc;
    private Bitmap bmpPlayerIdle;
    private Bitmap[] bmpPlayerWalk = new Bitmap[2];
    // Directional frames
    private static final int DIR_DOWN = 0, DIR_LEFT = 1, DIR_RIGHT = 2, DIR_UP = 3;
    private int playerDir = DIR_DOWN;
    private Bitmap[][] playerWalkDir = new Bitmap[4][2];
    private Bitmap[] playerIdleDir = new Bitmap[4];
    // NPC directional frames (shared for all NPCs)
    private Bitmap[][] npcWalkDir = new Bitmap[4][2];
    private Bitmap[] npcIdleDir = new Bitmap[4];
    private float animTime = 0f;
    private boolean moving = false;

    // Tileset support
    private Bitmap[] tileBitmaps = null;           // per-index tiles from drawables/palette
    private String[] tileDrawableNames = null;     // to rebuild on resize
    private int[] tilePaletteColors = null;        // to rebuild on resize
    // Atlas (Tiled JSON) support
    private Bitmap atlasBitmap = null;
    private int atlasTileW = 0, atlasTileH = 0, atlasColumns = 0, atlasFirstGid = 1;
    private final java.util.List<MapData.ImageLayer> imageLayers = new java.util.ArrayList<>();

    private static final String PRIMARY_MAP_ASSET = "maps/tiled_map.json";
    private static final String FALLBACK_MAP_ASSET = "maps/demo_map.json";
    private String currentMapAsset = PRIMARY_MAP_ASSET;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        // Colors
        paintTile.setColor(Color.rgb(210, 210, 210));
        paintWall.setColor(Color.rgb(120, 120, 120));
        paintPlayer.setColor(Color.rgb(66, 165, 245)); // blue
        paintNpc.setColor(Color.rgb(244, 67, 54)); // red

        paintUi.setColor(Color.argb(90, 0, 0, 0));
        paintUiStroke.setStyle(Paint.Style.STROKE);
        paintUiStroke.setStrokeWidth(4);
        paintUiStroke.setColor(Color.WHITE);

        paintText.setColor(Color.BLACK);
        paintText.setTextSize(36f);

        paintDialogBg.setColor(Color.argb(220, 255, 255, 255));

        // Try Tiled map first; fallback to custom demo
        if (!loadMapFromAssetsSafe(PRIMARY_MAP_ASSET) && !loadMapFromAssetsSafe(FALLBACK_MAP_ASSET)) {
            buildDemoMap();
        }
        // Add few NPCs
                npcs.add(new Npc(10, 6, "NPC", new String[]{
                "\uC548\uB155! \uB370\uBAA8 NPC\uC57C.",
                "A \uBC84\uD2BC\uC744 \uB204\uB974\uBA74 \uB300\uD654\uAC00 \uC9C4\uD589\uB429\uB2C8\uB2E4.",
                "\uBCBD\uC740 \uD68C\uC0C9, \uBC14\uB2E5\uC740 \uBC1D\uC740 \uD68C\uC0C9\uC774\uC57C.",
        }));
                npcs.add(new Npc(5, 4, "\uC0C1\uC778", new String[]{
                "\uC5EC\uAE30\uB294 \uC2DC\uD5D8\uC6A9 \uB9F5\uC774\uC57C.",
                "\uB9F5\uACFC \uC5D0\uC14B\uC740 \uC774\uD6C4 \uAD50\uCCB4 \uAC00\uB2A5!",
        }));
    }

    private void buildDemoMap() {
        map = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // border walls
                if (r == 0 || c == 0 || r == rows - 1 || c == cols - 1) {
                    map[r][c] = 1;
                } else {
                    map[r][c] = 0;
                }
            }
        }
        // inner blocks
        for (int c = 3; c < 8; c++) map[3][c] = 1;
        for (int r = 5; r < 9; r++) map[r][12] = 1;
        // layers and collision defaults
        tileLayers.clear();
        tileLayers.add(map);
        collisionMask = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) collisionMask[r][c] = map[r][c] != 0;
        }
        imageLayers.clear();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Choose tile size relative to screen (target ~12x8 visible tiles)
        int w = getWidth();
        int h = getHeight();
        tileSize = Math.max(24, Math.min(w / 12, h / 8));
        int mapW = cols * tileSize;
        int mapH = rows * tileSize;
        // initialize camera centered on player
        centerCameraOnPlayer(w, h, mapW, mapH);
        playerRadiusPx = tileSize * 0.35f;

        // (Re)create sprite/tile bitmaps at the computed size
        createOrUpdateBitmaps();
        recreateTileBitmapsIfNeeded();

        layoutControls();
        resume();
    }

    private void createOrUpdateBitmaps() {
        // recycle old generated bitmaps to save memory
        // (Bitmaps created from drawables; safe to let GC collect, but recycle proactively)
        // Note: skipping recycle for atlasBitmap as it's managed from assets decode
        bmpFloor = null; bmpWall = null; bmpNpc = null; bmpPlayerIdle = null;
        bmpPlayerWalk[0] = null; bmpPlayerWalk[1] = null;

        int tile = Math.max(8, tileSize);
        int sprite = Math.max(8, (int) (tileSize * 0.9f));
        bmpFloor = fromDrawable(R.drawable.tile_floor, tile, tile);
        bmpWall = fromDrawable(R.drawable.tile_wall, tile, tile);
        bmpPlayerIdle = fromDrawable(R.drawable.sprite_player, sprite, sprite);
        bmpPlayerWalk[0] = fromDrawable(R.drawable.sprite_player_walk1, sprite, sprite);
        bmpPlayerWalk[1] = fromDrawable(R.drawable.sprite_player_walk2, sprite, sprite);
        bmpNpc = fromDrawable(R.drawable.sprite_npc, sprite, sprite);

        // Directional: load if present (falls back silently if missing)
        // Player
        int[][] pIds = new int[][]{
                { getId("sprite_player_down_1"), getId("sprite_player_down_2") },
                { getId("sprite_player_left_1"), getId("sprite_player_left_2") },
                { getId("sprite_player_right_1"), getId("sprite_player_right_2") },
                { getId("sprite_player_up_1"), getId("sprite_player_up_2") }
        };
        int[] pIdle = new int[]{
                getId("sprite_player_down_1"),
                getId("sprite_player_left_1"),
                getId("sprite_player_right_1"),
                getId("sprite_player_up_1")
        };
        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 2; f++) {
                playerWalkDir[d][f] = pIds[d][f] != 0 ? fromDrawable(pIds[d][f], sprite, sprite) : null;
            }
            playerIdleDir[d] = pIdle[d] != 0 ? fromDrawable(pIdle[d], sprite, sprite) : null;
        }
        // NPC
        int[][] nIds = new int[][]{
                { getId("sprite_npc_down_1"), getId("sprite_npc_down_2") },
                { getId("sprite_npc_left_1"), getId("sprite_npc_left_2") },
                { getId("sprite_npc_right_1"), getId("sprite_npc_right_2") },
                { getId("sprite_npc_up_1"), getId("sprite_npc_up_2") }
        };
        int[] nIdle = new int[]{
                getId("sprite_npc_down_1"),
                getId("sprite_npc_left_1"),
                getId("sprite_npc_right_1"),
                getId("sprite_npc_up_1")
        };
        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 2; f++) {
                npcWalkDir[d][f] = nIds[d][f] != 0 ? fromDrawable(nIds[d][f], sprite, sprite) : null;
            }
            npcIdleDir[d] = nIdle[d] != 0 ? fromDrawable(nIdle[d], sprite, sprite) : null;
        }

        // Attempt to load external sprite sheets (falls back to shapes if missing)
        if (loadDirectionalSpriteSheet("sprites/heroine.png", playerIdleDir, playerWalkDir)) {
            bmpPlayerIdle = playerIdleDir[DIR_DOWN];
        }
        loadDirectionalSpriteSheet("sprites/villager.png", npcIdleDir, npcWalkDir);
    }

    private int getId(String name) {
        if (name == null || name.isEmpty()) return 0;
        return getResources().getIdentifier(name, "drawable", getContext().getPackageName());
    }

    private Bitmap fromDrawable(int resId, int width, int height) {
        Drawable d = ContextCompat.getDrawable(getContext(), resId);
        if (d == null) return null;
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return b;
    }

    private void layoutControls() {
        int w = getWidth();
        int h = getHeight();

        int pad = (int) (tileSize * 0.4f);
        int btn = (int) (tileSize * 1.2f);

        if (useJoystick) {
            float size = btn * 2.8f;
            joyCenter.set(pad + size * 0.9f, h - pad - size * 0.9f);
            joyBaseRadius = size * 0.5f;
            joyKnobRadius = joyBaseRadius * 0.35f;
            // D-pad rects unused
            btnUp.set(0,0,0,0); btnDown.set(0,0,0,0); btnLeft.set(0,0,0,0); btnRight.set(0,0,0,0);
        } else {
            // D-pad on bottom-left
            int baseX = pad;
            int baseY = h - (btn * 3 + pad);

            btnUp.set(baseX + btn, baseY, baseX + btn * 2, baseY + btn);
            btnLeft.set(baseX, baseY + btn, baseX + btn, baseY + btn * 2);
            btnDown.set(baseX + btn, baseY + btn * 2, baseX + btn * 2, baseY + btn * 3);
            btnRight.set(baseX + btn * 2, baseY + btn, baseX + btn * 3, baseY + btn * 2);
        }

        // A button on bottom-right
        int aSize = (int) (btn * 1.2f);
        btnA.set(w - aSize - pad, h - aSize - pad, w - pad, h - pad);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Recompute sizes & bitmaps on resize
        tileSize = Math.max(24, Math.min(width / 12, height / 8));
        int mapW = cols * tileSize;
        int mapH = rows * tileSize;
        centerCameraOnPlayer(width, height, mapW, mapH);
        createOrUpdateBitmaps();
        recreateTileBitmapsIfNeeded();
        layoutControls();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    public void resume() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        lastFrameNanos = System.nanoTime();
        gameThread.start();
    }

    public void pause() {
        running = false;
        if (gameThread != null) {
            try { gameThread.join(500); } catch (InterruptedException ignored) {}
            gameThread = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = (now - lastFrameNanos) / 1_000_000_000f;
            if (dt > 0.1f) dt = 0.1f;
            lastFrameNanos = now;

            try {
                update(dt);
            } catch (Throwable t) {
                android.util.Log.e("GameView", "update() crash", t);
                lastError = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
            }

            SurfaceHolder holder = getHolder();
            if (holder == null || holder.getSurface() == null || !holder.getSurface().isValid()) {
                try { Thread.sleep(8); } catch (InterruptedException ignored) {}
                continue;
            }

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) drawGame(canvas);
            } catch (Throwable t) {
                android.util.Log.e("GameView", "draw crash", t);
                lastError = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
            } finally {
                try {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (!dialogOpen) {
            float dx, dy;
            if (useJoystick && (Math.abs(joyVecX) > 0.01f || Math.abs(joyVecY) > 0.01f)) {
                dx = joyVecX; dy = joyVecY;
            } else {
                dx = (rightPressed ? 1 : 0) - (leftPressed ? 1 : 0);
                dy = (downPressed ? 1 : 0) - (upPressed ? 1 : 0);
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 0) { dx /= len; dy /= len; }
            }
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            moving = len > 0.001f;
            if (moving) {
                if (Math.abs(dx) >= Math.abs(dy)) {
                    playerDir = dx >= 0 ? DIR_RIGHT : DIR_LEFT;
                } else {
                    playerDir = dy >= 0 ? DIR_DOWN : DIR_UP;
                }
            }
            float step = moveSpeedTilesPerSec * dt;
            moveAndCollide(dx * step, dy * step);
            if (moving) animTime += dt; else animTime = 0f;
            // warp check with small cooldown to avoid loops
            if (warpCooldown > 0f) warpCooldown -= dt;
            if (warpCooldown <= 0f) checkAndApplyWarp();
        }
        // update camera to follow player
        updateCamera();
    }

    private void centerCameraOnPlayer(int viewW, int viewH, int mapW, int mapH) {
        float px = playerX * tileSize;
        float py = playerY * tileSize;
        cameraX = px - viewW / 2f;
        cameraY = py - viewH / 2f;
        if (cameraX < 0) cameraX = 0;
        if (cameraY < 0) cameraY = 0;
        if (cameraX > mapW - viewW) cameraX = Math.max(0, mapW - viewW);
        if (cameraY > mapH - viewH) cameraY = Math.max(0, mapH - viewH);
        mapOffsetX = -Math.round(cameraX);
        mapOffsetY = -Math.round(cameraY);
    }

    private void updateCamera() {
        int w = getWidth();
        int h = getHeight();
        int mapW = cols * tileSize;
        int mapH = rows * tileSize;
        centerCameraOnPlayer(w, h, mapW, mapH);
    }

    private void moveAndCollide(float dtx, float dty) {
        // move on X
        float newX = playerX + dtx;
        if (isWalkable(newX, playerY)) {
            playerX = newX;
        } else {
            // try to slide to tile boundary
            playerX = clipToWalkable(playerX, playerY, dtx, true);
        }
        // move on Y
        float newY = playerY + dty;
        if (isWalkable(playerX, newY)) {
            playerY = newY;
        } else {
            playerY = clipToWalkable(playerX, playerY, dty, false);
        }
        // clamp inside map
        playerX = Math.max(1.1f, Math.min(cols - 2.1f, playerX));
        playerY = Math.max(1.1f, Math.min(rows - 2.1f, playerY));
    }

    private boolean isWalkable(float fx, float fy) {
        int c = Math.round(fx);
        int r = Math.round(fy);
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        if (collisionMask != null) return !collisionMask[r][c];
        return map == null || map[r][c] == 0;
    }

    private float clipToWalkable(float fx, float fy, float delta, boolean isX) {
        // Move up to the boundary of the next tile
        float tryPos = (isX ? fx + delta : fy + delta);
        int tile = Math.round(tryPos);
        int r = Math.round(fy);
        int c = Math.round(fx);
        if (isX) {
            if (delta > 0) {
                // moving right: avoid hitting next column if wall
                if (tile < cols && map[r][tile] == 0) return tryPos;
                return tile - 0.51f;
            } else {
                if (tile >= 0 && map[r][tile] == 0) return tryPos;
                return tile + 0.51f;
            }
        } else {
            if (delta > 0) {
                if (tile < rows && map[tile][c] == 0) return tryPos;
                return tile - 0.51f;
            } else {
                if (tile >= 0 && map[tile][c] == 0) return tryPos;
                return tile + 0.51f;
            }
        }
    }

    private void drawGame(Canvas g) {
        g.drawColor(Color.rgb(235, 235, 235));
        drawMap(g);
        drawNpcs(g);
        drawPlayer(g);
        drawControls(g);
        if (dialogOpen) drawDialog(g);
        if (lastError != null) drawErrorOverlay(g, lastError);
        if (debugHud) drawDebugHud(g);
    }

    private void drawErrorOverlay(Canvas g, String msg) {
        int w = getWidth();
        int pad = Math.max(8, (int)(tileSize * 0.3f));
        Paint bg = new Paint(); bg.setColor(Color.argb(210, 200, 40, 40));
        Rect box = new Rect(pad, pad, w - pad, pad + (int)(tileSize * 2.6f));
        g.drawRect(box, bg);
        Paint tp = new Paint(paintText);
        tp.setColor(Color.WHITE);
        tp.setTextSize(Math.max(22f, tileSize * 0.5f));
        g.drawText("?¤ë¥ ë°ì: " + msg, box.left + pad, box.top + tp.getTextSize() * 1.1f, tp);
        g.drawText("?ì¸???´ì©? Logcat ?ì¸", box.left + pad, box.top + tp.getTextSize() * 2.2f, tp);
    }

    private void drawDebugHud(Canvas g) {
        int pad = Math.max(8, (int)(tileSize*0.3f));
        Paint bg = new Paint(); bg.setColor(Color.argb(160, 0, 0, 0));
        Rect box = new Rect(pad, pad, pad + (int)(tileSize*8), pad + (int)(tileSize*3));
        g.drawRect(box, bg);
        Paint tp = new Paint(paintText); tp.setColor(Color.WHITE); tp.setTextSize(Math.max(18f, tileSize*0.45f));
        int y = box.top + (int)(tp.getTextSize()*1.2f);
        g.drawText(String.format(java.util.Locale.US, "FPS ~%.0f", fps()), box.left + pad, y, tp); y += tp.getTextSize()*1.2f;
        g.drawText(String.format(java.util.Locale.US, "Pos %.2f,%.2f tile", playerX, playerY), box.left + pad, y, tp); y += tp.getTextSize()*1.2f;
        g.drawText(String.format(java.util.Locale.US, "Map %dx%d tile", cols, rows), box.left + pad, y, tp);
    }

    private void handleDebugTap() {
        long now = System.currentTimeMillis();
        if (now - debugLastTapMs < 600) {
            debugTapCount++;
        } else {
            debugTapCount = 1;
        }
        debugLastTapMs = now;
        if (debugTapCount >= 3) {
            debugTapCount = 0; debugHud = !debugHud;
        }
    }

    // crude fps estimation over last frame duration
    private float fps() {
        float dt = Math.max(1e-3f, (System.nanoTime() - lastFrameNanos) / 1_000_000_000f);
        return 1f / dt;
    }

    private void drawMap(Canvas g) {
        // Draw image layers first (if any)
        if (!imageLayers.isEmpty()) {
            for (MapData.ImageLayer il : imageLayers) {
                if (il == null || il.bitmap == null) continue;
                int x = mapOffsetX + il.offsetX;
                int y = mapOffsetY + il.offsetY;
                g.drawBitmap(il.bitmap, x, y, null);
            }
        }
        if (tileLayers != null && !tileLayers.isEmpty()) {
            for (int[][] layer : tileLayers) drawLayer(g, layer);
        } else if (map != null) {
            drawLayer(g, map);
        }
    }

    private void drawLayer(Canvas g, int[][] layer) {
        if (layer == null) return;
        // compute visible tile range (camera culling)
        int viewW = getWidth();
        int viewH = getHeight();
        int startC = Math.max(0, (int) Math.floor((-mapOffsetX) / (float) tileSize));
        int endC = Math.min(cols - 1, (int) Math.floor(((-mapOffsetX) + viewW) / (float) tileSize));
        int startR = Math.max(0, (int) Math.floor((-mapOffsetY) / (float) tileSize));
        int endR = Math.min(rows - 1, (int) Math.floor(((-mapOffsetY) + viewH) / (float) tileSize));
        int maxR = Math.min(rows, layer.length);
        startR = Math.max(0, Math.min(startR, maxR - 1));
        endR = Math.max(0, Math.min(endR, maxR - 1));
        for (int r = startR; r <= endR; r++) {
            int[] rowArr = layer[r];
            if (rowArr == null) continue;
            int maxC = Math.min(cols, rowArr.length);
            int sC = Math.max(0, Math.min(startC, maxC - 1));
            int eC = Math.max(0, Math.min(endC, maxC - 1));
            for (int c = sC; c <= eC; c++) {
                int left = mapOffsetX + c * tileSize;
                int top = mapOffsetY + r * tileSize;
                int right = left + tileSize;
                int bottom = top + tileSize;
                int idx = rowArr[c];
                if (atlasBitmap != null && idx != 0) {
                    int gid = idx;
                    int local = gid - atlasFirstGid;
                    int colsAtlas = atlasColumns > 0 ? atlasColumns : (atlasTileW > 0 ? (atlasBitmap.getWidth() / atlasTileW) : 0);
                    if (colsAtlas <= 0 || atlasTileW <= 0 || atlasTileH <= 0) {
                        // atlas invalid: draw fallback colors
                        if (idx == 0) g.drawRect(left, top, right, bottom, paintTile); else g.drawRect(left, top, right, bottom, paintWall);
                    } else if (local >= 0) {
                        int sx = (local % colsAtlas) * atlasTileW;
                        int sy = (local / colsAtlas) * atlasTileH;
                        android.graphics.Rect src = new android.graphics.Rect(sx, sy, sx + atlasTileW, sy + atlasTileH);
                        android.graphics.Rect dst = new android.graphics.Rect(left, top, right, bottom);
                        g.drawBitmap(atlasBitmap, src, dst, null);
                    }
                } else if (tileBitmaps != null) {
                    if (idx >= 0 && idx < tileBitmaps.length && tileBitmaps[idx] != null) {
                        g.drawBitmap(tileBitmaps[idx], left, top, null);
                    } else {
                        if (idx == 0) g.drawRect(left, top, right, bottom, paintTile); else g.drawRect(left, top, right, bottom, paintWall);
                    }
                } else {
                    if (idx == 0) {
                        if (bmpFloor != null) g.drawBitmap(bmpFloor, left, top, null);
                        else g.drawRect(left, top, right, bottom, paintTile);
                    } else {
                        if (bmpWall != null) g.drawBitmap(bmpWall, left, top, null);
                        else g.drawRect(left, top, right, bottom, paintWall);
                    }
                }
            }
        }
    }

    private void drawPlayer(Canvas g) {
        float px = mapOffsetX + playerX * tileSize;
        float py = mapOffsetY + playerY * tileSize;
        Bitmap current = null;
        if (moving && playerWalkDir[playerDir][0] != null && playerWalkDir[playerDir][1] != null) {
            int frame = ((int) (animTime * 8f)) % 2; // ~8 fps
            current = playerWalkDir[playerDir][frame];
        } else if (playerIdleDir[playerDir] != null) {
            // subtle idle bob using animTime
            int frame = ((int) (animTime * 2f)) % 2;
            current = (frame == 0 ? playerIdleDir[playerDir]
                    : (playerWalkDir[playerDir][0] != null ? playerWalkDir[playerDir][0] : playerIdleDir[playerDir]));
        } else if (moving && bmpPlayerWalk[0] != null && bmpPlayerWalk[1] != null) {
            int frame = ((int) (animTime * 8f)) % 2;
            current = bmpPlayerWalk[frame];
        } else {
            current = bmpPlayerIdle != null ? bmpPlayerIdle : null;
        }
        if (current != null) {
            int size = current.getWidth();
            float left = px - size / 2f;
            float top = py - size / 2f;
            g.drawBitmap(current, left, top, null);
        } else {
            g.drawCircle(px, py, playerRadiusPx, paintPlayer);
        }
    }

    private void drawNpcs(Canvas g) {
        for (Npc n : npcs) {
            // face toward player
            float dx = playerX - n.col;
            float dy = playerY - n.row;
            int dir;
            if (Math.abs(dx) >= Math.abs(dy)) dir = dx >= 0 ? DIR_RIGHT : DIR_LEFT;
            else dir = dy >= 0 ? DIR_DOWN : DIR_UP;

            Bitmap current = null;
            int frame = ((int) (animTime * 2f)) % 2; // slow idle bounce
            if (npcIdleDir[dir] != null || npcWalkDir[dir][0] != null) {
                current = (frame == 0 ? npcIdleDir[dir]
                        : (npcWalkDir[dir][0] != null ? npcWalkDir[dir][0] : npcIdleDir[dir]));
            }

            if (current != null) {
                int size = current.getWidth();
                int left = mapOffsetX + Math.round((n.col) * tileSize - size / 2f);
                int top = mapOffsetY + Math.round((n.row) * tileSize - size / 2f);
                g.drawBitmap(current, left, top, null);
            } else if (bmpNpc != null) {
                int size = bmpNpc.getWidth();
                int left = mapOffsetX + Math.round(n.col * tileSize - size / 2f);
                int top = mapOffsetY + Math.round(n.row * tileSize - size / 2f);
                g.drawBitmap(bmpNpc, left, top, null);
            } else {
                int size = (int) (tileSize * 0.8f);
                int left = mapOffsetX + Math.round(n.col * tileSize - size / 2f);
                int top = mapOffsetY + Math.round(n.row * tileSize - size / 2f);
                g.drawRect(left, top, left + size, top + size, paintNpc);
            }
        }
    }

    private void drawControls(Canvas g) {
        if (!useJoystick) {
            // D-pad buttons
            g.drawRect(btnUp, paintUi); g.drawRect(btnUp, paintUiStroke);
            g.drawRect(btnDown, paintUi); g.drawRect(btnDown, paintUiStroke);
            g.drawRect(btnLeft, paintUi); g.drawRect(btnLeft, paintUiStroke);
            g.drawRect(btnRight, paintUi); g.drawRect(btnRight, paintUiStroke);
            paintText.setColor(Color.WHITE);
            float txtSize = Math.max(28f, tileSize * 0.45f);
            paintText.setTextSize(txtSize);
            drawCenteredText(g, "\u25B2", btnUp, paintText);
            drawCenteredText(g, "\u25BC", btnDown, paintText);
            drawCenteredText(g, "\u25C0", btnLeft, paintText);
            drawCenteredText(g, "\u25B6", btnRight, paintText);
            paintText.setColor(Color.BLACK);
        } else {
            // Joystick base
            Paint base = new Paint(paintUi);
            base.setColor(Color.argb(80, 0, 0, 0));
            g.drawCircle(joyCenter.x, joyCenter.y, joyBaseRadius, base);
            g.drawCircle(joyCenter.x, joyCenter.y, joyBaseRadius, paintUiStroke);

            // Joystick knob
            float knobX = joyCenter.x + joyVecX * (joyBaseRadius * 0.6f);
            float knobY = joyCenter.y + joyVecY * (joyBaseRadius * 0.6f);
            Paint knob = new Paint(); knob.setColor(Color.argb(150, 255, 255, 255));
            g.drawCircle(knobX, knobY, joyKnobRadius, knob);
            g.drawCircle(knobX, knobY, joyKnobRadius, paintUiStroke);
        }

        // A button (always)
        g.drawRect(btnA, paintUi); g.drawRect(btnA, paintUiStroke);
        paintText.setColor(Color.WHITE);
        float txtSize = Math.max(28f, tileSize * 0.45f);
        paintText.setTextSize(txtSize);
        drawCenteredText(g, "A", btnA, paintText);
        paintText.setColor(Color.BLACK);
    }

    private void drawDialog(Canvas g) {
        int w = getWidth();
        int h = getHeight();
        int margin = (int) (tileSize * 0.6f);
        Rect box = new Rect(margin, h - (int) (tileSize * 4.2f), w - margin, h - margin);
        g.drawRect(box, paintDialogBg);

        float titleSize = Math.max(32f, tileSize * 0.55f);
        float bodySize = Math.max(28f, tileSize * 0.5f);
        Paint titlePaint = new Paint(paintText);
        titlePaint.setTextSize(titleSize);
        titlePaint.setColor(Color.rgb(33,33,33));

        Paint bodyPaint = new Paint(paintText);
        bodyPaint.setTextSize(bodySize);
        bodyPaint.setColor(Color.rgb(33,33,33));

        int padding = (int) (tileSize * 0.5f);
        g.drawText(dialogSpeaker, box.left + padding, box.top + padding + titleSize, titlePaint);

        // current line only
        String line = (dialogLines != null && dialogIndex >= 0 && dialogIndex < dialogLines.length)
                ? dialogLines[dialogIndex] : "";
        float y = box.top + padding + titleSize + padding + bodySize;
        g.drawText(line, box.left + padding, y, bodyPaint);

        // Footer: hint and page indicator
        String hint = (dialogLines != null && dialogIndex < dialogLines.length - 1) ? "A: \uB2E4\uC74C" : "A: \uB2EB\uAE30";
        Paint hintPaint = new Paint(paintText);
        float hintSize = Math.max(24f, tileSize * 0.45f);
        hintPaint.setTextSize(hintSize);
        hintPaint.setColor(Color.DKGRAY);
        float hintWidth = hintPaint.measureText(hint);
        g.drawText(hint, box.right - padding - hintWidth, box.bottom - padding, hintPaint);

        if (dialogLines != null) {
            String page = (dialogIndex + 1) + "/" + dialogLines.length;
            g.drawText(page, box.left + padding, box.bottom - padding, hintPaint);
        }
    }

    private void drawCenteredText(Canvas g, String s, Rect r, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        float tx = r.left + (r.width()) / 2f - p.measureText(s) / 2f;
        float ty = r.top + (r.height()) / 2f - (fm.ascent + fm.descent) / 2f;
        g.drawText(s, tx, ty, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        if (pointerIndex < 0 || pointerIndex >= event.getPointerCount()) {
            pointerIndex = Math.max(0, Math.min(event.getPointerCount() - 1, pointerIndex));
        }
        // hidden debug toggle: triple-tap near top-left corner
        if (action == MotionEvent.ACTION_DOWN) {
            float x = event.getX(pointerIndex), y = event.getY(pointerIndex);
            if (x < Math.max(60, tileSize * 1.2f) && y < Math.max(60, tileSize * 1.2f)) {
                handleDebugTap();
            }
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int x = (int) event.getX(pointerIndex);
                int y = (int) event.getY(pointerIndex);
                handlePress(event, pointerIndex, x, y);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (useJoystick && joyPointerId != -1) {
                    int idx = event.findPointerIndex(joyPointerId);
                    if (idx >= 0 && idx < event.getPointerCount()) {
                        float jx = event.getX(idx);
                        float jy = event.getY(idx);
                        updateJoystickVector(jx, jy);
                    }
                } else {
                    // D-pad: re-evaluate all pointers for continuous press tracking (movement only)
                    upPressed = downPressed = leftPressed = rightPressed = false;
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        int mx = (int) event.getX(i);
                        int my = (int) event.getY(i);
                        if (btnUp.contains(mx, my)) upPressed = true;
                        if (btnDown.contains(mx, my)) downPressed = true;
                        if (btnLeft.contains(mx, my)) leftPressed = true;
                        if (btnRight.contains(mx, my)) rightPressed = true;
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int x = (int) event.getX(pointerIndex);
                int y = (int) event.getY(pointerIndex);
                handleRelease(event, pointerIndex, x, y);
                // On last finger up, clear movement
                if (event.getPointerCount() <= 1) {
                    upPressed = downPressed = leftPressed = rightPressed = false;
                    if (useJoystick) { joyPointerId = -1; joyVecX = 0f; joyVecY = 0f; }
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void handlePress(MotionEvent event, int pointerIndex, int x, int y) {
        if (useJoystick) {
            float dx = x - joyCenter.x;
            float dy = y - joyCenter.y;
            if (dx*dx + dy*dy <= joyBaseRadius * joyBaseRadius && joyPointerId == -1) {
                joyPointerId = event.getPointerId(pointerIndex);
                updateJoystickVector(x, y);
                return;
            }
        } else {
            if (btnUp.contains(x, y)) upPressed = true;
            if (btnDown.contains(x, y)) downPressed = true;
            if (btnLeft.contains(x, y)) leftPressed = true;
            if (btnRight.contains(x, y)) rightPressed = true;
        }
        if (btnA.contains(x, y)) { tryStartOrAdvanceDialog(); }
    }

    private void handleRelease(MotionEvent event, int pointerIndex, int x, int y) {
        if (useJoystick) {
            int pid = event.getPointerId(pointerIndex);
            if (pid == joyPointerId) { joyPointerId = -1; joyVecX = 0f; joyVecY = 0f; return; }
        } else {
            if (btnUp.contains(x, y)) upPressed = false;
            if (btnDown.contains(x, y)) downPressed = false;
            if (btnLeft.contains(x, y)) leftPressed = false;
            if (btnRight.contains(x, y)) rightPressed = false;
        }
        // A button is tap-only; no continuous state
    }

    private void updateJoystickVector(float x, float y) {
        float dx = x - joyCenter.x;
        float dy = y - joyCenter.y;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len < joyBaseRadius * 0.1f) { joyVecX = 0f; joyVecY = 0f; return; }
        if (len > 0f) { dx /= len; dy /= len; }
        joyVecX = Math.max(-1f, Math.min(1f, dx));
        joyVecY = Math.max(-1f, Math.min(1f, dy));
    }

    // Keyboard / gamepad support
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W: upPressed = true; return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S: downPressed = true; return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A: leftPressed = true; return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D: rightPressed = true; return true;
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_Z:
            case KeyEvent.KEYCODE_BUTTON_A:
                tryStartOrAdvanceDialog(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W: upPressed = false; return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S: downPressed = false; return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A: leftPressed = false; return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D: rightPressed = false; return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void tryStartOrAdvanceDialog() {
        if (dialogOpen) {
            if (dialogLines != null && dialogIndex < dialogLines.length - 1) {
                dialogIndex++;
            } else {
                dialogOpen = false;
                dialogNpc = null;
                dialogLines = null;
                dialogIndex = 0;
            }
            return;
        }
        // If not open: check for adjacent NPC
        Npc target = findAdjacentNpc();
        if (target != null) {
            dialogOpen = true;
            dialogNpc = target;
            dialogSpeaker = target.name;
            // Defensive: ensure non-null lines to avoid NPE on some devices
            if (target.lines == null || target.lines.length == 0) {
                dialogLines = new String[]{"..."};
            } else {
                dialogLines = target.lines;
            }
            dialogIndex = 0;
        }
    }

    private Npc findAdjacentNpc() {
        int pc = Math.round(playerX);
        int pr = Math.round(playerY);
        for (Npc n : npcs) {
            int manhattan = Math.abs(n.col - pc) + Math.abs(n.row - pr);
            if (manhattan == 1) return n;
        }
        return null;
    }

    
    private boolean loadMapFromAssetsSafe(String assetPath) {
        try {
            MapData data = MapLoader.load(getContext(), assetPath);
            if (applyMap(data)) {
                currentMapAsset = assetPath;
                if (getWidth() > 0 && getHeight() > 0) {
                    createOrUpdateBitmaps();
                    recreateTileBitmapsIfNeeded();
                    centerCameraOnPlayer(getWidth(), getHeight(), cols * tileSize, rows * tileSize);
                }
                return true;
            }
        } catch (Exception e) {
            android.util.Log.e("GameView", "Failed to load map " + assetPath, e);
        }
        return false;
    }

    private boolean applyMap(MapData data) {
        if (data == null) return false;
        cols = data.cols > 0 ? data.cols : cols;
        rows = data.rows > 0 ? data.rows : rows;

        tileLayers.clear();
        if (data.tileLayers != null && !data.tileLayers.isEmpty()) {
            tileLayers.addAll(data.tileLayers);
            map = tileLayers.get(0);
        } else {
            map = null;
        }

        collisionMask = data.collision != null ? data.collision : new boolean[rows][cols];

        tileDrawableNames = data.tileDrawableNames;
        tilePaletteColors = data.tilePaletteColors;
        atlasBitmap = data.atlasBitmap;
        atlasColumns = data.atlasColumns;
        atlasTileW = data.atlasTileW;
        atlasTileH = data.atlasTileH;
        atlasFirstGid = data.atlasFirstGid;

        imageLayers.clear();
        if (data.imageLayers != null && !data.imageLayers.isEmpty()) {
            imageLayers.addAll(data.imageLayers);
        }

        npcs.clear();
        if (data.npcs != null) {
            for (MapData.Npc npcData : data.npcs) {
                npcs.add(new Npc(npcData.col, npcData.row, npcData.name, npcData.lines));
            }
        }

        warps.clear();
        if (data.warps != null) {
            for (MapData.Warp w : data.warps) {
                Warp copy = new Warp();
                copy.col = w.col;
                copy.row = w.row;
                copy.target = w.target;
                copy.targetCol = w.targetCol;
                copy.targetRow = w.targetRow;
                warps.add(copy);
            }
        }

        if (data.playerCol >= 0 && data.playerRow >= 0) {
            playerX = data.playerCol;
            playerY = data.playerRow;
        }
        return true;
    }

    private void buildTileBitmapsFromDrawables() {
        if (tileDrawableNames == null) { tileBitmaps = null; return; }
        tileBitmaps = new Bitmap[tileDrawableNames.length];
        for (int i = 0; i < tileDrawableNames.length; i++) {
            String name = tileDrawableNames[i];
            if (name == null || name.isEmpty()) { tileBitmaps[i] = null; continue; }
            int resId = getResources().getIdentifier(name, "drawable", getContext().getPackageName());
            if (resId != 0) tileBitmaps[i] = fromDrawable(resId, tileSize, tileSize);
        }
    }

    private void buildTileBitmapsFromPalette() {
        if (tilePaletteColors == null) { tileBitmaps = null; return; }
        tileBitmaps = new Bitmap[tilePaletteColors.length];
        for (int i = 0; i < tilePaletteColors.length; i++) {
            Bitmap b = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Paint p = new Paint();
            p.setColor(tilePaletteColors[i]);
            c.drawRect(0, 0, tileSize, tileSize, p);
            tileBitmaps[i] = b;
        }
    }

    private void recreateTileBitmapsIfNeeded() {
        if (tileDrawableNames != null) buildTileBitmapsFromDrawables();
        else if (tilePaletteColors != null) buildTileBitmapsFromPalette();
    }

    private boolean loadDirectionalSpriteSheet(String assetPath, Bitmap[] idleOut, Bitmap[][] walkOut) {
        try (java.io.InputStream is = getContext().getAssets().open(assetPath)) {
            Bitmap sheet = BitmapFactory.decodeStream(is);
            if (sheet == null) return false;
            final int columns = 4;
            final int rows = Math.min(4, sheet.getHeight() / Math.max(1, sheet.getHeight() / 4));
            int cellW = sheet.getWidth() / columns;
            int cellH = sheet.getHeight() / rows;
            int idleFrame = 0;
            int walkFrameA = Math.min(1, columns - 1);
            int walkFrameB = Math.min(2, columns - 1);
            for (int dir = 0; dir < 4 && dir < rows; dir++) {
                idleOut[dir] = trimSpriteFrame(Bitmap.createBitmap(sheet, idleFrame * cellW, dir * cellH, cellW, cellH));
                walkOut[dir][0] = trimSpriteFrame(Bitmap.createBitmap(sheet, walkFrameA * cellW, dir * cellH, cellW, cellH));
                walkOut[dir][1] = trimSpriteFrame(Bitmap.createBitmap(sheet, walkFrameB * cellW, dir * cellH, cellW, cellH));
            }
            return true;
        } catch (Exception e) {
            android.util.Log.w("GameView", "Sprite sheet not found: " + assetPath);
            return false;
        }
    }

    private Bitmap trimSpriteFrame(Bitmap frame) {
        if (frame == null) return null;
        Rect bounds = findOpaqueBounds(frame, 8);
        return Bitmap.createBitmap(frame, bounds.left, bounds.top, Math.max(1, bounds.width()), Math.max(1, bounds.height()));
    }

    private Rect findOpaqueBounds(Bitmap bmp, int alphaThreshold) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (pixels[y * w + x] >>> 24) & 0xFF;
                if (a > alphaThreshold) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) return new Rect(0, 0, w, h);
        int pad = Math.max(1, Math.round(Math.min(w, h) * 0.05f));
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);
        return new Rect(minX, minY, maxX + 1, maxY + 1);
    }

    private void checkAndApplyWarp() {
        if (warps.isEmpty()) return;
        int pc = Math.round(playerX);
        int pr = Math.round(playerY);
        for (Warp w : warps) {
            if (w.col == pc && w.row == pr) {
                boolean loaded = true;
                if (w.target != null && !w.target.isEmpty()) {
                    loaded = loadMapFromAssetsSafe(w.target);
                }
                // position after warp (either in current or new map)
                playerX = w.targetCol; playerY = w.targetRow;
                // refresh camera/bitmaps (view size unchanged)
                int viewW = getWidth();
                int viewH = getHeight();
                int mapW = cols * tileSize;
                int mapH = rows * tileSize;
                centerCameraOnPlayer(viewW, viewH, mapW, mapH);
                createOrUpdateBitmaps();
                recreateTileBitmapsIfNeeded();
                warpCooldown = 0.5f;
                break;
            }
        }
    }
}

