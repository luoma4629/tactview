package com.helospark.tactview.core.timeline.effect.distort;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.helospark.tactview.core.CloneRequestMetadata;
import com.helospark.tactview.core.LoadMetadata;
import com.helospark.tactview.core.ReflectionUtil;
import com.helospark.tactview.core.timeline.StatelessEffect;
import com.helospark.tactview.core.timeline.StatelessVideoEffect;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.distort.impl.OpenCVBasedLensDistort;
import com.helospark.tactview.core.timeline.effect.distort.impl.OpenCVLensDistortRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.MultiKeyframeBasedDoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.DoubleProvider;
import com.helospark.tactview.core.timeline.image.ClipImage;
import com.helospark.tactview.core.timeline.image.ReadOnlyClipImage;

public class LensDistortEffect extends StatelessVideoEffect {
    private OpenCVBasedLensDistort openCVBasedLensDistort;

    private DoubleProvider centerXProvider;
    private DoubleProvider centerYProvider;
    private DoubleProvider focalLengthProvider;
    private DoubleProvider k1Provider;
    private DoubleProvider k2Provider;
    private DoubleProvider k3Provider;
    private DoubleProvider p1Provider;
    private DoubleProvider p2Provider;

    public LensDistortEffect(TimelineInterval interval, OpenCVBasedLensDistort implementation) {
        super(interval);
        this.openCVBasedLensDistort = implementation;
    }

    public LensDistortEffect(LensDistortEffect lensDistortEffect, CloneRequestMetadata cloneRequestMetadata) {
        super(lensDistortEffect, cloneRequestMetadata);
        ReflectionUtil.copyOrCloneFieldFromTo(lensDistortEffect, this);
    }

    public LensDistortEffect(JsonNode node, LoadMetadata loadMetadata, OpenCVBasedLensDistort lensDistortImplementation) {
        super(node, loadMetadata);
        this.openCVBasedLensDistort = lensDistortImplementation;
    }

    @Override
    public ReadOnlyClipImage createFrame(StatelessEffectRequest request) {
        ReadOnlyClipImage currentFrame = request.getCurrentFrame();
        ClipImage result = ClipImage.sameSizeAs(currentFrame);
        OpenCVLensDistortRequest nativeRequest = new OpenCVLensDistortRequest();

        double k1 = k1Provider.getValueAt(request.getEffectPosition());
        double k2 = k2Provider.getValueAt(request.getEffectPosition());
        double k3 = k3Provider.getValueAt(request.getEffectPosition());
        double p1 = p1Provider.getValueAt(request.getEffectPosition());
        double p2 = p2Provider.getValueAt(request.getEffectPosition());
        double focalLength = focalLengthProvider.getValueAt(request.getEffectPosition());

        int centerX = (int) (currentFrame.getWidth() * centerXProvider.getValueAt(request.getEffectPosition()));
        int centerY = (int) (currentFrame.getWidth() * centerYProvider.getValueAt(request.getEffectPosition()));

        nativeRequest.input = currentFrame.getBuffer();
        nativeRequest.output = result.getBuffer();
        nativeRequest.width = currentFrame.getWidth();
        nativeRequest.height = currentFrame.getHeight();
        nativeRequest.opticalCenterX = centerX;
        nativeRequest.opticalCenterY = centerY;
        nativeRequest.focalLength = focalLength * currentFrame.getWidth();
        nativeRequest.k1 = k1;
        nativeRequest.k2 = k2;
        nativeRequest.p1 = p1;
        nativeRequest.p2 = p2;
        nativeRequest.k3 = k3;

        openCVBasedLensDistort.lensDistort(nativeRequest);

        return result;
    }

    @Override
    public void initializeValueProvider() {
        focalLengthProvider = new DoubleProvider(-20, 20, new MultiKeyframeBasedDoubleInterpolator(7.0));
        k1Provider = new DoubleProvider(-100, 100, new MultiKeyframeBasedDoubleInterpolator(-11.0));
        k2Provider = new DoubleProvider(-1000, 1000, new MultiKeyframeBasedDoubleInterpolator(0.1));
        k3Provider = new DoubleProvider(-1000, 1000, new MultiKeyframeBasedDoubleInterpolator(0.0));
        p1Provider = new DoubleProvider(-10, 10, new MultiKeyframeBasedDoubleInterpolator(0.0));
        p2Provider = new DoubleProvider(-10, 10, new MultiKeyframeBasedDoubleInterpolator(0.1));
        centerXProvider = new DoubleProvider(-1, 1, new MultiKeyframeBasedDoubleInterpolator(0.5));
        centerYProvider = new DoubleProvider(-1, 1, new MultiKeyframeBasedDoubleInterpolator(0.5));
    }

    @Override
    public List<ValueProviderDescriptor> getValueProviders() {
        ValueProviderDescriptor focalLengthDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(focalLengthProvider)
                .withName("focal length")
                .build();
        ValueProviderDescriptor centerXDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(centerXProvider)
                .withName("centerX")
                .build();
        ValueProviderDescriptor centerYDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(centerYProvider)
                .withName("centerY")
                .build();
        ValueProviderDescriptor k1Descriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(k1Provider)
                .withName("k1")
                .build();
        ValueProviderDescriptor k2Descriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(k2Provider)
                .withName("k2")
                .build();
        ValueProviderDescriptor k3Descriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(k3Provider)
                .withName("k3")
                .build();
        ValueProviderDescriptor p1Descriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(p1Provider)
                .withName("p1")
                .build();
        ValueProviderDescriptor p2Descriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(p2Provider)
                .withName("p2")
                .build();

        return List.of(focalLengthDescriptor, centerXDescriptor, centerYDescriptor, k1Descriptor, k2Descriptor, k3Descriptor, p1Descriptor, p2Descriptor);
    }

    @Override
    public StatelessEffect cloneEffect(CloneRequestMetadata cloneRequestMetadata) {
        return new LensDistortEffect(this, cloneRequestMetadata);
    }

}
