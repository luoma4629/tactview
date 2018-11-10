package com.helospark.tactview.core.decoder.ffmpeg.audio;

import com.sun.jna.Library;

//@NativeImplementation("avcodecaudiodecoder")
public interface AVCodecBasedAudioMediaDecoderImplementation extends Library {

    public AVCodecAudioMetadataResponse readMetadata(String filepath);

    public void readAudio(AVCodecAudioRequest request);
}
