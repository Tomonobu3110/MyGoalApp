package com.example.mygoalapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {
    private List<Goal> goalList;
    private Context context;

    public GoalAdapter(List<Goal> goalList, Context context) {
        this.goalList = goalList;
        this.context = context;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.goal_item, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goalList.get(position);
        holder.goalTextView.setText(goal.goal);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GoalEditActivity.class);
            intent.putExtra("index", position);
            Gson gson = new Gson();
            String goal_json = gson.toJson(goal);
            intent.putExtra("goal_json", goal_json);
            ((Activity)context).startActivityForResult(intent, MainActivity.REQUEST_CODE_FROM_ADAPTER);
        });
    }

    @Override
    public int getItemCount() {
        return goalList.size();
    }

    public static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView goalTextView;
        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            goalTextView = itemView.findViewById(R.id.goalTextView);
        }
    }
}

