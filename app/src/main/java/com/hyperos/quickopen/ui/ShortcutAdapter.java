package com.hyperos.quickopen.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.hyperos.quickopen.R;
import com.hyperos.quickopen.model.ShortcutItem;

import java.util.List;

public class ShortcutAdapter extends RecyclerView.Adapter<ShortcutAdapter.ViewHolder> {

    public interface OnItemChangeListener {
        void onItemChanged();
    }

    private final List<ShortcutItem> items;
    private final OnItemChangeListener listener;

    public ShortcutAdapter(List<ShortcutItem> items, OnItemChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shortcut, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);

        // 设置彩点
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(item.tintColor);
        holder.vColorDot.setBackground(dot);

        holder.switchEnable.setOnCheckedChangeListener(null);
        holder.switchEnable.setChecked(item.enabled);
        holder.switchEnable.setOnCheckedChangeListener((btn, isChecked) -> {
            item.enabled = isChecked;
            if (listener != null) listener.onItemChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View vColorDot;
        TextView tvTitle;
        TextView tvSubtitle;
        MaterialSwitch switchEnable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            vColorDot = itemView.findViewById(R.id.v_color_dot);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            switchEnable = itemView.findViewById(R.id.switch_enable);
        }
    }
}
