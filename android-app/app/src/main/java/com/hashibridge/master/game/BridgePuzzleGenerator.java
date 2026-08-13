package com.hashibridge.master.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BridgePuzzleGenerator {

    // Default grid size (kept for backward compat)
    public static final int GRID_W = 6;
    public static final int GRID_H = 6;

    public static class Island {
        public int col, row, number, x, y;
        public Island(int col, int row) {
            this.col = col; this.row = row;
        }
    }

    public static class Bridge {
        public int i1, i2, count;
        public Bridge(int i1, int i2, int count) {
            this.i1 = i1; this.i2 = i2; this.count = count;
        }
    }

    public static class Puzzle {
        public List<Island> islands = new ArrayList<>();
        public List<Bridge> solution = new ArrayList<>();
        public int gridW = GRID_W;
        public int gridH = GRID_H;

        public void reset() {
            for (Island i : islands) { i.number = 0; i.x = i.col; i.y = i.row; }
            solution.clear();
        }
    }

    /** Generate with explicit grid size for difficulty scaling. */
    public static Puzzle generate(Random rng, int numIslands, int gridSize) {
        Puzzle puzzle = new Puzzle();
        puzzle.gridW = gridSize;
        puzzle.gridH = gridSize;
        return _generate(rng, numIslands, gridSize, gridSize, puzzle);
    }

    /** Legacy: uses default 5x5 grid. */
    public static Puzzle generate(Random rng, int numIslands) {
        Puzzle puzzle = new Puzzle();
        return _generate(rng, numIslands, GRID_W, GRID_H, puzzle);
    }

    private static Puzzle _generate(Random rng, int numIslands,
                                    int gridW, int gridH, Puzzle puzzle) {
        boolean[][] occupied = new boolean[gridH][gridW];
        List<Island> islands = new ArrayList<>();

        Island first = new Island(rng.nextInt(gridW), rng.nextInt(gridH));
        islands.add(first);
        occupied[first.row][first.col] = true;

        int attempts = 0;
        while (islands.size() < numIslands && attempts < 800) {
            attempts++;
            Island parent = islands.get(rng.nextInt(islands.size()));

            int[] dcs = {0, 0, -1, 1};
            int[] drs = {-1, 1, 0, 0};
            int dir = rng.nextInt(4);
            int dc = dcs[dir], dr = drs[dir];

            // Higher levels: longer bridges (up to 3 cells gap)
            int dist = 1 + rng.nextInt(numIslands > 7 ? 3 : 2);

            int nc = parent.col + dc * dist;
            int nr = parent.row + dr * dist;

            if (nc < 0 || nc >= gridW || nr < 0 || nr >= gridH) continue;
            if (occupied[nr][nc]) continue;

            boolean blocked = false;
            for (Island other : islands) {
                if (dr == 0 && other.row == nr
                        && other.col > Math.min(parent.col, nc)
                        && other.col < Math.max(parent.col, nc)) { blocked = true; break; }
                if (dc == 0 && other.col == nc
                        && other.row > Math.min(parent.row, nr)
                        && other.row < Math.max(parent.row, nr)) { blocked = true; break; }
            }
            if (blocked) continue;

            Island newIsland = new Island(nc, nr);
            islands.add(newIsland);
            occupied[nr][nc] = true;

            // Randomly assign 1 or 2 bridge count for harder levels
            int bridgeCount = (numIslands > 6 && rng.nextInt(3) == 0) ? 2 : 1;
            puzzle.solution.add(new Bridge(islands.size() - 2, islands.size() - 1, bridgeCount));
        }

        puzzle.islands = islands;
        for (Island i : puzzle.islands) { i.x = i.col; i.y = i.row; }
        for (Bridge b : puzzle.solution) {
            puzzle.islands.get(b.i1).number += b.count;
            puzzle.islands.get(b.i2).number += b.count;
        }

        return puzzle;
    }
}
