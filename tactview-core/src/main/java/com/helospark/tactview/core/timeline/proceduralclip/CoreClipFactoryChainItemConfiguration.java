package com.helospark.tactview.core.timeline.proceduralclip;

import com.helospark.lightdi.annotation.Bean;
import com.helospark.lightdi.annotation.Configuration;
import com.helospark.tactview.core.decoder.ImageMetadata;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.TimelineLength;
import com.helospark.tactview.core.timeline.blendmode.impl.NormalBlendModeStrategy;
import com.helospark.tactview.core.timeline.framemerge.AlphaBlitService;
import com.helospark.tactview.core.timeline.proceduralclip.gradient.LinearGradientProceduralEffect;
import com.helospark.tactview.core.timeline.proceduralclip.gradient.RadialGradientProceduralEffect;
import com.helospark.tactview.core.timeline.proceduralclip.highlight.DrawnHighlightProceduralEffect;
import com.helospark.tactview.core.timeline.proceduralclip.noise.GaussianNoiseProceduralClip;
import com.helospark.tactview.core.timeline.proceduralclip.singlecolor.SingleColorProceduralClip;
import com.helospark.tactview.core.timeline.proceduralclip.text.TextProceduralClip;
import com.helospark.tactview.core.util.BresenhemPixelProvider;
import com.helospark.tactview.core.util.BufferedImageToClipFrameResultConverter;
import com.helospark.tactview.core.util.IndependentPixelOperation;
import com.helospark.tactview.core.util.brush.ScaledBrushProvider;

@Configuration
public class CoreClipFactoryChainItemConfiguration {

    @Bean
    public StandardProceduralClipFactoryChainItem singleColorProceduralClip(IndependentPixelOperation independentPixelOperation) {
        return new StandardProceduralClipFactoryChainItem("singlecolor", "Single color",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new SingleColorProceduralClip(metadata, new TimelineInterval(request.getPosition(), defaultLength), independentPixelOperation);
                });
    }

    @Bean
    public StandardProceduralClipFactoryChainItem textProceduralClip(BufferedImageToClipFrameResultConverter bufferedImageToClipFrameResultConverter) {
        return new StandardProceduralClipFactoryChainItem("text", "Text",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new TextProceduralClip(metadata, new TimelineInterval(request.getPosition(), defaultLength), bufferedImageToClipFrameResultConverter);
                });
    }

    @Bean
    public StandardProceduralClipFactoryChainItem gradientProceduralClip(IndependentPixelOperation independentPixelOperation) {
        return new StandardProceduralClipFactoryChainItem("radialgradient", "Radial gradient",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new RadialGradientProceduralEffect(metadata, new TimelineInterval(request.getPosition(), defaultLength), independentPixelOperation);
                });
    }

    @Bean
    public StandardProceduralClipFactoryChainItem linearProceduralClip(IndependentPixelOperation independentPixelOperation) {
        return new StandardProceduralClipFactoryChainItem("lineargradient", "Linear gradient",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new LinearGradientProceduralEffect(metadata, new TimelineInterval(request.getPosition(), defaultLength), independentPixelOperation);
                });
    }

    @Bean
    public StandardProceduralClipFactoryChainItem gaussianNoiseProceduralClip(IndependentPixelOperation independentPixelOperation) {
        return new StandardProceduralClipFactoryChainItem("gaussianNoise", "Gaussian noise",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new GaussianNoiseProceduralClip(metadata, new TimelineInterval(request.getPosition(), defaultLength), independentPixelOperation);
                });
    }

    @Bean
    public StandardProceduralClipFactoryChainItem drawnHighlightProceduralEffect(IndependentPixelOperation independentPixelOperation,
            AlphaBlitService alphaBlitService, NormalBlendModeStrategy normalBlendModeStrategy, ScaledBrushProvider scaledBrushProvider, BresenhemPixelProvider bresenhemPixelProvider) {
        return new StandardProceduralClipFactoryChainItem("drawnhighlight", "Drawn highlight",
                request -> {
                    TimelineLength defaultLength = TimelineLength.ofMillis(30000);
                    ImageMetadata metadata = ImageMetadata.builder()
                            .withWidth(1920)
                            .withHeight(1080)
                            .withLength(defaultLength)
                            .build();
                    return new DrawnHighlightProceduralEffect(metadata, new TimelineInterval(request.getPosition(), defaultLength), scaledBrushProvider, normalBlendModeStrategy, alphaBlitService,
                            bresenhemPixelProvider);
                });
    }
}
