package com.helospark.tactview.core.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.decoder.framecache.GlobalMemoryManagerAccessor;
import com.helospark.tactview.core.optionprovider.OptionProvider;
import com.helospark.tactview.core.render.ffmpeg.CodecExtraDataRequest;
import com.helospark.tactview.core.render.ffmpeg.CodecInformation;
import com.helospark.tactview.core.render.ffmpeg.FFmpegBasedMediaEncoder;
import com.helospark.tactview.core.render.ffmpeg.FFmpegClearEncoderRequest;
import com.helospark.tactview.core.render.ffmpeg.FFmpegEncodeFrameRequest;
import com.helospark.tactview.core.render.ffmpeg.FFmpegInitEncoderRequest;
import com.helospark.tactview.core.render.ffmpeg.QueryCodecRequest;
import com.helospark.tactview.core.render.ffmpeg.RenderFFMpegFrame;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.AudioFrameResult;
import com.helospark.tactview.core.timeline.AudioVideoFragment;
import com.helospark.tactview.core.timeline.TimelineManagerAccessor;
import com.helospark.tactview.core.timeline.TimelineManagerRenderService;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.interpolation.provider.ValueListElement;
import com.helospark.tactview.core.timeline.effect.scale.service.ScaleService;
import com.helospark.tactview.core.timeline.message.progress.ProgressAdvancedMessage;
import com.helospark.tactview.core.util.messaging.MessagingService;

@Component
public class FFmpegBasedRenderService extends AbstractRenderService {
    private static final String DEFAULT_VALUE = "default";
    private static final int MAX_NUMBER_OF_CODECS = 400;
    private FFmpegBasedMediaEncoder ffmpegBasedMediaEncoder;
    private TimelineManagerAccessor timelineManagerAccessor;

    public FFmpegBasedRenderService(TimelineManagerRenderService timelineManager, FFmpegBasedMediaEncoder ffmpegBasedMediaEncoder, MessagingService messagingService,
            ScaleService scaleService, TimelineManagerAccessor timelineManagerAccessor, ProjectRepository projectRepository) {
        super(timelineManager, messagingService, scaleService, projectRepository);
        this.ffmpegBasedMediaEncoder = ffmpegBasedMediaEncoder;
        this.timelineManagerAccessor = timelineManagerAccessor;
    }

    @Override
    public void renderInternal(RenderRequest renderRequest) {
        TimelinePosition currentPosition = renderRequest.getStartPosition();

        int videoBitRate = (int) renderRequest.getOptions().get("videobitrate").getValue();

        System.out.println("Video BitRate: " + videoBitRate);

        int audioBitRate = (int) renderRequest.getOptions().get("audiobitrate").getValue();
        System.out.println("Audio BitRate: " + audioBitRate);

        int audioSampleRate = (int) renderRequest.getOptions().get("audiosamplerate").getValue();
        System.out.println("Audio SampleRate: " + audioSampleRate);

        int bytesPerSample = Integer.parseInt(renderRequest.getOptions().get("audiobytespersample").getValue().toString());
        System.out.println("Audio bytes per sample: " + bytesPerSample);

        String videoCodec = (String) renderRequest.getOptions().get("videocodec").getValue();
        System.out.println("VideoCodec: " + videoCodec);

        String audioCodec = (String) renderRequest.getOptions().get("audiocodec").getValue();
        System.out.println("AudioCodec: " + audioCodec);

        String videoPixelFormat = (String) renderRequest.getOptions().get("videoPixelFormat").getValue();
        System.out.println("videoPixelFormat: " + videoPixelFormat);

        FFmpegInitEncoderRequest initNativeRequest = new FFmpegInitEncoderRequest();
        initNativeRequest.fileName = renderRequest.getFileName();
        initNativeRequest.fps = renderRequest.getFps();
        int width = renderRequest.getWidth();
        if (width % 2 == 1) {
            width++;
        }
        int height = renderRequest.getHeight();
        if (height % 2 == 1) {
            height++;
        }
        initNativeRequest.renderWidth = width;
        initNativeRequest.renderHeight = height;

        AudioVideoFragment tmpFrame = queryFrameAt(renderRequest, currentPosition, Optional.empty(), Optional.empty());//tmp solution

        initNativeRequest.actualWidth = width;
        initNativeRequest.actualHeight = height;
        initNativeRequest.bytesPerSample = bytesPerSample;
        initNativeRequest.audioChannels = tmpFrame.getAudioResult().getChannels().size();
        initNativeRequest.sampleRate = audioSampleRate;

        initNativeRequest.audioBitRate = audioBitRate;
        initNativeRequest.videoBitRate = videoBitRate;
        initNativeRequest.audioSampleRate = audioSampleRate;
        initNativeRequest.videoCodec = videoCodec;
        initNativeRequest.audioCodec = audioCodec;
        initNativeRequest.videoPixelFormat = videoPixelFormat;
        // frame not freed

        int encoderIndex = ffmpegBasedMediaEncoder.initEncoder(initNativeRequest);

        int frameIndex = 0;
        while (currentPosition.isLessOrEqualToThan(renderRequest.getEndPosition()) && !renderRequest.getIsCancelledSupplier().get()) {
            AudioVideoFragment frame = queryFrameAt(renderRequest, currentPosition, Optional.of(audioSampleRate), Optional.of(bytesPerSample));

            FFmpegEncodeFrameRequest nativeRequest = new FFmpegEncodeFrameRequest();
            nativeRequest.frame = new RenderFFMpegFrame();
            RenderFFMpegFrame[] array = (RenderFFMpegFrame[]) nativeRequest.frame.toArray(1);
            array[0].imageData = frame.getVideoResult().getBuffer();
            if (frame.getAudioResult().getChannels().size() > 0) {
                array[0].audioData = convertAudio(frame.getAudioResult());
                array[0].numberOfAudioSamples = frame.getAudioResult().getNumberSamples();
            }
            nativeRequest.encoderIndex = encoderIndex;
            nativeRequest.startFrameIndex = frameIndex;

            ffmpegBasedMediaEncoder.encodeFrames(nativeRequest);

            GlobalMemoryManagerAccessor.memoryManager.returnBuffer(frame.getVideoResult().getBuffer());
            for (var buffer : frame.getAudioResult().getChannels()) {
                GlobalMemoryManagerAccessor.memoryManager.returnBuffer(buffer);
            }

            currentPosition = currentPosition.add(renderRequest.getStep());
            messagingService.sendAsyncMessage(new ProgressAdvancedMessage(renderRequest.getRenderId(), 1));
            ++frameIndex;
        }

        FFmpegClearEncoderRequest clearRequest = new FFmpegClearEncoderRequest();
        clearRequest.encoderIndex = encoderIndex;

        ffmpegBasedMediaEncoder.clearEncoder(clearRequest);
    }

