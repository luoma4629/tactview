package com.helospark.tactview.ui.javafx.uicomponents.pattern;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.preference.PreferenceValue;
import com.helospark.tactview.core.timeline.AudibleTimelineClip;
import com.helospark.tactview.core.timeline.TimelineClip;
import com.helospark.tactview.core.timeline.TimelineManagerAccessor;
import com.helospark.tactview.core.timeline.VisualTimelineClip;
import com.helospark.tactview.core.timeline.message.ClipAddedMessage;
import com.helospark.tactview.core.timeline.message.ClipRemovedMessage;
import com.helospark.tactview.core.timeline.message.ClipResizedMessage;
import com.helospark.tactview.core.timeline.message.KeyframeEnabledWasChangedMessage;
import com.helospark.tactview.core.timeline.message.KeyframeSuccesfullyAddedMessage;
import com.helospark.tactview.core.timeline.message.KeyframeSuccesfullyRemovedMessage;
import com.helospark.tactview.core.util.ThreadSleep;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.core.util.messaging.MessagingService;
import com.helospark.tactview.ui.javafx.menu.defaultmenus.projectsize.RegenerateAllImagePatternsMessage;
import com.helospark.tactview.ui.javafx.uicomponents.TimelineState;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

@Component
public class ClipPatternDrawerListener {
    private Map<String, ClipPatternUpdateDomain> clipsToUpdate = new ConcurrentHashMap<>();
    private Set<ClipPatternUpdateRequest> updateRequests = new ConcurrentSkipListSet<>();
    private volatile boolean running = true;

    private MessagingService messagingService;
    private TimelineImagePatternService timelineImagePatternService;
    private AudioImagePatternService audioImagePatternService;
    private TimelineState timelineState;
    private TimelineManagerAccessor timelineManager;
    @Slf4j
    private Logger logger;

    private boolean patternDrawingEnabled = true;

    public ClipPatternDrawerListener(MessagingService messagingService, TimelineImagePatternService timelineImagePatternService,
            TimelineState timelineState, AudioImagePatternService audioImagePatternService, TimelineManagerAccessor timelineManager) {
        this.messagingService = messagingService;
        this.timelineImagePatternService = timelineImagePatternService;
        this.timelineState = timelineState;
        this.audioImagePatternService = audioImagePatternService;
        this.timelineManager = timelineManager;
    }

    @PreferenceValue(name = "Render pattern on clips", defaultValue = "true", group = "Performance")
    public void setImageClipLength(boolean patternDrawingEnabled) {
        this.patternDrawingEnabled = patternDrawingEnabled;
    }

    @PostConstruct
    public void init() {
        startConsumingThread();

        messagingService.register(ClipAddedMessage.class, message -> {
            clipsToUpdate.put(message.getClipId(), new ClipPatternUpdateDomain(message.getClip(), timelineState.getZoom()));
            updateRequests.add(new ClipPatternUpdateRequest(message.getClipId()));
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
        messagingService.register(KeyframeEnabledWasChangedMessage.class, message -> {
            if (clipsToUpdate.containsKey(message.getContainerId())) {
                updateRequests.add(new ClipPatternUpdateRequest(message.getContainerId()));
            }
        });
        messagingService.register(RegenerateAllImagePatternsMessage.class, message -> {
            updateRequests.clear();
            timelineManager.getAllClipIds()
                    .stream()
                    .forEach(clipId -> updateRequests.add(new ClipPatternUpdateRequest(clipId)));
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
                    updateRequests.removeAll(clonedRequests);
                    for (var request : clonedRequests) {
                        if (clipsToUpdate.containsKey(request.clipId)) {
                            logger.info("Updating timeline image pattern for {}", request.clipId);
                            updatePattern(request);
                        } else {
                            logger.info("No clip to update with id {}", request.clipId);
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
        }, "clip-pattern-updater-thread").start();
    }

    private void updatePattern(ClipPatternUpdateRequest request) {
        if (patternDrawingEnabled) {
            Pane clip = timelineState.findClipById(request.clipId).orElseThrow();
            ClipPatternUpdateDomain clipsToUpdateDomain = clipsToUpdate.get(request.clipId);
            double zoom = timelineState.getZoom();

            int pixelWidth = timelineState.secondsToPixels(timelineManager.findClipById(request.clipId).map(cl -> cl.getInterval().getLength()).get());
            int width = (int) (pixelWidth * zoom);
            Rectangle rectangle = (Rectangle) clip.getChildren().get(0);

            TimelineClip clipToUpdate = clipsToUpdateDomain.videoClip;
            Paint image;
            if (width <= 16000) {
                if (clipToUpdate instanceof VisualTimelineClip) {
                    VisualTimelineClip videoClip = (VisualTimelineClip) clipToUpdate;
                    image = new ImagePattern(timelineImagePatternService.createTimelinePattern(videoClip, width));
                } else if (clipToUpdate instanceof AudibleTimelineClip) {
                    AudibleTimelineClip audibleTimelineClip = (AudibleTimelineClip) clipToUpdate;
                    image = new ImagePattern(audioImagePatternService.createAudioImagePattern(audibleTimelineClip, width));
                } else {
                    image = new Color(0, 0, 0, 1.0);
                }

                Platform.runLater(() -> rectangle.setFill(image));
            } else {
                Platform.runLater(() -> rectangle.setFill(Color.GRAY));
            }
            clipsToUpdate.put(request.clipId, clipsToUpdateDomain.butWithZoomLevel(zoom));
        }
    }

    static class ClipPatternUpdateDomain {
        private TimelineClip videoClip;
        private double createdImageAtZoomLevel;

        public ClipPatternUpdateDomain(TimelineClip videoClip, double createImageAtZoomLevel) {
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
