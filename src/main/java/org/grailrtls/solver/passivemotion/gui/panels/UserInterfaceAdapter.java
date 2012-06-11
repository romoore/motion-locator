/*
 * GRAIL Real Time Localization System
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
package org.grailrtls.solver.passivemotion.gui.panels;

import java.awt.image.BufferedImage;

import org.grailrtls.solver.passivemotion.ScoredTile;
import org.grailrtls.solver.passivemotion.FilteredTileResultSet;

public interface UserInterfaceAdapter {
	
	public void solutionGenerated(FilteredTileResultSet tiles);
	
	public void setBackground(BufferedImage backgroundImage);
}
