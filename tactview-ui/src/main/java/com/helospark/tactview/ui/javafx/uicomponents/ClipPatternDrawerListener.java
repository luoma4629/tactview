package com.helospark.tactview.ui.javafx.uicomponents;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.VisualTimelineClip;
import com.helospark.tactview.core.timeline.message.ClipAddedMessage;
import com.helospark.tactview.core.timeline.message.ClipRemovedMessage;
import com.helospark.tactview.core.timeline.message.ClipResizedMessage;
import com.helospark.tactview.core.timeline.message.KeyframeSuccesfullyAddedMessage;
import com.helospark.tactview.core.timeline.message.KeyframeSuccesfullyRemovedMessage;
import com.helospark.tactview.core.util.ThreadSleep;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.core.util.messaging.MessagingService;
import com.helospark.tactview.ui.javafx.TimelineImagePatternService;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

@Component
public class ClipPatternDrawerListener {
    private Map<String, ClipPatternUpdateDomain> clipsToUpdate = new ConcurrentHashMap<>();
    private Set<ClipPatternUpdateRequest> updateRequests = new ConcurrentSkipListSet<>();
    private volatile boolean running = true;

    private MessagingService messagingService;
    private TimelineImagePatternService timelineImagePatternService;
    private TimelineState timelineState;
    @Slf4j
    private Logger logger;

    public ClipPatternDrawerListener(MessagingService messagingService, TimelineImagePatternService timelineImagePatternService,
            TimelineState timelineState) {
        this.messagingService = messagingService;
        this.timelineImagePatternService = timelineImagePatternService;
        this.timelineState = timelineState;
    }

    @PostConstruct
    public void init() {
        startConsumingThread();

        messagingService.register(ClipAddedMessage.class, message -> {
            if (message.getClip() instanceof VisualTimelineClip) {
                clipsToUpdate.put(message.getClipId(), new ClipPatternUpdateDomain((VisualTimelineClip) message.getClip(), timelineState.getZoom()));
                updateRequests.add(new ClipPatternUpdateRequest(message.getClipId()));
            }
        });
        messagingService.register(ClipRemovedMessage.class, message -> {
            clipsToUpdate.remove(message.getElementId());
        });
        messagingService.register(ClipResizedMessage.class, message -> {
            updateRequests.add(new ClipPatternUpdateRequest(message.getClipId()));
        });
        messagingService.register(KeyframeSuccesfullyAddedMessage.class, message -> {
            if (clipsToUpdate.containsKey(message.getContainingElementId())) {
                updateRequests.add(new ClipPatternUpdateRequest(message.getContainingElementId()));
            }
        });
        messagingService.register(KeyframeSuccesfullyRemovedMessage.class, message -> {
            if (clipsToUpdate.containsKey(message.getContainingElementId())) {
                updateRequests.add(new ClipPatternUpdateRequest(message.getContainingElementId()));
            }
        });
    }

    @PreDestroy
    public void destroy() {
        this.running = false;
    }

    private void startConsumingThread() {
        new Thread(() -> {
            while (running) {
                try {
                    ThreadSleep.sleep(1000);
                    Set<ClipPatternUpdateRequest> clonedRequests = new HashSet<ClipPatternUpdateRequest>(updateRequests);
                    updateRequests.clear();
                    for (var request : clonedRequests) {
                        if (clipsToUpdate.containsKey(request.clipId)) {
                            logger.info("Updating timeline image pattern for {}", request.clipId);
                            updatePattern(request);
                        }
                    }
                    double currentZoomLevel = timelineState.getZoom();
                    for (var entry : clipsToUpdate.entrySet()) {
                        if (Math.abs(entry.getValue().createdImageAtZoomLevel - currentZoomLevel) > 0.2) {
                            updateRequests.add(new ClipPatternUpdateRequest(entry.getKey()));
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Image pattern update failed", e);
                }
            }
        }).start();
    }

    private void updatePattern(ClipPatternUpdateRequest request) {
        Pane clip = timelineState.findClipById(request.clipId).orElseThrow();
        ClipPatternUpdateDomain clipsToUpdateDomain = clipsToUpdate.get(request.clipId);
        VisualTimelineClip videoClip = clipsToUpdateDomain.videoClip;
        double zoom = timelineState.getZoom();

        int width = (int) (clip.getWidth() * zoom);
        Rectangle rectangle = (Rectangle) clip.getChildren().get(0);

        Image image = timelineImagePatternService.createTimelinePattern(videoClip, width);
        Platform.runLater(() -> rectangle.setFill(new ImagePattern(image)));
        clipsToUpdate.put(request.clipId, clipsToUpdateDomain.butWithZoomLevel(zoom));
    }

    static class ClipPatternUpdateDomain {
        private VisualTimelineClip videoClip;
        private double createdImageAtZoomLevel;

        public ClipPatternUpdateDomain(VisualTimelineClip videoClip, double createImageAtZoomLevel) {
            this.videoClip = videoClip;
            this.createdImageAtZoomLevel = createImageAtZoomLevel;
        }

        public ClipPatternUpdateDomain butWithZoomLevel(double newZoomLevel) {
            return new ClipPatternUpdateDomain(videoClip, newZoomLevel);
        }

    }

    static class ClipPatternUpdateRequest implements Comparable<ClipPatternUpdateRequest> {
        private String clipId;

        public ClipPatternUpdateRequest(String clipId) {
            this.clipId = clipId;
        }

        @Override
        public int compareTo(ClipPatternUpdateRequest o) {
            return clipId.compareTo(o.clipId);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clipId == null) ? 0 : clipId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClipPatternUpdateRequest other = (ClipPatternUpdateRequest) obj;
            if (clipId == null) {
                if (other.clipId != null)
                    return false;
            } else if (!clipId.equals(other.clipId))
                return false;
            return true;
        }

    }
}