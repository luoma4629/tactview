package com.helospark.tactview.core.timeline.message;

import java.util.List;

import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.util.messaging.AffectedModifiedIntervalAware;

public class KeyframeSuccesfullyAddedMessage implements AffectedModifiedIntervalAware {
    private String descriptorId;
    private TimelineInterval interval;
    private String containingElementId;

    public KeyframeSuccesfullyAddedMessage(String descriptorId, TimelineInterval globalInterval, String containingElementId) {
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

    @Override
    public String toString() {
        return "KeyframeSuccesfullyAddedMessage [descriptorId=" + descriptorId + ", interval=" + interval + ", containingElementId=" + containingElementId + "]";
    }

}
