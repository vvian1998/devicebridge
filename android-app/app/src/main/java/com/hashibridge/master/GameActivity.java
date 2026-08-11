package com.hashibridge.master;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.hashibridge.master.game.BridgeGameView;
import com.hashibridge.master.game.BridgePuzzleGenerator;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private BridgeGameView gameView;
    private TextView tvHud;
    private int islandCount = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameView = findViewById(R.id.game_view);
        tvHud = findViewById(R.id.tv_hud);
        Button btnNew = findViewById(R.id.btn_new_game);
        Button btnBack = findViewById(R.id.btn_back);

        gameView.setOnWinListener(() -> {
            tvHud.setText("Solved! Great job!");
            Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_LONG).show();
        });

        btnNew.setOnClickListener(v -> newGame());
        btnBack.setOnClickListener(v -> finish());

        newGame();
    }

    private void newGame() {
        BridgePuzzleGenerator.Puzzle puzzle =
                BridgePuzzleGenerator.generate(new Random(), islandCount);
        gameView.setPuzzle(puzzle);
        tvHud.setText("Islands: " + puzzle.islands.size() + " · Tap two islands to build a bridge");
    }
}
