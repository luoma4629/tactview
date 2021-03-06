package com.helospark.tactview.ui.javafx.repository.selection;

import javafx.scene.Node;

public class ClipSelectionChangedMessage {
    private Node clip;
    private ChangeType type;

    public ClipSelectionChangedMessage(Node item, ChangeType type) {
        this.clip = item;
        this.type = type;
    }

    public Node getClip() {
        return clip;
    }

    public ChangeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ClipSelectionChangedMessage [clip=" + clip.getUserData() + ", type=" + type + "]";
    }

}
