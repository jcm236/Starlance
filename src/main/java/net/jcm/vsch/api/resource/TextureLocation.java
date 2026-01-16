/**
 * Copyright (C) 2025  the authors of Starlance
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 **/
package net.jcm.vsch.api.resource;

import net.minecraft.resources.ResourceLocation;

public record TextureLocation(ResourceLocation location, int offsetX, int offsetY, float scale) {
	public TextureLocation(ResourceLocation location, int offsetX, int offsetY) {
		this(location, offsetX, offsetY, 1f);
	}

	public static TextureLocation fromNonStandardSize(ResourceLocation location, int offsetX, int offsetY, int size) {
		return new TextureLocation(location, offsetX, offsetY, 16f / size);
	}
}
