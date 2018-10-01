package com.helospark.tactview.core.timeline.effect.interpolation.provider;

import java.util.List;
import java.util.stream.Collectors;

import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.interpolation.KeyframeableEffect;
import com.helospark.tactview.core.timeline.effect.interpolation.pojo.Point;
import com.helospark.tactview.core.timeline.effect.interpolation.pojo.Rectangle;

public class RectangleProvider extends KeyframeableEffect {
    private List<PointProvider> pointProviders;

    public RectangleProvider(List<PointProvider> pointProviders) {
        this.pointProviders = pointProviders;
    }

    @Override
    public Object getValueAt(TimelinePosition position) {
        List<Point> points = pointProviders.stream()
                .map(provider -> provider.getValueAt(position))
                .collect(Collectors.toList());
        return new Rectangle(points);
    }

}