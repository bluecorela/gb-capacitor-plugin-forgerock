package com.plugins.forgerockbridge.state;

import org.forgerock.android.auth.Node;

public class PluginState {
    private Node pendingNode = null;
    private String lastErrorMessage;

    public Node getPendingNode() {
        return pendingNode;
    }

    public void setPendingNode(Node pendingNode) {
        this.pendingNode = pendingNode;
    }

    public void reset() {
        this.pendingNode = null;
    }

    public void setLastErrorMessage(String errorMessage) {
        this.lastErrorMessage = errorMessage;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

}
