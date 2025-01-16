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

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public final static int REQUEST_CODE_FROM_MAIN = 1; // From MainActivity
    public final static int REQUEST_CODE_FROM_ADAPTER = 2; // From GoalAdapter
    public final static int REQUEST_CODE_SAVE_JSON = 1001; // For Google Drive (Save)
    public final static int REQUEST_CODE_LOAD_JSON = 1002; // For Google Drive (Load)

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

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            signIn(REQUEST_CODE_SAVE_JSON);
        });

        findViewById(R.id.loadButton).setOnClickListener(v -> {
            signIn(REQUEST_CODE_LOAD_JSON);
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

        // for Google Drive (Save & Load)
        else if (requestCode == REQUEST_CODE_SAVE_JSON || requestCode == REQUEST_CODE_LOAD_JSON)  {
            Log.i("MainActivity", "Sign In Result");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.i("MainActivity", "login : " + account.getDisplayName());
                    Log.i("MainActivity", "email : " + account.getEmail());
                    switch (requestCode) {
                        case REQUEST_CODE_SAVE_JSON:
                            saveJsonToGoogleDrive(account);
                            break;
                        case REQUEST_CODE_LOAD_JSON:
                            loadLatestJsonFromGoogleDrive(account);
                            break;
                    }
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

    public void signIn(int requestCode) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(googleSignInClient.getSignInIntent(), requestCode);
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
                    // フォルダが存在するかチェック
                    String folderId = null;
                    FileList result = driveService.files().list()
                            .setQ("name = 'MyGoalApplicationData' and mimeType = 'application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute();

                    if (!result.getFiles().isEmpty()) {
                        folderId = result.getFiles().get(0).getId();
                    } else {
                        // フォルダを作成
                        File folderMetadata = new File();
                        folderMetadata.setName("MyGoalApplicationData");
                        folderMetadata.setMimeType("application/vnd.google-apps.folder");

                        File folder = driveService.files().create(folderMetadata)
                                .setFields("id")
                                .execute();
                        folderId = folder.getId();
                    }

                    // 日時付きファイル名の生成
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String timestamp = sdf.format(new Date());
                    String fileName = "my_goal_application_data_" + timestamp + ".json";

                    // ファイルをフォルダ内に作成
                    File fileMetadata = new File();
                    fileMetadata.setName(fileName);
                    fileMetadata.setParents(Collections.singletonList(folderId));

                    ByteArrayContent content = new ByteArrayContent("application/json", json.getBytes());

                    driveService.files().create(fileMetadata, content)
                            .setFields("id")
                            .execute();

                    // メインスレッドでUIの更新
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "JSON saved to Google Drive", Toast.LENGTH_SHORT).show()
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    // メインスレッドでエラー表示
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "Error: JSON save", Toast.LENGTH_SHORT).show()
                    ); // end of runOnUiThread()
                }
            } // end of run()
        }); // end of executor.execute()
    } // end of function of saveJsonToGoogleDrive()

    public void loadLatestJsonFromGoogleDrive(GoogleSignInAccount account) {
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

                try {
                    // フォルダIDの取得
                    String folderId = null;
                    FileList folderResult = driveService.files().list()
                            .setQ("name = 'MyGoalApplicationData' and mimeType = 'application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("files(id)")
                            .execute();

                    if (!folderResult.getFiles().isEmpty()) {
                        folderId = folderResult.getFiles().get(0).getId();
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(getApplicationContext(), "Folder not found", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // フォルダ内のファイル一覧取得 (my_goal_application_data_*.json)
                    FileList fileResult = driveService.files().list()
                            .setQ("'" + folderId + "' in parents and name contains 'my_goal_application_data_' and name contains '.json'")
                            .setSpaces("drive")
                            .setFields("files(id, name, createdTime)")
                            .setOrderBy("createdTime desc") // 最新順にソート
                            .execute();

                    if (fileResult.getFiles().isEmpty()) {
                        runOnUiThread(() ->
                                Toast.makeText(getApplicationContext(), "No JSON file found", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }

                    // 最新ファイルの取得
                    File latestFile = fileResult.getFiles().get(0);
                    String fileId = latestFile.getId();

                    // ファイルのコンテンツ取得
                    InputStream inputStream = driveService.files().get(fileId).executeMediaAsInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    String json = jsonBuilder.toString();

                    // JSONパースとデータの読み込み
                    Gson gson = new Gson();
                    List<Goal> loadedGoalList = gson.fromJson(json, new TypeToken<List<Goal>>() {}.getType());

                    // メインスレッドでUIを更新
                    runOnUiThread(() -> {
                        goalList.clear();
                        goalList.addAll(loadedGoalList);
                        saveGoals();
                        goalAdapter.notifyDataSetChanged();
                        Toast.makeText(getApplicationContext(), "Latest JSON loaded", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(getApplicationContext(), "Error loading JSON", Toast.LENGTH_SHORT).show()
                    ); // end of runOnUiThread()
                }
            } // end of run()
        }); // end of executor.execute()
    } // end of function of loadLatestJsonFromGoogleDrive()
}
