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

import java.awt.GridLayout;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.solver.passivemotion.PassiveMotionAlgorithm;

public class KernelPanel extends JPanel implements ChangeListener{

	private static final Logger log = LoggerFactory.getLogger(KernelPanel.class);
	
	/**
	 * A kernel that does nothing.
	 */
	public static final float[][] DEFAULT_KERNEL = {{0, 0, 0},
													{0, 1, 0},
													{0, 0, 0}};
	
	protected final ConcurrentHashMap<String, PassiveMotionAlgorithm> algorithmsByName = new ConcurrentHashMap<String, PassiveMotionAlgorithm>();
	
	protected float[][] kernel = DEFAULT_KERNEL;
	
	protected JSpinner[][] spinners;
	
	public KernelPanel()
	{
		super();
		
		this.setLayout(new GridLayout(this.kernel.length, this.kernel[0].length));
		this.spinners = new JSpinner[this.kernel.length][this.kernel[0].length];
		for(int i = 0; i < this.kernel.length; ++i)
		{
		  
			for(int j = 0; j < this.kernel[i].length; ++j)
			{
				this.spinners[i][j] = new JSpinner(new SpinnerNumberModel(0.0, -10, 10, 0.1));
				this.add(spinners[i][j]);
				this.spinners[i][j].addChangeListener(this);
			}
		}
		
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		
		// Linear search of spinners for source
		for(int i = 0; i < this.spinners.length; ++i)
		{
			for(int j = 0; j < this.spinners[i].length; ++j)
			{
				if(arg0.getSource() == this.spinners[i][j])
				{
					this.kernel[i][j] = ((Double)((JSpinner)arg0.getSource()).getValue()).floatValue();
					break;
				}
			}
		}
	}
	
	public float[][] getKernel()
	{
		return this.kernel;
	}

  public void setKernel(float[][] kernel) {
    this.kernel = kernel;
  }
	
	
}
