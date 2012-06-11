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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.grailrtls.solver.passivemotion.ScoredTile;
import org.grailrtls.solver.passivemotion.FilteredTileResultSet;

public class GraphicalUserInterface extends JFrame implements
		UserInterfaceAdapter {

	protected final ConcurrentLinkedQueue<FilteredTileResultSet> tileHistory = new ConcurrentLinkedQueue<FilteredTileResultSet>();

	protected ConcurrentHashMap<String, TileViewPanel> tilePanels = new ConcurrentHashMap<String, TileViewPanel>();

	protected JTabbedPane tabbedPane = new JTabbedPane();
	
	protected BufferedImage backgroundImage = null;

	protected final ExecutorService workers = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public GraphicalUserInterface() {
		super();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(800, 600));
		this.setLayout(new BorderLayout());
		this.add(BorderLayout.CENTER, this.tabbedPane);
		this.pack();
		this.setVisible(true);
	}

	@Override
	public void solutionGenerated(final FilteredTileResultSet tileSet) {

		if(tileSet == null){
			return;
		}
		this.workers.execute(new Runnable() {

			@Override
			public void run() {
				// TODO: Add history?
				GraphicalUserInterface.this.tileHistory.poll();
				GraphicalUserInterface.this.tileHistory.add(tileSet);

				for (String desc : tileSet.getResults().keySet()) {
					TileViewPanel panel = null;
					synchronized (GraphicalUserInterface.this.tilePanels) {
						panel = GraphicalUserInterface.this.tilePanels
								.get(desc);
						if (panel == null) {
							panel = new TileViewPanel();
							panel.setBackgroundImage(GraphicalUserInterface.this.backgroundImage);
							GraphicalUserInterface.this.tilePanels.put(desc,
									panel);
							GraphicalUserInterface.this.tabbedPane.add(desc,
									panel);
						}
					}
					panel.setLines(tileSet.getLines());
					panel.setTiles(tileSet.getResult(desc).getTiles());

				}
			}
		});
	}

	@Override
	public void setBackground(BufferedImage backgroundImage) {
		this.backgroundImage = backgroundImage;
		for(TileViewPanel panel : this.tilePanels.values()){
			panel.setBackgroundImage(backgroundImage);
		}
	}

}
