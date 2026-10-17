package com.hyperos.quickopen.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.hyperos.quickopen.R;
import com.hyperos.quickopen.model.ShortcutItem;
import com.hyperos.quickopen.view.IconGlyphFactory;

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
        final ShortcutItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);

        // 显示真实图标（应用图标或内置矢量符号）
        Drawable icon = IconGlyphFactory.resolve(holder.itemView.getContext(), item);
        holder.ivIcon.setImageDrawable(icon);
        holder.ivIcon.setColorFilter(null);

        holder.switchEnable.setOnCheckedChangeListener(null);
        holder.switchEnable.setChecked(item.enabled);
        holder.switchEnable.setOnCheckedChangeListener((btn, isChecked) -> {
            item.enabled = isChecked;
            if (listener != null) listener.onItemChanged();
        });

        // 长按自定义图标来源
        holder.itemView.setOnLongClickListener(v -> {
            showIconEditDialog(v.getContext(), item);
            return true;
        });
    }

    private void showIconEditDialog(Context ctx, ShortcutItem item) {
        View content = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_icon, null);
        EditText etPackage = content.findViewById(R.id.et_icon_package);
        EditText etGlyph = content.findViewById(R.id.et_icon_glyph);
        ImageView ivPreview = content.findViewById(R.id.iv_preview);

        etPackage.setText(item.iconPackage);
        etGlyph.setText(item.iconGlyph);

        Runnable refreshPreview = () -> {
            String pkg = etPackage.getText().toString().trim();
            String glyph = etGlyph.getText().toString().trim();
            Drawable d = null;
            if (!pkg.isEmpty()) d = IconGlyphFactory.appIcon(ctx, pkg);
            if (d == null && !glyph.isEmpty()) {
                d = IconGlyphFactory.createGlyph(glyph, item.tintColor);
            }
            if (d == null) d = IconGlyphFactory.resolve(ctx, item);
            ivPreview.setImageDrawable(d);
        };
        refreshPreview.run();

        etPackage.addTextChangedListener(new SimpleTextWatcher(refreshPreview));
        etGlyph.addTextChangedListener(new SimpleTextWatcher(refreshPreview));

        new AlertDialog.Builder(ctx)
            .setTitle("自定义图标 - " + item.title)
            .setView(content)
            .setPositiveButton("保存", (dlg, which) -> {
                item.iconPackage = etPackage.getText().toString().trim();
                item.iconGlyph = etGlyph.getText().toString().trim();
                notifyDataSetChanged();
                if (listener != null) listener.onItemChanged();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;
        MaterialSwitch switchEnable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            switchEnable = itemView.findViewById(R.id.switch_enable);
        }
    }
}
