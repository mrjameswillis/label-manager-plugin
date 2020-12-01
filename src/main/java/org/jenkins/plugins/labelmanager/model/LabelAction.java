package org.jenkins.plugins.labelmanager.model;

public enum LabelAction {
    ADD("Add"), REMOVE("Remove"), REPLACE("Replace");

    private final String name;

    LabelAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
