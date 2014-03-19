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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.owlplatform.solver.passivemotion.FilteredTileResult;
import com.owlplatform.solver.passivemotion.FilteredTileResultSet;

public class GraphicalUserInterface extends JFrame implements
    UserInterfaceAdapter {

  protected final TileViewPanel tilePanel = new TileViewPanel();

  protected JTabbedPane tabbedPane = new JTabbedPane();

  // protected KernelPanel customKernelPanel = new KernelPanel();

  protected BufferedImage backgroundImage = null;

  protected final ExecutorService workers = Executors
      .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public GraphicalUserInterface() {
    super();
    this.tabbedPane.add("main", this.tilePanel);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setPreferredSize(new Dimension(800, 600));
    this.setLayout(new BorderLayout());
    this.add(BorderLayout.CENTER, this.tabbedPane);
    JPanel flowPanel = new JPanel(new FlowLayout());
    // flowPanel.add(this.customKernelPanel);
    this.add(BorderLayout.SOUTH, flowPanel);
    this.pack();
    this.setVisible(true);
  }

  /*
  public void setCustomKernel(float[][] kernel){
    this.customKernelPanel.setKernel(kernel);
  }
  */

  @Override
  public void solutionGenerated(final FilteredTileResultSet tileSet) {

    if (tileSet == null) {
      return;
    }
    this.workers.execute(new Runnable() {

      @Override
      public void run() {
        GraphicalUserInterface.this.tilePanel.clearTiles();
        for (String desc : tileSet.getResults().keySet()) {

          GraphicalUserInterface.this.tilePanel.setLines(tileSet.getLines());

          FilteredTileResult res = tileSet.getResult(desc);
          if (res != null && res.getTiles() != null) {
            GraphicalUserInterface.this.tilePanel.setTiles(desc, res.getTiles());
          }

        }
      }
    });
  }

  @Override
  public void setBackground(BufferedImage backgroundImage) {
    this.backgroundImage = backgroundImage;

    this.tilePanel.setBackgroundImage(backgroundImage);

  }

}
