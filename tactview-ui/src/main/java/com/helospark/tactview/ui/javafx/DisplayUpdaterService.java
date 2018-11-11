package com.helospark.tactview.ui.javafx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.GlobalDirtyClipManager;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.ui.javafx.audio.AudioStreamService;
import com.helospark.tactview.ui.javafx.repository.UiProjectRepository;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

@Component
public class DisplayUpdaterService {
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private Map<TimelinePosition, Future<JavaDisplayableAudioVideoFragment>> framecache = new ConcurrentHashMap<>();
    private volatile long currentPositionLastRendered = -1;
    private volatile boolean running = true;

    private PlaybackController playbackController;
    private UiProjectRepository uiProjectRepostiory;
    private UiTimelineManager uiTimelineManager;
    private GlobalDirtyClipManager globalDirtyClipManager;
    private AudioStreamService audioStreamService;

    @Slf4j
    private Logger logger;

    private Canvas canvas;

    public DisplayUpdaterService(PlaybackController playbackController, UiProjectRepository uiProjectRepostiory, UiTimelineManager uiTimelineManager,
            GlobalDirtyClipManager globalDirtyClipManager, AudioStreamService audioStreamService) {
        this.playbackController = playbackController;
        this.uiProjectRepostiory = uiProjectRepostiory;
        this.uiTimelineManager = uiTimelineManager;
        this.globalDirtyClipManager = globalDirtyClipManager;
        this.audioStreamService = audioStreamService;
    }

    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(200);
                    TimelinePosition currentPosition = uiTimelineManager.getCurrentPosition();
                    long currentPostionLastModified = globalDirtyClipManager.positionLastModified(currentPosition);
                    if (currentPostionLastModified > currentPositionLastRendered) {
                        updateCurrentPosition();
                        logger.debug("Current position changed, updating {}", currentPosition);
                    }
                } catch (Exception e) {
                    logger.warn("Unable to check dirty state of display", e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void destroy() {
        running = false;
    }

    // TODO: something is really not right here
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    public void updateCurrentPosition() {
        updateDisplay(uiTimelineManager.getCurrentPosition());
    }

    public void updateDisplay(TimelinePosition currentPosition) {
        Future<JavaDisplayableAudioVideoFragment> cachedKey = framecache.remove(currentPosition);
        JavaDisplayableAudioVideoFragment actualAudioVideoFragment;
        if (cachedKey == null) {
            actualAudioVideoFragment = playbackController.getFrameAt(currentPosition);
        } else {
            try {
                System.out.println("Served from cache " + currentPosition);
                actualAudioVideoFragment = cachedKey.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        currentPositionLastRendered = System.currentTimeMillis();
        logger.debug("Rendered at {}", currentPositionLastRendered);
        Platform.runLater(() -> {
            int width = uiProjectRepostiory.getPreviewWidth();
            int height = uiProjectRepostiory.getPreviewHeight();
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(actualAudioVideoFragment.getImage(), 0, 0, width, height);
        });
        audioStreamService.streamAudio(actualAudioVideoFragment.getAudioData());

        startCacheJobs(currentPosition);
    }

    private void startCacheJobs(TimelinePosition currentPosition) {
        List<TimelinePosition> expectedNextFrames = uiTimelineManager.expectedNextFrames();
        for (TimelinePosition nextFrameTime : expectedNextFrames) {
            if (!framecache.containsKey(nextFrameTime)) {
                Future<JavaDisplayableAudioVideoFragment> task = executorService.submit(() -> {
                    return playbackController.getFrameAt(currentPosition);
                });
                framecache.put(nextFrameTime, task);
                System.out.println("started " + nextFrameTime);
            }
        }
    }

}
