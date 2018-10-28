package com.helospark.tactview.core.timeline.effect.threshold;

import java.util.List;

import com.helospark.tactview.core.timeline.ClipFrameResult;
import com.helospark.tactview.core.timeline.StatelessVideoEffect;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.DoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.IntegerProvider;
import com.helospark.tactview.core.util.IndependentPixelOperation;

public class SimpleThresholdEffect extends StatelessVideoEffect {
    private IndependentPixelOperation independentPixelOperation;

    private IntegerProvider thresholdLimitProvider;

    public SimpleThresholdEffect(TimelineInterval interval, IndependentPixelOperation independentPixelOperation) {
        super(interval);
        this.independentPixelOperation = independentPixelOperation;
    }

    @Override
    public ClipFrameResult createFrame(StatelessEffectRequest request) {
        Integer threshold = thresholdLimitProvider.getValueAt(request.getEffectPosition());
        return independentPixelOperation.createNewImageWithAppliedTransformation(request.getCurrentFrame(), pixelRequest -> {
            int[] input = pixelRequest.input;
            double value = (input[0] + input[1] + input[2]) / 3.0;
            if (value > threshold) {
                pixelRequest.output[0] = 255;
                pixelRequest.output[1] = 255;
                pixelRequest.output[2] = 255;
                pixelRequest.output[3] = input[3];
            } else {
                pixelRequest.output[0] = 0;
                pixelRequest.output[1] = 0;
                pixelRequest.output[2] = 0;
                pixelRequest.output[3] = input[3];
            }
        });
    }

    @Override
    public List<ValueProviderDescriptor> getValueProviders() {
        thresholdLimitProvider = new IntegerProvider(0, 255, new DoubleInterpolator(127.0));

        ValueProviderDescriptor thresholdDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(thresholdLimitProvider)
                .withName("Added constant")
                .build();

        return List.of(thresholdDescriptor);
    }

}
