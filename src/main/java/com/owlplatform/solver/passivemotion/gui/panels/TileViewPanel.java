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
package com.owlplatform.solver.passivemotion.gui.panels;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.solver.passivemotion.RSSILine;
import com.owlplatform.solver.passivemotion.ScoredTile;

public class TileViewPanel extends JPanel {

	private static final Logger log = LoggerFactory
			.getLogger(TileViewPanel.class);

	protected ScoredTile[][] tiles = null;
	
	protected Collection<RSSILine> lines = null;

	protected BufferedImage backgroundImage = null;
	
	public BufferedImage getBackgroundImage() {
		return backgroundImage;
	}

	public void setBackgroundImage(BufferedImage backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	public Collection<RSSILine> getLines() {
		return lines;
	}

	public void setLines(Collection<RSSILine> lines) {
		this.lines = lines;
	}

	public TileViewPanel() {
		super();
	}

	
	
	public void setTiles(final ScoredTile[][] tiles) {
		this.tiles = tiles;
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				TileViewPanel.this.repaint();
			}
		});
	}

	@Override
	public String getToolTipText(MouseEvent me) {

		return "TODO: Tool tips.";

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if(!this.isVisible())
			return;
		
		Graphics2D g2 = (Graphics2D) g;

		g2.setColor(Color.BLACK);
		int screenWidth = this.getWidth();
		int screenHeight = this.getHeight();

		if(this.backgroundImage == null)
		{
			g2.fillRect(0, 0, screenWidth, screenHeight);
		}
		else
		{
			g2.drawImage(this.backgroundImage, 0, 0, screenWidth, screenHeight, 0, 0, this.backgroundImage.getWidth(), this.backgroundImage.getHeight(), null);
		}

		if (this.tiles == null)
			return;
		
		Composite oneTenthComposite = this.makeComposite(0.1f);

		long startRender = System.currentTimeMillis();


	
		// Grab a reference to the current tiles
		ScoredTile[][] currTiles = this.tiles;

		ScoredTile maxTile = currTiles[currTiles.length - 1][currTiles[currTiles.length - 1].length - 1];

		double regionWidth = maxTile.getTile().getX()
				+ maxTile.getTile().getWidth();

		double regionHeight = maxTile.getTile().getY()
				+ maxTile.getTile().getHeight();

		double xScale = screenWidth / regionWidth;
		double yScale = screenHeight / regionHeight;

		Color origColor = g2.getColor();
		Composite origComposite = g2.getComposite();		
		
		for (int x = 0; x < currTiles.length; ++x) {
			for (int y = 0; y < currTiles[x].length; ++y) {
				
//				if(currTiles[x][y].getScore() < 0.01f)
//					continue;
				
				float alpha = (float) currTiles[x][y].getScore() / 5f;

				if (alpha < 0) {
					alpha = 0.0f;
				}

				if (alpha > 1.0) {
					alpha = 1.0f;
				}
				g2.setComposite(this.makeComposite(alpha));
				g2.setColor(Color.BLUE);
				Rectangle2D.Float drawRect = new Rectangle2D.Float();

				drawRect
						.setRect(
								currTiles[x][y].getTile().getX() * xScale,
								(regionHeight
										- currTiles[x][y].getTile().getY() - currTiles[x][y]
										.getTile().getHeight())
										* yScale, currTiles[x][y].getTile()
										.getWidth()
										* xScale, currTiles[x][y].getTile()
										.getHeight()
										* yScale);
				g2.fill(drawRect);
				g2.setColor(Color.DARK_GRAY);
				g2.setComposite(oneTenthComposite);
				g2.draw(drawRect);

				g2.setComposite(origComposite);
				g2.setColor(Color.WHITE);
				if (currTiles[x][y].getScore() > 0)
					g2.drawString(String.format("%04.2f", currTiles[x][y]
							.getScore()), (int) drawRect.getX(),
							(int) (drawRect.getY() + drawRect.getHeight()));
			}
		}
		
		g2.setColor(Color.GREEN);
		
		if(this.lines != null)
		{
			Collection<RSSILine> currLines = this.lines;
			for(RSSILine line : currLines){
				float alpha = line.getValue() / 10f;
				if(alpha > 1)
				{
					alpha = 1f;
				}
				g2.setComposite(this.makeComposite(alpha));
				g2.drawLine((int)(line.getLine().x1*xScale), (int)(screenHeight - line.getLine().y1*yScale), (int)(line.getLine().x2*xScale), (int)(screenHeight - line.getLine().y2*yScale));
			}
		}
		g2.setComposite(origComposite);
		
		g2.setColor(origColor);
		

		long endRender = System.currentTimeMillis();
		log.debug("Rendered in {}ms.", endRender - startRender);

	}

	private AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}
}