    private ByteBuffer convertAudio(AudioFrameResult audioResult) {
        ByteBuffer result = GlobalMemoryManagerAccessor.memoryManager.requestBuffer(audioResult.getBytesPerSample() * audioResult.getNumberSamples() * audioResult.getChannels().size());

        int index = 0;
        for (int i = 0; i < audioResult.getNumberSamples(); ++i) {
            for (int j = 0; j < audioResult.getChannels().size(); ++j) {
                for (int k = 0; k < audioResult.getBytesPerSample(); ++k) {
                    result.put(index++, audioResult.getChannels().get(j).get(i * audioResult.getBytesPerSample() + k));
                }
            }
        }

        //        int sum = 0;
        //        System.out.println("#############################x");
        //        for (int i = 0; i < result.capacity(); ++i) {
        //            System.out.print(result.get(i) + " ");
        //            sum += result.get(i);
        //        }
        //        System.out.println("\n/////////////////////////////\n" + sum);

        return result;
    }

    @Override
    public String getId() {
        return "ffmpegrenderer";
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("mpeg");
    }

    @Override
    public boolean supports(RenderRequest renderRequest) {
        //        return renderRequest.getFileName().endsWith(".mpeg");
        return true;
    }

    @Override
    public Map<String, OptionProvider<?>> getOptionProviders(CreateValueProvidersRequest request) {
        int maximumVideoBitRate = timelineManagerAccessor.findMaximumVideoBitRate();
        OptionProvider<Integer> bitRateProvider = OptionProvider.integerOptionBuilder()
                .withTitle("Video bitrate")
                .withDefaultValue(maximumVideoBitRate > 0 ? maximumVideoBitRate : 1600000)
                .withValidationErrorProvider(bitRate -> {
                    List<String> errors = new ArrayList<>();
                    if (bitRate < 100) {
                        errors.add("Too low bitrate");
                    }
                    return errors;
                })
                .build();
        int maximumAudioBitRate = timelineManagerAccessor.findMaximumAudioBitRate();
        OptionProvider<Integer> audioBitRateProvider = OptionProvider.integerOptionBuilder()
                .withTitle("Audio bitrate")
                .withDefaultValue(maximumAudioBitRate > 0 ? maximumAudioBitRate : 192000)
                .withValidationErrorProvider(bitRate -> {
                    List<String> errors = new ArrayList<>();
                    if (bitRate < 100) {
                        errors.add("Too low bitrate");
                    }
                    return errors;
                })
                .build();
        OptionProvider<Integer> audioSampleRateProvider = OptionProvider.integerOptionBuilder()
                .withTitle("Audio samplerate")
                .withDefaultValue(projectRepository.getSampleRate())
                .withValidationErrorProvider(bitRate -> {
                    List<String> errors = new ArrayList<>();
                    return errors;
                })
                .build();
        OptionProvider<String> audioBytesPerSampelProvider = OptionProvider.stringOptionBuilder()
                .withTitle("Bytes/sample")
                .withDefaultValue(projectRepository.getBytesPerSample() + "")
                .withValidValues(List.of(createElement("1"), createElement("2"), createElement("4")))
                .build();

        CodecValueElements codecs = queryCodecInformation();

        OptionProvider<String> videoCodecProvider = OptionProvider.stringOptionBuilder()
                .withTitle("Video codec")
                .withDefaultValue(DEFAULT_VALUE)
                .withValidValues(codecs.videoCodecs)
                .withShouldTriggerUpdate(true)
                .build();
        OptionProvider<String> audioCodecProvider = OptionProvider.stringOptionBuilder()
                .withTitle("Audio codec")
                .withDefaultValue(DEFAULT_VALUE)
                .withValidValues(codecs.audioCodecs)
                .build();

        EncoderExtraData extra = queryCodecExtraData(request.fileName, DEFAULT_VALUE);

        OptionProvider<String> videoPixelFormatProvider = OptionProvider.stringOptionBuilder()
                .withTitle("Video pixel format")
                .withDefaultValue(DEFAULT_VALUE)
                .withValidValues(extra.pixelFormats)
                .build();

        LinkedHashMap<String, OptionProvider<?>> result = new LinkedHashMap<>();

        result.put("videobitrate", bitRateProvider);
        result.put("audiobitrate", audioBitRateProvider);
        result.put("audiosamplerate", audioSampleRateProvider);
        result.put("audiobytespersample", audioBytesPerSampelProvider);
        result.put("videocodec", videoCodecProvider);
        result.put("audiocodec", audioCodecProvider);
        result.put("videoPixelFormat", videoPixelFormatProvider);

        return result;
    }

