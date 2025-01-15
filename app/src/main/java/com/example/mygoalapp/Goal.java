package com.example.mygoalapp;

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
}
