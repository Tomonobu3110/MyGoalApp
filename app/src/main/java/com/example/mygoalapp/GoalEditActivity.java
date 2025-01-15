package com.example.mygoalapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GoalEditActivity extends AppCompatActivity {
    private EditText goalEditText;
    private LinearLayout purposesLayout, actionsLayout, higherPurposesLayout;

    private String mGoalJson;
    private int mGoalIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_edit);

        goalEditText = findViewById(R.id.goalEditText);
        purposesLayout = findViewById(R.id.purposesLayout);
        actionsLayout = findViewById(R.id.actionsLayout);
        higherPurposesLayout = findViewById(R.id.higherPurposesLayout);

        // intent
        Intent intent = getIntent();
        mGoalJson  = intent.getStringExtra("goal_json"); // null when goal_json is not set.
        mGoalIndex = intent.getIntExtra("index", -1);

        if (0 <= mGoalIndex && null != mGoalJson) {
            Gson gson = new Gson();
            Type goalType = new TypeToken<Goal>() {}.getType();
            Goal goal = gson.fromJson(mGoalJson, goalType);
            goalEditText.setText(goal.goal);
            for (String action : goal.actions) {
                EditText newAction = new EditText(this);
                newAction.setText(action);
                actionsLayout.addView(newAction);
            }
            for (String purpose : goal.purposes) {
                EditText newPurpose = new EditText(this);
                newPurpose.setText(purpose);
                purposesLayout.addView(newPurpose);
            }
            for (String high_purpose : goal.higherPurposes) {
                EditText newHigherPurpose = new EditText(this);
                newHigherPurpose.setText(high_purpose);
                higherPurposesLayout.addView(newHigherPurpose);
            }
        }
    }

    public void addPurpose(View view) {
        EditText newPurpose = new EditText(this);
        purposesLayout.addView(newPurpose);
        newPurpose.requestFocus();
    }

    public void addAction(View view) {
        EditText newAction = new EditText(this);
        actionsLayout.addView(newAction);
        newAction.requestFocus();
    }

    public void addHigherPurpose(View view) {
        EditText newHigherPurpose = new EditText(this);
        higherPurposesLayout.addView(newHigherPurpose);
        newHigherPurpose.requestFocus();
    }

    public void saveGoal(View view) {
        // 入力結果をクラス化
        Goal newGoal = new Goal();
        newGoal.goal = goalEditText.getText().toString();
        newGoal.purposes = extractTextFromLayout(purposesLayout);
        newGoal.actions = extractTextFromLayout(actionsLayout);
        newGoal.higherPurposes = extractTextFromLayout(higherPurposesLayout);

        // 保存処理
        SharedPreferences prefs = getSharedPreferences("GoalData", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("goals", "[]");
        Type listType = new TypeToken<ArrayList<Goal>>() {}.getType();
        List<Goal> goalList = gson.fromJson(json, listType);

        // update or append the goal
        if (0 <= mGoalIndex) {
            goalList.set(mGoalIndex, newGoal);
        } else {
            goalList.add(newGoal);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("goals", gson.toJson(goalList));
        editor.apply();

        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();

        // MainActivityにnewGoalをjson文字列にして返す
        Intent resultIntent = new Intent();
        resultIntent.putExtra("index", mGoalIndex);
        resultIntent.putExtra("goal_json", gson.toJson(newGoal));
        setResult(RESULT_OK, resultIntent);
        finish();  // 画面を閉じる
    }

    private List<String> extractTextFromLayout(LinearLayout layout) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < layout.getChildCount(); i++) {
            EditText editText = (EditText) layout.getChildAt(i);
            result.add(editText.getText().toString());
        }
        return result;
    }
}
