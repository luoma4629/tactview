package com.helospark.tactview.ui.javafx.uicomponents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;

import com.helospark.lightdi.annotation.Component;
import com.helospark.tactview.core.timeline.TimelinePosition;
import com.helospark.tactview.core.timeline.effect.interpolation.KeyframeableEffect;
import com.helospark.tactview.core.timeline.effect.interpolation.ValueProviderDescriptor;
import com.helospark.tactview.core.timeline.message.ClipAddedMessage;
import com.helospark.tactview.core.timeline.message.ClipDescriptorsAdded;
import com.helospark.tactview.core.timeline.message.EffectDescriptorsAdded;
import com.helospark.tactview.core.util.logger.Slf4j;
import com.helospark.tactview.core.util.messaging.MessagingService;
import com.helospark.tactview.ui.javafx.UiTimelineManager;
import com.helospark.tactview.ui.javafx.uicomponents.EffectPropertyPage.Builder;
import com.helospark.tactview.ui.javafx.uicomponents.detailsdata.DetailsGridChain;
import com.helospark.tactview.ui.javafx.uicomponents.propertyvalue.EffectLine;
import com.helospark.tactview.ui.javafx.uicomponents.propertyvalue.PropertyValueSetterChain;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

@Component
public class PropertyView {
    private FlowPane propertyWindow;
    private Map<String, GridPane> details = new HashMap<>();
    private Map<String, EffectPropertyPage> effectProperties = new HashMap<>();
    private Map<String, EffectPropertyPage> clipProperties = new HashMap<>();

    private MessagingService messagingService;
    private UiTimelineManager uiTimelineManager;
    private EffectPropertyPage shownEntries;
    private PropertyValueSetterChain propertyValueSetterChain;
    private DetailsGridChain detailsGridChain;

    @Slf4j
    private Logger logger;

    public PropertyView(MessagingService messagingService, UiTimelineManager uiTimelineManager, PropertyValueSetterChain propertyValueSetterChain,
            DetailsGridChain detailsGridChain) {
        this.messagingService = messagingService;
        this.uiTimelineManager = uiTimelineManager;
        this.propertyValueSetterChain = propertyValueSetterChain;
        this.detailsGridChain = detailsGridChain;
    }

    @PostConstruct
    public void init() {
        propertyWindow = new FlowPane();
        propertyWindow.setId("property-view");
        propertyWindow.setPrefWidth(200);

        messagingService.register(ClipAddedMessage.class, message -> {
            String clipId = message.getClipId();
            GridPane grid = detailsGridChain.createDetailsGridForClip(clipId);
            details.put(clipId, grid);
        });

        messagingService.register(EffectDescriptorsAdded.class, message -> Platform.runLater(() -> {
            EffectPropertyPage asd = createBox(message.getDescriptors());
            effectProperties.put(message.getEffectId(), asd);
        }));
        messagingService.register(ClipDescriptorsAdded.class, message -> Platform.runLater(() -> {
            EffectPropertyPage asd = createBox(message.getDescriptors());
            clipProperties.put(message.getClipId(), asd);
        }));
    }

    private EffectPropertyPage createBox(List<ValueProviderDescriptor> descriptors) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("effect-property-grid");
        Builder result = EffectPropertyPage.builder().withBox(grid);
        for (int i = 0; i < descriptors.size(); ++i) {
            addElement(descriptors.get(i), result, i);
        }
        return result.build();
    }

    private void addElement(ValueProviderDescriptor descriptor, Builder result, int line) {
        Label label = new Label(descriptor.getName());
        EffectLine keyframeChange = createKeyframeUi(descriptor.getKeyframeableEffect());

        Node key = keyframeChange.getVisibleNode();
        key.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode().equals(KeyCode.INSERT)) {
                keyframeChange.sendKeyframe(uiTimelineManager.getCurrentPosition());
                logger.info("Keyframe added");
                event.consume();
            }
        });

        result.getBox().add(label, 0, line);
        result.getBox().add(key, 1, line);

        result.addUpdateFunctions(keyframeChange::updateUi);
    }

    private EffectLine createKeyframeUi(KeyframeableEffect keyframeableEffect) {
        return propertyValueSetterChain.create(keyframeableEffect);
    }

    public FlowPane getPropertyWindow() {
        return propertyWindow;
    }

    public void showEffectProperties(String effectId) {
        showProperties(effectProperties.get(effectId), effectId);
    }

    public void showClipProperties(String clipId) {
        showProperties(clipProperties.get(clipId), clipId);
    }

    private void showProperties(EffectPropertyPage shownEntries2, String id) {
        shownEntries = shownEntries2;
        propertyWindow.getChildren().clear();
        GridPane dataGrid = details.get(id);
        if (dataGrid != null) {
            VBox vbox = new VBox();
            vbox.getChildren().addAll(dataGrid, new Separator());

            propertyWindow.getChildren().add(vbox);
        }
        if (shownEntries2 != null) {
            propertyWindow.getChildren().add(shownEntries.getBox());
            shownEntries2.getUpdateFunctions().stream().forEach(a -> a.accept(uiTimelineManager.getCurrentPosition()));
        } else {
            System.out.println("Effect not found, should not happen");
        }
    }

    public void clearProperties() {
        shownEntries = null;
    }

    public void updateValues(TimelinePosition position) {
        if (shownEntries != null) {
            shownEntries.getUpdateFunctions().stream().forEach(updateFunction -> updateFunction.accept(position));
        }
    }

}
