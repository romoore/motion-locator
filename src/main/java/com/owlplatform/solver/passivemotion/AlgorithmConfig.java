/*
 * Motion Solver for Owl Platform
 * Copyright (C) 2012 Robert Moore and the Owl Platform
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

/**
 * @author Robert Moore
 *
 */
public class AlgorithmConfig {
  
  
  protected float tileScoreThreshold = .5f;

  protected float stdDevNoiseThreshold = 1.2f;

  protected float radiusThreshold = 90f;
  
  protected float linkMinDistance = 6f;

  protected float lineLengthPower = 1.1f;
  
  protected float desiredTileWidth = 20f;

  protected float desiredTileHeight = 20f;
  
  protected float neighborRatio = 0.5f;
  
  protected float peakRatio = 0.5f;
}
