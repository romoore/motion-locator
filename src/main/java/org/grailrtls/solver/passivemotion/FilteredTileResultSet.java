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
package org.grailrtls.solver.passivemotion;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilteredTileResultSet {
	protected final ConcurrentHashMap<String, FilteredTileResult> results = new ConcurrentHashMap<String, FilteredTileResult>();
	
	protected Collection<RSSILine> lines = null;
	
	public Collection<RSSILine> getLines() {
		return lines;
	}

	public void setLines(Collection<RSSILine> lines) {
		this.lines = lines;
	}

	protected Collection<ScoredTile> tilesToPublish = null;
	
	public void setTiles(final String description, final FilteredTileResult result)
	{
		this.results.put(description,result);
	}
	
	public Map<String, FilteredTileResult> getResults()
	{
		return this.results;
	}
	
	public FilteredTileResult getResult(final String description)
	{
		return this.results.get(description);
	}

	public Collection<ScoredTile> getTilesToPublish() {
		return tilesToPublish;
	}

	public void setTilesToPublish(Collection<ScoredTile> tilesToPublish) {
		this.tilesToPublish = tilesToPublish;
	}
}
