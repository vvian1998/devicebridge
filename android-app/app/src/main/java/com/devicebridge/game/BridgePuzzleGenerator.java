package com.devicebridge.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BridgePuzzleGenerator {

    public static final int GRID_W = 5;
    public static final int GRID_H = 5;

    public static class Island {
        public int col;
        public int row;
        public int number;
        public int x;
        public int y;

        public Island(int col, int row) {
            this.col = col;
            this.row = row;
        }
    }

    public static class Bridge {
        public int i1;
        public int i2;
        public int count;

        public Bridge(int i1, int i2, int count) {
            this.i1 = i1;
            this.i2 = i2;
            this.count = count;
        }
    }

    public static class Puzzle {
        public List<Island> islands = new ArrayList<>();
        public List<Bridge> solution = new ArrayList<>();

        public void reset() {
            for (Island i : islands) {
                i.number = 0;
                i.x = i.col;
                i.y = i.row;
            }
            solution.clear();
        }
    }

    public static Puzzle generate(Random rng, int numIslands) {
        Puzzle puzzle = new Puzzle();

        boolean[][] occupied = new boolean[GRID_H][GRID_W];

        List<Island> islands = new ArrayList<>();

        int c0 = rng.nextInt(GRID_W);
        int r0 = rng.nextInt(GRID_H);
        Island first = new Island(c0, r0);
        islands.add(first);
        occupied[r0][c0] = true;

        int attempts = 0;
        while (islands.size() < numIslands && attempts < 500) {
            attempts++;
            int parentIdx = rng.nextInt(islands.size());
            Island parent = islands.get(parentIdx);

            int dir = rng.nextInt(4);
            int dc = 0, dr = 0;
            switch (dir) {
                case 0: dr = -1; break;
                case 1: dr = 1; break;
                case 2: dc = -1; break;
                case 3: dc = 1; break;
            }

            int dist = 1 + rng.nextInt(2);

            int nc = parent.col + dc * dist;
            int nr = parent.row + dr * dist;

            if (nc < 0 || nc >= GRID_W || nr < 0 || nr >= GRID_H) continue;
            if (occupied[nr][nc]) continue;

            boolean blocked = false;
            for (Island other : islands) {
                if (other.row == nr && other.col > Math.min(parent.col, nc) && other.col < Math.max(parent.col, nc)) {
                    blocked = true;
                    break;
                }
                if (other.col == nc && other.row > Math.min(parent.row, nr) && other.row < Math.max(parent.row, nr)) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) continue;

            Island newIsland = new Island(nc, nr);
            islands.add(newIsland);
            occupied[nr][nc] = true;

            puzzle.solution.add(new Bridge(parentIdx, islands.size() - 1, 1));
        }

        puzzle.islands = islands;

        for (Island i : puzzle.islands) {
            i.number = 0;
            i.x = i.col;
            i.y = i.row;
        }

        for (Bridge b : puzzle.solution) {
            puzzle.islands.get(b.i1).number++;
            puzzle.islands.get(b.i2).number++;
        }

        return puzzle;
    }
}
