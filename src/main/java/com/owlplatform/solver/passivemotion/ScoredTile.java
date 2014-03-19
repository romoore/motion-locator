/*
 * Motion Locator Solver for Owl Platform
 * Copyright (C) 2012 Robert Moore and the Owl Platform
 * Copyright (C) 2011 Rutgers University and Robert Moore
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.owlplatform.solver.passivemotion;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;

public class ScoredTile implements Cloneable {

	protected Rectangle2D.Float tile;

	protected float score;

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public Rectangle2D.Float getTile() {
		return tile;
	}

	public void setTile(Rectangle2D.Float tile) {
		this.tile = tile;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("Scored Tile (").append(this.tile.x).append(", ").append(
				this.tile.y).append(") (")
				.append(this.tile.x + this.tile.width).append(", ").append(
						this.tile.y + this.tile.height).append("): ").append(
						this.score);

		return sb.toString();
	}

	/**
	 * Copies this tile, including its score and a copy of the underlying Rectangle2D.
	 */
	@Override
	public ScoredTile clone() {
		ScoredTile newTile = new ScoredTile();
		newTile.setScore(this.getScore());
		newTile.setTile(new Rectangle2D.Float((float) this.tile.getX(),
				(float) this.tile.getY(), (float) this.tile.getWidth(),
				(float) this.tile.getHeight()));
		return newTile;
	}

}
