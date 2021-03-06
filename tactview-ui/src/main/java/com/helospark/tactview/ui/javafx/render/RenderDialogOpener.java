package com.helospark.tactview.ui.javafx.render;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.render.RenderServiceChain;
import com.helospark.tactview.core.repository.ProjectRepository;
import com.helospark.tactview.core.timeline.TimelineManagerAccessor;
import com.helospark.tactview.ui.javafx.UiMessagingService;

@Component
public class RenderDialogOpener {
    private RenderServiceChain renderService;
    private ProjectRepository projectRepository;
    private UiMessagingService messagingService;
    private TimelineManagerAccessor timelineManager;

    public RenderDialogOpener(RenderServiceChain renderService, ProjectRepository projectRepository, UiMessagingService messagingService, TimelineManagerAccessor timelineManager) {
        this.renderService = renderService;
        this.projectRepository = projectRepository;
        this.messagingService = messagingService;
        this.timelineManager = timelineManager;
    }

    public void render() {
        RenderDialog renderDialog = new RenderDialog(renderService, projectRepository, messagingService, timelineManager);
        renderDialog.show();
    }

}
