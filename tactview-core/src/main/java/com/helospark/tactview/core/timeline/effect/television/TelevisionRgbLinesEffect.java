package com.helospark.tactview.core.timeline.effect.television;

import java.util.List;

import com.helospark.tactview.core.timeline.ClipFrameResult;
import com.helospark.tactview.core.timeline.StatelessEffect;
import com.helospark.tactview.core.timeline.StatelessVideoEffect;
import com.helospark.tactview.core.timeline.TimelineInterval;
import com.helospark.tactview.core.timeline.effect.StatelessEffectRequest;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.effect.interpolation.interpolator.MultiKeyframeBasedDoubleInterpolator;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.IntegerProvider;
import com.helospark.tactview.core.util.IndependentPixelOperation;
import com.helospark.tactview.core.util.ReflectionUtil;

public class TelevisionRgbLinesEffect extends StatelessVideoEffect {
    private static final int NUMBER_OF_COLOR_COMPONENTS = 3;
    private IndependentPixelOperation independentPixelOperation;
    private IntegerProvider pixelColumnWidthProvider;

    public TelevisionRgbLinesEffect(TimelineInterval interval, IndependentPixelOperation independentPixelOperation) {
        super(interval);
        this.independentPixelOperation = independentPixelOperation;
    }

    public TelevisionRgbLinesEffect(TelevisionRgbLinesEffect cloneFrom) {
        super(cloneFrom);
        ReflectionUtil.copyOrCloneFieldFromTo(cloneFrom, this);
    }

    @Override
    public ClipFrameResult createFrame(StatelessEffectRequest request) {
        ClipFrameResult currentFrame = request.getCurrentFrame();
        ClipFrameResult result = ClipFrameResult.sameSizeAs(currentFrame);

        int rgbColumnWidth = getRgbColumnWidth(request);

        independentPixelOperation.executePixelTransformation(result.getWidth(), result.getHeight(), (x, y) -> {
            int colorComponentToExtract = (x / rgbColumnWidth) % NUMBER_OF_COLOR_COMPONENTS;
            int color = calculateColorComponent(currentFrame, x, y, colorComponentToExtract, rgbColumnWidth);
            result.setColorComponentByOffset(color, x, y, colorComponentToExtract);
            result.setAlpha(currentFrame.getAlpha(x, y), x, y);
        });
        return result;
    }

    private int calculateColorComponent(ClipFrameResult currentFrame, Integer x, Integer y, int pixelMergeStartIndex, int rgbColumnWidth) {
        int result = 0;
        int startIndex = x - pixelMergeStartIndex;
        int endIndex = Math.min(currentFrame.getWidth(), startIndex + rgbColumnWidth);
        int sumCount = 0;
        for (int i = startIndex; i < endIndex; ++i) {
            result += currentFrame.getColorComponentWithOffset(i, y, pixelMergeStartIndex);
            ++sumCount;
        }
        return result / sumCount;
    }

    private int getRgbColumnWidth(StatelessEffectRequest request) {
        int rgbColumnWidth = (int) (pixelColumnWidthProvider.getValueAt(request.getEffectPosition()) * request.getScale());
        if (rgbColumnWidth < 1) {
            rgbColumnWidth = 1;
        }
        return rgbColumnWidth;
    }

    @Override
    public List<ValueProviderDescriptor> getValueProviders() {
        pixelColumnWidthProvider = new IntegerProvider(1, 20, new MultiKeyframeBasedDoubleInterpolator(5.0));

        ValueProviderDescriptor widthDescriptor = ValueProviderDescriptor.builder()
                .withKeyframeableEffect(pixelColumnWidthProvider)
                .withName("Column width")
                .build();

        return List.of(widthDescriptor);
    }

    @Override
    public StatelessEffect cloneEffect() {
        return new TelevisionRgbLinesEffect(this);
    }

}