package com.helospark.tactview.ui.javafx.uicomponents;

import javax.annotation.PostConstruct;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.TimelineManagerAccessor;
import com.helospark.tactview.core.timeline.message.ChannelAddedMessage;
import com.helospark.tactview.ui.javafx.UiCommandInterpreterService;
import com.helospark.tactview.ui.javafx.UiMessagingService;
import com.helospark.tactview.ui.javafx.commands.impl.DisableChannelCommand;
import com.helospark.tactview.ui.javafx.commands.impl.MuteChannelCommand;
import com.helospark.tactview.ui.javafx.repository.NameToIdRepository;

import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

@Component
public class ChannelAddedListener {
    private UiMessagingService messagingService;
    private TimelineState timelineState;
    private TimelineDragAndDropHandler timelineDragAndDropHandler;
    private NameToIdRepository nameToIdRepository;
    private UiCommandInterpreterService commandInterpreterService;
    private TimelineManagerAccessor timelineManager;

    public ChannelAddedListener(UiMessagingService messagingService, TimelineState timelineState, TimelineDragAndDropHandler timelineDragAndDropHandler, NameToIdRepository nameToIdRepository,
            UiCommandInterpreterService commandInterpreterService, TimelineManagerAccessor timelineManager) {
        this.messagingService = messagingService;
        this.timelineState = timelineState;
        this.timelineDragAndDropHandler = timelineDragAndDropHandler;
        this.nameToIdRepository = nameToIdRepository;
        this.commandInterpreterService = commandInterpreterService;
        this.timelineManager = timelineManager;
    }

    @PostConstruct
    public void setup() {
        messagingService.register(ChannelAddedMessage.class, message -> addChannel(message));
    }

    private void addChannel(ChannelAddedMessage message) {
        String generatedName = nameToIdRepository.generateAndAddNameForIdIfNotPresent("channel", message.getChannelId());

        System.out.println("Generated channel " + generatedName);

        HBox timeline = new HBox();
        timeline.setMinHeight(60);
        timeline.getStyleClass().add("timelinerow");
        timeline.setPrefWidth(2000);
        timeline.setMinWidth(2000);

        VBox timelineTitle = new VBox();
        TextField timelineTitleChannelNameLabel = new TextField(generatedName);
        timelineTitleChannelNameLabel.getStyleClass().add("timeline-title-channel-name-label");
        timelineTitleChannelNameLabel.setTooltip(new Tooltip("Change channel name"));

        timelineTitleChannelNameLabel.setOnKeyReleased(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                nameToIdRepository.addNameForId(e.getText(), message.getChannelId());
            }
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                timelineTitleChannelNameLabel.setText(nameToIdRepository.getNameForId(message.getChannelId()));
            }
        });
        timelineTitleChannelNameLabel.focusedProperty().addListener((value, oldValue, newValue) -> {
            if (oldValue == true && newValue == false) {
                nameToIdRepository.addNameForId(timelineTitleChannelNameLabel.getText(), message.getChannelId());
            }
        });

        timelineTitle.getChildren().add(timelineTitleChannelNameLabel);

        HBox buttonBar = new HBox();
        buttonBar.getStyleClass().add("channel-header-button-bar");

        ToggleButton disableButton = new ToggleButton("", new Glyph("FontAwesome", FontAwesome.Glyph.EYE_SLASH));
        disableButton.getStyleClass().add("channel-title-button");
        disableButton.setSelected(message.isDisabled());
        disableButton.setOnAction(e -> {
            boolean isDisable = disableButton.isSelected();
            commandInterpreterService.sendWithResult(new DisableChannelCommand(timelineManager, message.getChannelId(), isDisable));
        });
        buttonBar.getChildren().add(disableButton);

        ToggleButton muteButton = new ToggleButton("", new Glyph("FontAwesome", FontAwesome.Glyph.VOLUME_OFF));
        muteButton.getStyleClass().add("channel-title-button");
        muteButton.setSelected(message.isMute());
        muteButton.setOnAction(e -> {
            boolean isMute = muteButton.isSelected();
            commandInterpreterService.sendWithResult(new MuteChannelCommand(timelineManager, message.getChannelId(), isMute));
        });

        buttonBar.getChildren().add(muteButton);

        timelineTitle.getChildren().add(buttonBar);

        timelineTitle.setMaxWidth(200);
        timelineTitle.setMinWidth(150);
        timelineTitle.getStyleClass().add("timeline-title");

        Pane timelineRow = new Pane();
        timelineRow.minWidth(2000);
        timelineRow.minHeight(60);
        timelineRow.getStyleClass().add("timeline-clips");
        timeline.getChildren().add(timelineRow);

        timelineTitle.prefHeightProperty().bind(timelineRow.heightProperty().add(12));

        timelineDragAndDropHandler.addDragAndDrop(timeline, timelineRow, message.getChannelId());

        timelineState.addChannel(message.getIndex(), message.getChannelId(), timeline, timelineTitle);
    }
}
