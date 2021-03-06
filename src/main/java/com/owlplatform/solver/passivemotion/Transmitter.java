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

public class Transmitter {

	private String regionUri;

	private float xLocation;

	private float yLocation;

	private String deviceId;

	public String getRegionUri() {
		return regionUri;
	}

	public void setRegionUri(String regionUri) {
		this.regionUri = regionUri;
	}

	public float getxLocation() {
		return xLocation;
	}

	public void setxLocation(float xLocation) {
		this.xLocation = xLocation;
	}

	public float getyLocation() {
		return yLocation;
	}

	public void setyLocation(float yLocation) {
		this.yLocation = yLocation;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("Transmitter (").append(
				this.deviceId).append(") in \"")
				.append(this.regionUri).append("\" @ (").append(this.xLocation)
				.append(", ").append(this.yLocation).append(")");

		return sb.toString();
	}
}
