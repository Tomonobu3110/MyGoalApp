package com.example.mygoalapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.DriveScopes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE_FROM_MAIN = 1; // From MainActivity
    public final static int REQUEST_CODE_FROM_ADAPTER = 2; // From GoalAdapter
    public final static int REQUEST_CODE_GOOGLE_DRIVE = 1001; // For Google Drive (Auth)
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

        findViewById(R.id.signInButton).setOnClickListener(v -> {
            signIn();
        });
    }

    // 結果を受け取ってリストを更新
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("MainActivity", "requestCode : " + requestCode + " / resultCode : " + resultCode);

        // for new item
        if (requestCode == REQUEST_CODE_FROM_MAIN && resultCode == RESULT_OK) {
            String new_goal_json = data.getStringExtra("goal_json");
            Log.i("MainActivity", new_goal_json);

            // json to object (Goal)
            goalList.add(Goal.fromJson(new_goal_json));
            goalAdapter.notifyDataSetChanged(); // リストビューの更新
        }

        // for item update
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

        // for Google Drive
        else if (requestCode == REQUEST_CODE_GOOGLE_DRIVE) {
            Log.i("MainActivity", "Sign In Result");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.i("MainActivity", "login : " + account.getDisplayName());
                    Log.i("MainActivity", "email : " + account.getEmail());
                    saveJsonToGoogleDrive(account);
                } else {
                    Toast.makeText(this, "error : account is null", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "error : Google Sign In", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", e.toString());
                Log.e("MainActivity", e.getMessage());
                e.printStackTrace();
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

    public void signIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_GOOGLE_DRIVE);
    }

    public void saveJsonToGoogleDrive(GoogleSignInAccount account) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(account.getAccount());
                com.google.api.services.drive.Drive driveService =
                        new com.google.api.services.drive.Drive.Builder(
                                new NetHttpTransport(),
                                new JacksonFactory(),
                                credential)
                                .setApplicationName("My Goal Application")
                                .build();

                Gson gson = new Gson();
                String json = gson.toJson(goalList);

                try {
                    File fileMetadata = new File();
                    fileMetadata.setName("my_goal_application_data.json");

                    ByteArrayContent content = new ByteArrayContent("application/json", json.getBytes());

                    driveService.files().create(fileMetadata, content)
                            .setFields("id")
                            .execute();

                    // メインスレッドでUIの更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "JSON saved to Google Drive", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();

                    // メインスレッドでUIの更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "error : JSON save", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}
