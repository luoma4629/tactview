package com.helospark.tactview.core.timeline.effect.blur;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.helospark.tactview.core.CloneRequestMetadata;
import com.helospark.tactview.core.LoadMetadata;
import com.helospark.tactview.core.ReflectionUtil;
import com.helospark.tactview.core.timeline.StatelessEffect;
import com.helospark.tactview.core.timeline.StatelessVideoEffect;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.blur.service.LinearBlurRequest;
import com.helospark.tactview.core.timeline.effect.blur.service.LinearBlurService;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.pojo.InterpolationLine;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.LineProvider;
import com.helospark.tactview.core.timeline.image.ReadOnlyClipImage;

public class LinearBlurEffect extends StatelessVideoEffect {
    private LinearBlurService linearBlurService;

    private LineProvider directionProvider;

    public LinearBlurEffect(TimelineInterval interval, LinearBlurService linearBlurService) {
        super(interval);
        this.linearBlurService = linearBlurService;
    }

    public LinearBlurEffect(LinearBlurEffect blurEffect, CloneRequestMetadata cloneRequestMetadata) {
        super(blurEffect, cloneRequestMetadata);
        ReflectionUtil.copyOrCloneFieldFromTo(blurEffect, this);
    }

    public LinearBlurEffect(JsonNode node, LoadMetadata loadMetadata, LinearBlurService linearBlurService) {
        super(node, loadMetadata);
        this.linearBlurService = linearBlurService;
    }

    @Override
    public ReadOnlyClipImage createFrame(StatelessEffectRequest request) {
        ReadOnlyClipImage currentFrame = request.getCurrentFrame();

        InterpolationLine direction = directionProvider.getValueAt(request.getEffectPosition()).multiply(currentFrame.getWidth(), currentFrame.getHeight());

        LinearBlurRequest blurRequest = LinearBlurRequest.builder()
                .withDirection(direction)
                .withInput(currentFrame)
                .build();
        return linearBlurService.linearBlur(blurRequest);
    }

    @Override
    public void initializeValueProvider() {
        directionProvider = LineProvider.ofNormalizedScreenCoordinates(0.5, 0.5, 0.55, 0.55);
    }

    @Override
    public List<ValueProviderDescriptor> getValueProviders() {
        ValueProviderDescriptor directionDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(directionProvider)
                .withName("direction")
                .build();

        return Arrays.asList(directionDescriptor);
    }

    @Override
    public StatelessEffect cloneEffect(CloneRequestMetadata cloneRequestMetadata) {
        return new LinearBlurEffect(this, cloneRequestMetadata);
    }

}
