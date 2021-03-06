package com.helospark.tactview.core.timeline.message;

import java.util.List;

import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.util.messaging.AffectedModifiedIntervalAware;

public class KeyframeSuccesfullyRemovedMessage implements AffectedModifiedIntervalAware {
    private String descriptorId;
    private TimelineInterval interval;
    private String containingElementId;

    public KeyframeSuccesfullyRemovedMessage(String descriptorId, TimelineInterval globalInterval, String containingElementId) {
        this.descriptorId = descriptorId;
        this.interval = globalInterval;
        this.containingElementId = containingElementId;
    }

    public String getDescriptorId() {
        return descriptorId;
    }

    public TimelineInterval getInterval() {
        return interval;
    }

    public String getContainingElementId() {
        return containingElementId;
    }

    @Override
    public List<TimelineInterval> getAffectedIntervals() {
        return List.of(interval);
    }

}
