package net.jcm.vsch.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public final class RayCastUtil {
	private RayCastUtil() {}

	private static final double MAX_ENTITY_BOX_SIZE = 27.7;

	public static EntityHitResult rayCastEntity(Level level, Vec3 from, Vec3 to, Predicate<Entity> filter) {
		Vec3 unit = from.vectorTo(to).normalize().scale(MAX_ENTITY_BOX_SIZE);
		final int sections = (int) (unit.length() / MAX_ENTITY_BOX_SIZE);
		Vec3 begin, end = from;
		for (int i = 0; i < sections; i++) {
			begin = end;
			end = begin.add(unit);
			AABB box = new AABB(begin, end);
			List<Entity> entities = level.getEntities(null, box, filter);
			EntityHitResult hit = clipNearestEntity(entities, from, to);
			if (hit != null) {
				return hit;
			}
		}
		if (!end.equals(to)) {
			AABB box = new AABB(end, to);
			List<Entity> entities = level.getEntities(null, box, filter);
			EntityHitResult hit = clipNearestEntity(entities, from, to);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private static EntityHitResult clipNearestEntity(List<Entity> entities, Vec3 from, Vec3 to) {
		Entity nearestEntity = null;
		Vec3 hitPos = null;
		double nearestDist = 0;

		for (Entity entity : entities) {
			Vec3 pos = entity.getBoundingBox().clip(from, to).orElse(null);
			if (pos != null) {
				double distance = from.distanceToSqr(pos);
				if (nearestEntity == null || distance < nearestDist) {
					nearestEntity = entity;
					hitPos = pos;
					nearestDist = distance;
				}
			}
		}
		return nearestEntity == null ? null : new EntityHitResult(nearestEntity, hitPos);
	}
}
