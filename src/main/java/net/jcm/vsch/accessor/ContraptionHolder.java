package net.jcm.vsch.accessor;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import java.util.List;

public interface ContraptionHolder {
	List<AbstractContraptionEntity> starlance$clearContraptions();

	void starlance$restoreContraptions(List<AbstractContraptionEntity> contraptions);
}
