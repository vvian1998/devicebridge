package com.devicebridge.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BridgeGameView extends View {

    public interface OnWinListener {
        void onWin();
    }

    private static final int PAD_DP = 24;
    private static final float ISLAND_RADIUS_DP = 24;
    private static final float LINE_WIDTH_DP = 5;
    private static final float LINE_GAP_DP = 7;
    private static final float TOUCH_RADIUS_DP = 42;

    private static final int BG_COLOR = 0xFF090D16;
    private static final int GRID_DOT_COLOR = 0xFF1E293B;
    private static final int ISLAND_FILL = 0xFF1E293B;
    private static final int ISLAND_SOLVED_FILL = 0xFF0F291E;
    private static final int ISLAND_STROKE = 0xFF3B82F6;
    private static final int ISLAND_SOLVED_STROKE = 0xFF10B981;
    private static final int TEXT_COLOR = 0xFFF8FAFC;
    private static final int BRIDGE_COLOR = 0xFF38BDF8;
    private static final int HIGHLIGHT_COLOR = 0xFF60A5FA;

    private BridgePuzzleGenerator.Puzzle puzzle;
    private OnWinListener winListener;

    private final Map<String, Integer> bridgeCounts = new HashMap<>();
    private int selected = -1;
    private boolean solved = false;

    private final Paint paintGridDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintIslandFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintIslandStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBridge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Vibrator vibrator;

    private float density;
    private float pad;
    private float cellW;
    private float cellH;

    public BridgeGameView(Context context) {
        super(context);
        init();
    }

    public BridgeGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        pad = PAD_DP * density;
        setBackgroundColor(BG_COLOR);

        try {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}

        paintGridDot.setColor(GRID_DOT_COLOR);
        paintGridDot.setStyle(Paint.Style.FILL);

        paintIslandFill.setStyle(Paint.Style.FILL);

        paintIslandStroke.setStyle(Paint.Style.STROKE);
        paintIslandStroke.setStrokeWidth(3.5f * density);

        paintBridge.setColor(BRIDGE_COLOR);
        paintBridge.setStyle(Paint.Style.STROKE);
        paintBridge.setStrokeCap(Paint.Cap.ROUND);
        paintBridge.setStrokeWidth(LINE_WIDTH_DP * density);

        paintHighlight.setColor(HIGHLIGHT_COLOR);
        paintHighlight.setStyle(Paint.Style.STROKE);
        paintHighlight.setStrokeWidth(4 * density);

        paintText.setColor(TEXT_COLOR);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(18 * density);
        paintText.setFakeBoldText(true);
    }

    public void setPuzzle(BridgePuzzleGenerator.Puzzle puzzle) {
        this.puzzle = puzzle;
        bridgeCounts.clear();
        selected = -1;
        solved = false;
        for (BridgePuzzleGenerator.Bridge b : puzzle.solution) {
            bridgeCounts.put(key(b.i1, b.i2), 0);
        }
        invalidate();
    }

    public void setOnWinListener(OnWinListener listener) {
        this.winListener = listener;
    }

    private String key(int a, int b) {
        return a < b ? a + "-" + b : b + "-" + a;
    }

    private float islandCenterX(BridgePuzzleGenerator.Island island) {
        return pad + island.col * cellW + cellW / 2f;
    }

    private float islandCenterY(BridgePuzzleGenerator.Island island) {
        return pad + island.row * cellH + cellH / 2f;
    }

    private float islandRadius() {
        return ISLAND_RADIUS_DP * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (puzzle == null) return;

        float w = getWidth();
        float h = getHeight();
        cellW = (w - 2 * pad) / BridgePuzzleGenerator.GRID_W;
        cellH = (h - 2 * pad) / BridgePuzzleGenerator.GRID_H;

        drawGridDots(canvas);
        drawBridges(canvas);
        drawIslands(canvas);
    }

    private void drawGridDots(Canvas canvas) {
        for (int r = 0; r < BridgePuzzleGenerator.GRID_H; r++) {
            for (int c = 0; c < BridgePuzzleGenerator.GRID_W; c++) {
                float cx = pad + c * cellW + cellW / 2f;
                float cy = pad + r * cellH + cellH / 2f;
                canvas.drawCircle(cx, cy, 2 * density, paintGridDot);
            }
        }
    }

    private void drawBridges(Canvas canvas) {
        for (BridgePuzzleGenerator.Bridge b : puzzle.solution) {
            int count = bridgeCounts.getOrDefault(key(b.i1, b.i2), 0);
            if (count <= 0) continue;

            BridgePuzzleGenerator.Island a = puzzle.islands.get(b.i1);
            BridgePuzzleGenerator.Island c = puzzle.islands.get(b.i2);

            float ax = islandCenterX(a), ay = islandCenterY(a);
            float cx = islandCenterX(c), cy = islandCenterY(c);

            boolean vertical = a.col == c.col;
            float gap = LINE_GAP_DP * density;

            if (vertical) {
                if (count == 2) {
                    canvas.drawLine(ax - gap, ay, ax - gap, cy, paintBridge);
                    canvas.drawLine(ax + gap, ay, ax + gap, cy, paintBridge);
                } else {
                    canvas.drawLine(ax, ay, ax, cy, paintBridge);
                }
            } else {
                if (count == 2) {
                    canvas.drawLine(ax, ay - gap, cx, ay - gap, paintBridge);
                    canvas.drawLine(ax, ay + gap, cx, ay + gap, paintBridge);
                } else {
                    canvas.drawLine(ax, ay, cx, ay, paintBridge);
                }
            }
        }
    }

    private void drawIslands(Canvas canvas) {
        for (int i = 0; i < puzzle.islands.size(); i++) {
            BridgePuzzleGenerator.Island island = puzzle.islands.get(i);
            float cx = islandCenterX(island);
            float cy = islandCenterY(island);
            float r = islandRadius();

            int currentConnected = getConnectedBridges(i);
            boolean nodeComplete = currentConnected == island.number;

            // Highlight ring if selected
            if (i == selected) {
                paintHighlight.setAlpha(220);
                canvas.drawCircle(cx, cy, r + 7 * density, paintHighlight);
            }

            // Fill & Stroke
            if (nodeComplete || solved) {
                paintIslandFill.setColor(ISLAND_SOLVED_FILL);
                paintIslandStroke.setColor(ISLAND_SOLVED_STROKE);
            } else {
                paintIslandFill.setColor(ISLAND_FILL);
                paintIslandStroke.setColor(ISLAND_STROKE);
            }

            canvas.drawCircle(cx, cy, r, paintIslandFill);
            canvas.drawCircle(cx, cy, r, paintIslandStroke);

            // Draw Island Number
            String text = String.valueOf(island.number);
            Paint.FontMetrics fm = paintText.getFontMetrics();
            float baseline = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(text, cx, baseline, paintText);
        }
    }

    private int getConnectedBridges(int islandIdx) {
        int total = 0;
        for (BridgePuzzleGenerator.Bridge br : puzzle.solution) {
            if (br.i1 == islandIdx || br.i2 == islandIdx) {
                total += bridgeCounts.getOrDefault(key(br.i1, br.i2), 0);
            }
        }
        return total;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (puzzle == null || solved) return true;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float tx = event.getX();
            float ty = event.getY();
            int tapped = findIsland(tx, ty);

            if (tapped >= 0) {
                vibrateTouch();
                if (selected == -1) {
                    selected = tapped;
                } else if (selected == tapped) {
                    selected = -1;
                } else if (canConnect(selected, tapped)) {
                    toggleBridge(selected, tapped);
                    selected = -1;
                    checkWin();
                } else {
                    selected = tapped;
                }
                invalidate();
            }
        }
        return true;
    }

    private void vibrateTouch() {
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(25);
                }
            } catch (Exception ignored) {}
        }
    }

    private int findIsland(float tx, float ty) {
        float touchRadius = TOUCH_RADIUS_DP * density;
        for (int i = 0; i < puzzle.islands.size(); i++) {
            BridgePuzzleGenerator.Island island = puzzle.islands.get(i);
            float dx = tx - islandCenterX(island);
            float dy = ty - islandCenterY(island);
            if (dx * dx + dy * dy <= touchRadius * touchRadius) {
                return i;
            }
        }
        return -1;
    }

    private boolean canConnect(int a, int b) {
        BridgePuzzleGenerator.Island ia = puzzle.islands.get(a);
        BridgePuzzleGenerator.Island ib = puzzle.islands.get(b);

        if (ia.col != ib.col && ia.row != ib.row) return false;

        for (int i = 0; i < puzzle.islands.size(); i++) {
            if (i == a || i == b) continue;
            BridgePuzzleGenerator.Island mid = puzzle.islands.get(i);
            if (ia.col == ib.col) {
                if (mid.col == ia.col
                        && mid.row > Math.min(ia.row, ib.row)
                        && mid.row < Math.max(ia.row, ib.row)) return false;
            } else {
                if (mid.row == ia.row
                        && mid.col > Math.min(ia.col, ib.col)
                        && mid.col < Math.max(ia.col, ib.col)) return false;
            }
        }
        return true;
    }

    private void toggleBridge(int a, int b) {
        String k = key(a, b);
        int current = bridgeCounts.getOrDefault(k, 0);
        int next = (current + 1) % 3;
        bridgeCounts.put(k, next);
    }

    private void checkWin() {
        for (int i = 0; i < puzzle.islands.size(); i++) {
            int count = getConnectedBridges(i);
            if (count != puzzle.islands.get(i).number) return;
        }

        if (!allConnected()) return;

        solved = true;
        if (winListener != null) {
            winListener.onWin();
        }
    }

    private boolean allConnected() {
        boolean[] visited = new boolean[puzzle.islands.size()];
        List<Integer> queue = new ArrayList<>();
        queue.add(0);
        visited[0] = true;

        while (!queue.isEmpty()) {
            int cur = queue.remove(0);
            for (BridgePuzzleGenerator.Bridge br : puzzle.solution) {
                if (bridgeCounts.getOrDefault(key(br.i1, br.i2), 0) <= 0) continue;

                int other = -1;
                if (br.i1 == cur) other = br.i2;
                else if (br.i2 == cur) other = br.i1;

                if (other >= 0 && !visited[other]) {
                    visited[other] = true;
                    queue.add(other);
                }
            }
        }

        for (boolean v : visited) {
            if (!v) return false;
        }
        return true;
    }

    public boolean isSolved() {
        return solved;
    }
}
