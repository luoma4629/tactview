package com.helospark.tactview.core.timeline.proceduralclip;

import java.nio.ByteBuffer;

import com.helospark.tactview.core.decoder.VisualMediaMetadata;
import com.helospark.tactview.core.timeline.ClipFrameResult;
import com.helospark.tactview.core.timeline.GetFrameRequest;
import com.helospark.tactview.core.timeline.TimelineClipType;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.VisualTimelineClip;

public abstract class ProceduralVisualClip extends VisualTimelineClip {

    public ProceduralVisualClip(VisualMediaMetadata visualMediaMetadata, TimelineInterval interval) {
        super(visualMediaMetadata, interval, TimelineClipType.IMAGE);
    }

    public ProceduralVisualClip(ProceduralVisualClip proceduralVisualClip) {
        super(proceduralVisualClip);
    }

    @Override
    public ByteBuffer requestFrame(TimelinePosition position, int width, int height) {
        // TODO something is very wrong here
        throw new IllegalStateException();
    }

    public abstract ClipFrameResult createProceduralFrame(GetFrameRequest request, TimelinePosition relativePosition);

    @Override
    public ClipFrameResult getFrameInternal(GetFrameRequest request) {
        TimelinePosition relativePosition = request.calculateRelativePositionFrom(this);
        ClipFrameResult result = createProceduralFrame(request, relativePosition);
        return applyEffects(relativePosition, result, request);
    }

    @Override
    public VisualMediaMetadata getMediaMetadata() {
        return mediaMetadata;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

}