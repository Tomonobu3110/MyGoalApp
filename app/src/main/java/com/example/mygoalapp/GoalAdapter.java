package com.example.mygoalapp;

import android.app.Activity;
import android.app.AlertDialog;
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

        // event handler for normal tap (edit)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GoalEditActivity.class);
            intent.putExtra("index", position);
            intent.putExtra("goal_json", goal.getJson());
            ((Activity)context).startActivityForResult(intent, MainActivity.REQUEST_CODE_FROM_ADAPTER);
        });

        // event handler for long tap (remove)
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("削除確認")
                    .setMessage("この目標を削除しますか？")
                    .setPositiveButton("YES", (dialog, which) -> {
                        // リストから削除し、UIを更新
                        goalList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, goalList.size());
                        ((MainActivity)context).saveGoals();
                    })
                    .setNegativeButton("NO", null)
                    .show();
            return true; // 長押しの消費を示すため true を返す
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

