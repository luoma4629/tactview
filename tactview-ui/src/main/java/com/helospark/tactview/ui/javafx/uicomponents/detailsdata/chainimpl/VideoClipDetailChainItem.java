package com.helospark.tactview.ui.javafx.uicomponents.detailsdata.chainimpl;

import java.util.Map;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.decoder.VideoMetadata;
import com.helospark.tactview.core.timeline.VideoClip;

import javafx.scene.Node;
import javafx.scene.control.Label;

@Component
public class VideoClipDetailChainItem extends TypeBasedDetailGridChainElement<VideoClip> {

    public VideoClipDetailChainItem() {
        super(VideoClip.class);
    }

    @Override
    protected void updateMapInternal(Map<String, Node> mapToUpdate, VideoClip clip) {
        mapToUpdate.put("file", new Label(clip.getBackingSource().backingFile));
        mapToUpdate.put("info", createMediaInfo(clip.getMediaMetadata()));
    }

    private Node createMediaInfo(VideoMetadata mediaMetadata) {
        return new Label(mediaMetadata.getWidth() + " × " + mediaMetadata.getHeight() + " " + mediaMetadata.getFps());
    }

}
