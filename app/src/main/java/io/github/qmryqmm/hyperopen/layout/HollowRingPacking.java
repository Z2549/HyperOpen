package io.github.qmryqmm.hyperopen.layout;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 空心同心圆环布局：中心保持空心，图标排布在 1 ~ ringCount 圈同心环上。
 * 与 {@link CircularHoneycombPacking} 共享同一套间距语义（iconSpacingOffset 紧凑程度通用生效），
 * 返回的点集同样按「距圆心欧氏距离升序」排列，保证展开动画节奏一致。
 */
public class HollowRingPacking {

    public static class PackPoint {
        public PointF point;
        public float distFromCenter;

        public PackPoint(float x, float y, float dist) {
            this.point = new PointF(x, y);
            this.distFromCenter = dist;
        }
    }

    /**
     * 在给定圆形区域 (center, regionRadius) 内生成空心同心圆环坐标。
     *
     * @param count         需要的图标数量
     * @param centerX       圆环中心 X (通常与指纹中心对齐)
     * @param centerY       圆环中心 Y (指纹中心 - offsetY)
     * @param regionRadius  圆形区域半径 (最外环由它收边)
     * @param iconSize      单个图标的基础直径
     * @param spacingOffset 紧凑程度偏移量 (与蜂窝布局同一语义)
     * @param ringCount     同心环圈数 (1 ~ 4，中心保持空心)
     * @return 排序好的坐标列表 (由内环向外环扩散，空心无中心点)
     */
    public static List<PointF> calculatePoints(int count, float centerX, float centerY,
                                               float regionRadius, float iconSize,
                                               float spacingOffset, int ringCount) {
        List<PointF> result = new ArrayList<>();
        if (count <= 0) return result;

        // 间距语义与蜂窝完全一致：图标大小 + 基础边距 + 紧凑度微调（保底防重叠）
        float spacing = Math.max(iconSize * 0.75f, iconSize + 12f + spacingOffset);
        // 最外环图标中心半径：与蜂窝同款 +8 容差收边
        float rOut = regionRadius + 8f - iconSize / 2f;
        // 空心下限：内环不得逼近圆心，保证中心恒空
        float minRingRadius = iconSize * 0.9f;
        if (rOut < minRingRadius) rOut = minRingRadius;

        int wantRings = Math.max(1, Math.min(ringCount, 4));

        // 先按标准角距装填；容量不足时角距压缩到 0.85x 重算；仍不足则截断（与 maxIconCount 语义一致）
        List<PackPoint> placed = tryPlace(count, centerX, centerY, spacing, rOut, minRingRadius, wantRings);
        if (placed.size() < count) {
            placed = tryPlace(count, centerX, centerY, spacing * 0.85f, rOut, minRingRadius, wantRings);
        }

        // 由内环向外环排序，展开动画节奏与蜂窝一致
        Collections.sort(placed, new Comparator<PackPoint>() {
            @Override
            public int compare(PackPoint o1, PackPoint o2) {
                return Float.compare(o1.distFromCenter, o2.distFromCenter);
            }
        });
        for (PackPoint p : placed) result.add(p.point);
        return result;
    }

    /** 单环均分：点位落在半径 r 的环上，起始角 -90°（正上方），环间错开半步防径向对齐 */
    private static List<PackPoint> tryPlace(int count, float cx, float cy,
                                            float spacing, float rOut, float minRingRadius,
                                            int wantRings) {
        List<PackPoint> out = new ArrayList<>();

        // 由外向内收集可行环半径
        List<Float> radii = new ArrayList<>();
        float r = rOut;
        while (radii.size() < wantRings && r >= minRingRadius) {
            radii.add(r);
            r -= spacing; // 环间径向净距 = 间距（相邻环图标边缘近贴）
        }
        if (radii.isEmpty()) radii.add(Math.max(rOut, minRingRadius));

        // 每环容量：弦长 >= spacing 的均分上限
        int rings = radii.size();
        int[] caps = new int[rings];
        int totalCap = 0;
        for (int i = 0; i < rings; i++) {
            float R = radii.get(i);
            float ratio = spacing / (2f * R);
            int cap;
            if (ratio >= 1f) {
                cap = 1;
            } else {
                cap = (int) Math.floor(Math.PI / Math.asin(ratio));
                if (cap < 1) cap = 1;
            }
            caps[i] = cap;
            totalCap += cap;
        }
        if (totalCap <= 0) return out;

        // 按容量比例分配数量（最大余数法，外环优先 +1 补余数）
        int target = Math.min(count, totalCap);
        int[] perRing = new int[rings];
        float[] frac = new float[rings];
        int assigned = 0;
        for (int i = 0; i < rings; i++) {
            float share = (float) target * caps[i] / totalCap;
            perRing[i] = (int) Math.floor(share);
            frac[i] = share - perRing[i];
            assigned += perRing[i];
        }
        int remain = target - assigned;
        while (remain > 0) {
            int best = -1;
            float bestFrac = -1f;
            for (int i = 0; i < rings; i++) {
                if (perRing[i] >= caps[i]) continue;
                // 余数相同时外环优先
                if (frac[i] > bestFrac + 0.0001f) {
                    bestFrac = frac[i];
                    best = i;
                }
            }
            if (best < 0) break;
            perRing[best]++;
            frac[best] = -1f;
            remain--;
        }

        // 每环等角分布；奇环错开半步，避免相邻环图标径向对齐
        for (int i = 0; i < rings; i++) {
            int n = perRing[i];
            if (n <= 0) continue;
            float ringR = radii.get(i);
            float start = (float) (-Math.PI / 2d);
            if (n > 1 && (i % 2) != 0) start += (float) Math.PI / n;
            for (int k = 0; k < n; k++) {
                double ang = start + k * 2d * Math.PI / n;
                float x = cx + ringR * (float) Math.cos(ang);
                float y = cy + ringR * (float) Math.sin(ang);
                out.add(new PackPoint(x, y, ringR));
            }
        }

        // 空心单点特例：仅 1 个图标时落在最外环正上方（保持"空心"语义，不回圆心）
        if (count == 1 && out.isEmpty()) {
            float ringR = radii.get(0);
            out.add(new PackPoint(cx, cy - ringR, ringR));
        }
        return out;
    }
}
