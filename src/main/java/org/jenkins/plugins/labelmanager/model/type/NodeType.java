package org.jenkins.plugins.labelmanager.model.type;

import hudson.model.Node;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * implements Serializable
 */
public class NodeType implements TypeInterface {

    public static final String MASTER_NODE = "master";

    private String name;
    private String fullName;
    transient private Optional<Integer> numberOfExecutors;

    public NodeType(Node node) {
        this(node.getNodeName(), Optional.empty());
    }

    public NodeType(String name) {
        this(name, Optional.empty());
    }

    public NodeType(Node node, int numberOfExecutors) {
        this(node.getNodeName(), Optional.of(numberOfExecutors));
    }

    public NodeType(String name, int numberOfExecutors) {
        this(name, Optional.of(numberOfExecutors));
    }

    public NodeType(String name, Optional<Integer> numberOfExecutors) {
        if (name != null && name.startsWith("\"") && name.endsWith("\""))
            this.name = name.replaceAll("^\"|\"$", "");
        else
            this.name = name;
        this.fullName = name;
        this.numberOfExecutors = numberOfExecutors;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public Optional getNumberOfExecutors() {
        return numberOfExecutors;
    }

    @Override
    public Node get() {
        if (name.compareToIgnoreCase(MASTER_NODE) == 0) {
            return Jenkins.getActiveInstance();
        }
        return Jenkins.getActiveInstance().getNode(name);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        if (numberOfExecutors.isPresent()) stream.writeInt(numberOfExecutors.get());
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            numberOfExecutors = Optional.of(stream.readInt());
        } catch (Exception e) {
            numberOfExecutors = Optional.empty();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static final long serialVersionUID = 1L;
}
