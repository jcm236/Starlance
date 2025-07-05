package net.jcm.vsch.api.pipe;

import net.jcm.vsch.pipe.level.NodeLevel;

@FunctionalInterface
public interface PipeNodeProvider<T extends PipeNode<T>> {
	T createNode(NodeLevel level, NodePos pos);
}
