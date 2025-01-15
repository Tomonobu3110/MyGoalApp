package com.example.mygoalapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE_FROM_MAIN = 1; // From MainActivity
    public final static int REQUEST_CODE_FROM_ADAPTER = 2; // From GoalAdapter
    private RecyclerView goalRecyclerView;
    private GoalAdapter goalAdapter;
    private List<Goal> goalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goalRecyclerView = findViewById(R.id.goalRecyclerView);
        goalRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // データの読み込み
        loadGoals();

        goalAdapter = new GoalAdapter(goalList, this);
        goalRecyclerView.setAdapter(goalAdapter);

        findViewById(R.id.addGoalButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, GoalEditActivity.class);
            startActivityForResult(intent, REQUEST_CODE_FROM_MAIN); // 1 はリクエストコード
        });
    }

    // 結果を受け取ってリストを更新
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("MainActivity", "requestCode : " + requestCode + " / resultCode : " + resultCode);

        if (requestCode == REQUEST_CODE_FROM_MAIN && resultCode == RESULT_OK) {
            String new_goal_json = data.getStringExtra("goal_json");
            Log.i("MainActivity", new_goal_json);

            // json to object (Goal)
            goalList.add(Goal.fromJson(new_goal_json));
            goalAdapter.notifyDataSetChanged(); // リストビューの更新
        }
        else if (requestCode == REQUEST_CODE_FROM_ADAPTER && resultCode == RESULT_OK) {
            int index = data.getIntExtra("index", -1);
            if (0 <= index) {
                String updated_goal_json = data.getStringExtra("goal_json");
                Log.i("MainActivity", updated_goal_json);

                // json to object (Goal)
                goalList.set(index, Goal.fromJson(updated_goal_json));
                goalAdapter.notifyDataSetChanged();
            }
        }
    }

    private void loadGoals() {
        SharedPreferences prefs = getSharedPreferences("GoalData", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("goals", "[]");
        Type listType = new TypeToken<ArrayList<Goal>>() {}.getType();
        goalList = gson.fromJson(json, listType);
    }

    public void saveGoals() {
        SharedPreferences prefs = getSharedPreferences("GoalData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(goalList);
        editor.putString("goals", json);
        editor.apply();
    }
}
