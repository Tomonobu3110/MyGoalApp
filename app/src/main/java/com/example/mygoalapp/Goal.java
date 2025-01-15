package com.example.mygoalapp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Goal {
    public String goal;
    public List<String> purposes;
    public List<String> actions;
    public List<String> higherPurposes;

    public Goal() {
        this.goal = new String();
        this.purposes = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.higherPurposes = new ArrayList<>();
    }

    public String getJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Goal fromJson(String json) {
            Gson gson = new Gson();
            Type goalType = new TypeToken<Goal>() {}.getType();
            return gson.fromJson(json, goalType);
    }
}
