package com.non_breath.finlitrush.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.ContextCompat;

import com.non_breath.finlitrush.R;

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

    // Bitmaps (generated from drawable resources at runtime)
    private Bitmap bmpFloor, bmpWall, bmpNpc;
    private Bitmap bmpPlayerIdle;
    private Bitmap[] bmpPlayerWalk = new Bitmap[2];
    private float animTime = 0f;
    private boolean moving = false;

    // Tileset support
    private Bitmap[] tileBitmaps = null;           // per-index tiles from drawables/palette
    private String[] tileDrawableNames = null;     // to rebuild on resize
    private int[] tilePaletteColors = null;        // to rebuild on resize
    // Atlas (Tiled JSON) support
    private Bitmap atlasBitmap = null;
    private int atlasTileW = 0, atlasTileH = 0, atlasColumns = 0, atlasFirstGid = 1;

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
        if (!loadMapFromAssetsSafe("maps/tiled_map.json") && !loadMapFromAssetsSafe("maps/demo_map.json")) {
            buildDemoMap();
        }
        // Add few NPCs
        npcs.add(new Npc(10, 6, "NPC", new String[]{
                "안녕! 데모 NPC야.",
                "A 버튼을 눌러 대화를 넘겨봐.",
                "벽은 회색, 바닥은 밝은 회색이야.",
        }));
        npcs.add(new Npc(5, 4, "상인", new String[]{
                "여긴 시험용 맵이야.",
                "맵과 에셋은 곧 교체 가능!",
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
        int tile = Math.max(8, tileSize);
        int sprite = Math.max(8, (int) (tileSize * 0.9f));
        bmpFloor = fromDrawable(R.drawable.tile_floor, tile, tile);
        bmpWall = fromDrawable(R.drawable.tile_wall, tile, tile);
        bmpPlayerIdle = fromDrawable(R.drawable.sprite_player, sprite, sprite);
        bmpPlayerWalk[0] = fromDrawable(R.drawable.sprite_player_walk1, sprite, sprite);
        bmpPlayerWalk[1] = fromDrawable(R.drawable.sprite_player_walk2, sprite, sprite);
        bmpNpc = fromDrawable(R.drawable.sprite_npc, sprite, sprite);
    }

    private Bitmap fromDrawable(int resId, int width, int height) {
        Drawable d = ContextCompat.getDrawable(getContext(), resId);
        if (d == null) return null;
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, width, height);
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
            if (dt > 0.1f) dt = 0.1f; // clamp to avoid big jumps
            lastFrameNanos = now;

            update(dt);

            Canvas canvas = null;
            try {
                canvas = getHolder().lockCanvas();
                if (canvas != null) drawGame(canvas);
            } finally {
                if (canvas != null) getHolder().unlockCanvasAndPost(canvas);
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
            float step = moveSpeedTilesPerSec * dt;
            moveAndCollide(dx * step, dy * step);
            if (moving) animTime += dt; else animTime = 0f;
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
    }

    private void drawMap(Canvas g) {
        if (tileLayers != null && !tileLayers.isEmpty()) {
            for (int[][] layer : tileLayers) drawLayer(g, layer);
        } else if (map != null) {
            drawLayer(g, map);
        }
    }

    private void drawLayer(Canvas g, int[][] layer) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int left = mapOffsetX + c * tileSize;
                int top = mapOffsetY + r * tileSize;
                int right = left + tileSize;
                int bottom = top + tileSize;
                int idx = layer[r][c];
                if (atlasBitmap != null && idx != 0) {
                    int gid = idx;
                    int local = gid - atlasFirstGid;
                    if (local >= 0) {
                        int sx = (local % atlasColumns) * atlasTileW;
                        int sy = (local / atlasColumns) * atlasTileH;
                        android.graphics.Rect src = new android.graphics.Rect(sx, sy, sx + atlasTileW, sy + atlasTileH);
                        android.graphics.Rect dst = new android.graphics.Rect(left, top, right, bottom);
                        g.drawBitmap(atlasBitmap, src, dst, null);
                    }
                } else if (tileBitmaps != null && idx != 0) {
                    int local = idx;
                    // If Tiled JSON provided GIDs, convert to local index
                    if (atlasFirstGid > 0 && idx >= atlasFirstGid) local = idx - atlasFirstGid;
                    if (local >= 0 && local < tileBitmaps.length && tileBitmaps[local] != null) {
                        g.drawBitmap(tileBitmaps[local], left, top, null);
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
        if (moving && bmpPlayerWalk[0] != null && bmpPlayerWalk[1] != null) {
            int frame = ((int) (animTime * 8f)) % 2; // ~8 fps
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
            int size = bmpNpc != null ? bmpNpc.getWidth() : (int) (tileSize * 0.8f);
            int left = mapOffsetX + Math.round((n.col - 0.5f) * tileSize - size / 2f);
            int top = mapOffsetY + Math.round((n.row - 0.5f) * tileSize - size / 2f);
            if (bmpNpc != null) {
                g.drawBitmap(bmpNpc, left, top, null);
            } else {
                int right = left + size;
                int bottom = top + size;
                g.drawRect(left, top, right, bottom, paintNpc);
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
            drawCenteredText(g, "▲", btnUp, paintText);
            drawCenteredText(g, "▼", btnDown, paintText);
            drawCenteredText(g, "◀", btnLeft, paintText);
            drawCenteredText(g, "▶", btnRight, paintText);
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
        String hint = (dialogLines != null && dialogIndex < dialogLines.length - 1) ? "A: 다음" : "A: 닫기";
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
            dialogLines = target.lines;
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

    private boolean loadMapFromAssetsSafe(String path) {
        try {
            return loadMapFromAssets(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean loadMapFromAssets(String path) throws Exception {
        Context ctx = getContext();
        try (java.io.InputStream is = ctx.getAssets().open(path)) {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String json = sb.toString();

            org.json.JSONObject root = new org.json.JSONObject(json);
            // Tiled JSON detection
            if (root.has("layers") && root.has("tilesets")) {
                return parseTiledJson(root);
            }

            int newCols = root.getInt("cols");
            int newRows = root.getInt("rows");
            org.json.JSONArray tiles = root.getJSONArray("tiles");
            int[][] newMap = new int[newRows][newCols];
            for (int r = 0; r < newRows; r++) {
                org.json.JSONArray rowArr = tiles.getJSONArray(r);
                for (int c = 0; c < newCols; c++) {
                    newMap[r][c] = rowArr.getInt(c);
                }
            }

            // Optional tileset
            atlasBitmap = null; atlasTileW = atlasTileH = atlasColumns = 0; atlasFirstGid = 1;
            tileBitmaps = null; tileDrawableNames = null; tilePaletteColors = null;
            if (root.has("tileset")) {
                org.json.JSONObject ts = root.getJSONObject("tileset");
                String type = ts.optString("type", "");
                if ("android-drawables".equals(type)) {
                    org.json.JSONArray arr = ts.getJSONArray("drawables");
                    tileDrawableNames = new String[arr.length()];
                    for (int i = 0; i < arr.length(); i++) tileDrawableNames[i] = arr.getString(i);
                    buildTileBitmapsFromDrawables();
                } else if (ts.has("palette")) {
                    org.json.JSONArray pal = ts.getJSONArray("palette");
                    tilePaletteColors = new int[pal.length()];
                    for (int i = 0; i < pal.length(); i++) tilePaletteColors[i] = android.graphics.Color.parseColor(pal.getString(i));
                    buildTileBitmapsFromPalette();
                } else if (ts.has("atlas")) {
                    org.json.JSONObject a = ts.getJSONObject("atlas");
                    String imagePath = a.getString("image");
                    atlasTileW = a.getInt("tileW");
                    atlasTileH = a.getInt("tileH");
                    atlasColumns = a.getInt("columns");
                    atlasFirstGid = a.optInt("firstgid", 1);
                    try (java.io.InputStream ais = ctx.getAssets().open(imagePath)) {
                        atlasBitmap = android.graphics.BitmapFactory.decodeStream(ais);
                    }
                }
            }

            // player
            if (root.has("player")) {
                org.json.JSONObject p = root.getJSONObject("player");
                playerX = (float) p.optDouble("col", 2);
                playerY = (float) p.optDouble("row", 2);
            }

            // npcs
            npcs.clear();
            if (root.has("npcs")) {
                org.json.JSONArray arr = root.getJSONArray("npcs");
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject n = arr.getJSONObject(i);
                    int col = n.getInt("col");
                    int row = n.getInt("row");
                    String name = n.optString("name", "NPC");
                    org.json.JSONArray linesArr = n.optJSONArray("lines");
                    String[] ls;
                    if (linesArr != null) {
                        ls = new String[linesArr.length()];
                        for (int j = 0; j < linesArr.length(); j++) ls[j] = linesArr.getString(j);
                    } else {
                        ls = new String[]{"..."};
                    }
                    npcs.add(new Npc(col, row, name, ls));
                }
            }

            // apply
            cols = newCols;
            rows = newRows;
            map = newMap;
            // layers
            tileLayers.clear();
            tileLayers.add(map);
            // default collision from non-zero tiles
            collisionMask = new boolean[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) collisionMask[r][c] = map[r][c] != 0;
            }
            // optional custom collision matrix (0/1)
            if (root.has("collision")) {
                org.json.JSONArray coll = root.getJSONArray("collision");
                for (int r = 0; r < rows; r++) {
                    org.json.JSONArray rowArr = coll.getJSONArray(r);
                    for (int c = 0; c < cols; c++) collisionMask[r][c] = rowArr.getInt(c) != 0;
                }
            }
            return true;
        }
    }

    private boolean parseTiledJson(org.json.JSONObject root) throws Exception {
        int newCols = root.getInt("width");
        int newRows = root.getInt("height");
        int tw = root.getInt("tilewidth");
        int th = root.getInt("tileheight");
        org.json.JSONArray layers = root.getJSONArray("layers");
        java.util.List<int[][]> draw = new java.util.ArrayList<>();
        boolean[][] coll = new boolean[newRows][newCols];
        for (int i = 0; i < layers.length(); i++) {
            org.json.JSONObject layer = layers.getJSONObject(i);
            if (!"tilelayer".equals(layer.optString("type"))) continue;
            org.json.JSONArray data = layer.getJSONArray("data");
            int[][] grid = new int[newRows][newCols];
            for (int r = 0; r < newRows; r++) {
                for (int c = 0; c < newCols; c++) grid[r][c] = data.getInt(r * newCols + c);
            }
            String name = layer.optString("name", "");
            boolean isCollision = name.equalsIgnoreCase("collision") || name.equalsIgnoreCase("collide");
            if (isCollision) {
                for (int r = 0; r < newRows; r++) {
                    for (int c = 0; c < newCols; c++) coll[r][c] = coll[r][c] || grid[r][c] != 0;
                }
            } else {
                draw.add(grid);
            }
        }
        org.json.JSONArray tilesets = root.getJSONArray("tilesets");
        org.json.JSONObject ts0 = tilesets.getJSONObject(0);
        atlasFirstGid = ts0.getInt("firstgid");
        atlasColumns = ts0.optInt("columns", 0);
        atlasTileW = ts0.optInt("tilewidth", tw);
        atlasTileH = ts0.optInt("tileheight", th);
        tileBitmaps = null; tileDrawableNames = null; tilePaletteColors = null; atlasBitmap = null;
        // Prefer inline base64 if provided
        if (ts0.has("imageBase64")) {
            String b64 = ts0.getString("imageBase64");
            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            atlasBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else if (ts0.has("colors")) {
            org.json.JSONArray pal = ts0.getJSONArray("colors");
            tilePaletteColors = new int[pal.length()];
            for (int i = 0; i < pal.length(); i++) tilePaletteColors[i] = android.graphics.Color.parseColor(pal.getString(i));
            buildTileBitmapsFromPalette();
        } else if (ts0.has("image")) {
            String image = ts0.getString("image");
            Context ctx2 = getContext();
            try (java.io.InputStream ais = ctx2.getAssets().open(image)) {
                atlasBitmap = android.graphics.BitmapFactory.decodeStream(ais);
            }
        }
        cols = newCols; rows = newRows; map = null;
        tileLayers.clear(); tileLayers.addAll(draw);
        collisionMask = coll;
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
}
