package com.plugins.forgerockbridge.state;

import org.forgerock.android.auth.Node;

public class PluginState {
    private Node pendingNode = null;
    private boolean didSubmitConfirmation = false;
    private String lastErrorMessage;

    public Node getPendingNode() {
        return pendingNode;
    }

    public void setPendingNode(Node pendingNode) {
        this.pendingNode = pendingNode;
    }

    public boolean getDidSubmitConfirmation() {
        return didSubmitConfirmation;
    }

    public void setDidSubmitConfirmation(boolean value) {
        this.didSubmitConfirmation = value;
    }

    public void reset() {
        this.pendingNode = null;
        this.didSubmitConfirmation = false;
    }


    public void setLastErrorMessage(String errorMessage) {
        this.lastErrorMessage = errorMessage;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

}