    @Override
    public Map<String, OptionProvider<?>> updateValueProviders(UpdateValueProvidersRequest request) {
        Map<String, OptionProvider<?>> optionsToUpdate = new LinkedHashMap<String, OptionProvider<?>>(request.options);

        // update pixel formats
        String codec = (String) optionsToUpdate.get("videocodec").getValue();
        OptionProvider<?> pixelFormat = optionsToUpdate.get("videoPixelFormat");
        EncoderExtraData extra = queryCodecExtraData(request.fileName, codec);
        if (!pixelFormat.getValidValues().equals(extra.pixelFormats)) {
            optionsToUpdate.put("videoPixelFormat", pixelFormat.butWithUpdatedValidValues(extra.pixelFormats));
        }

        return optionsToUpdate;
    }

    private ValueListElement createElement(String e) {
        return new ValueListElement(e, e);
    }

    static class CodecValueElements {
        List<ValueListElement> videoCodecs = new ArrayList<>();
        List<ValueListElement> audioCodecs = new ArrayList<>();
    }

    private CodecValueElements queryCodecInformation() {
        QueryCodecRequest nativeRequest = new QueryCodecRequest();
        nativeRequest.audioCodecs = new CodecInformation();
        CodecInformation[] audioCodecs = (CodecInformation[]) nativeRequest.audioCodecs.toArray(MAX_NUMBER_OF_CODECS);
        nativeRequest.videoCodecs = new CodecInformation();
        CodecInformation[] videoCodecs = (CodecInformation[]) nativeRequest.videoCodecs.toArray(MAX_NUMBER_OF_CODECS);

        ffmpegBasedMediaEncoder.queryCodecs(nativeRequest);

        CodecValueElements result = new CodecValueElements();

        for (int i = 0; i < nativeRequest.videoCodecNumber; ++i) {
            result.videoCodecs.add(new ValueListElement(videoCodecs[i].id, "[" + videoCodecs[i].id + "] " + videoCodecs[i].longName));
        }
        for (int i = 0; i < nativeRequest.audioCodecNumber; ++i) {
            result.audioCodecs.add(new ValueListElement(audioCodecs[i].id, "[" + audioCodecs[i].id + "] " + audioCodecs[i].longName));
        }
        result.videoCodecs.add(new ValueListElement(DEFAULT_VALUE, DEFAULT_VALUE));
        result.audioCodecs.add(new ValueListElement(DEFAULT_VALUE, DEFAULT_VALUE));
        return result;
    }

    static class EncoderExtraData {
        List<ValueListElement> pixelFormats = new ArrayList<>();
    }

    private EncoderExtraData queryCodecExtraData(String fileName, String videoCodec) {
        CodecExtraDataRequest nativeRequest = new CodecExtraDataRequest();
        nativeRequest.availablePixelFormatNumber = 0;
        nativeRequest.fileName = fileName;
        nativeRequest.videoCodec = videoCodec;
        nativeRequest.availablePixelFormats = new CodecInformation();
        CodecInformation[] availablePixelFormats = (CodecInformation[]) nativeRequest.availablePixelFormats.toArray(MAX_NUMBER_OF_CODECS);

        ffmpegBasedMediaEncoder.queryCodecExtraData(nativeRequest);

        EncoderExtraData result = new EncoderExtraData();

        result.pixelFormats.add(new ValueListElement(DEFAULT_VALUE, DEFAULT_VALUE));
        for (int i = 0; i < nativeRequest.availablePixelFormatNumber; ++i) {
            result.pixelFormats.add(new ValueListElement(availablePixelFormats[i].id, availablePixelFormats[i].id));
        }
        return result;
    }

}
