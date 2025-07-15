package net.jcm.vsch.api.pipe.capability;

import net.jcm.vsch.api.pipe.RelativeNodePos;

@FunctionalInterface
public interface INodePortProvider {
	NodePort getNodePort(RelativeNodePos pos);
}
