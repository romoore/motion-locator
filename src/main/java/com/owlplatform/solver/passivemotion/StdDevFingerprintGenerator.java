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

package com.owlplatform.solver.passivemotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.common.util.NumericUtils;
import com.owlplatform.common.util.OnlineVariance;

public class StdDevFingerprintGenerator {

	private static final Logger log = LoggerFactory
			.getLogger(StdDevFingerprintGenerator.class);

	/**
	 * The maximum age of samples used to generate the fingerprints. Defaults to
	 * 0, which permits unbounded sample age.
	 */
	protected long maxSampleAge = 4000l;

	/**
	 * The maximum number of samples used to generate the fingerprints. Defaults
	 * to 5.
	 */
	protected int maxNumSamples = 5;

	private static class SampleMessageEntry {
		public long receiveTime;

		public SampleMessage sample;
	}

	protected ConcurrentHashMap<HashableByteArray, ConcurrentHashMap<HashableByteArray, OnlineVariance>> varianceByRbyT = new ConcurrentHashMap<HashableByteArray, ConcurrentHashMap<HashableByteArray, OnlineVariance>>();

	public void addSample(SampleMessage sample) {
		HashableByteArray transmitterHash = new HashableByteArray(sample
				.getDeviceId());
		HashableByteArray recHash = new HashableByteArray(sample
				.getReceiverId());
		if (transmitterHash == null || recHash == null)
			return;
		ConcurrentHashMap<HashableByteArray, OnlineVariance> varianceByTxer = this.varianceByRbyT
				.get(recHash);
		if (varianceByTxer == null) {
			varianceByTxer = new ConcurrentHashMap<HashableByteArray, OnlineVariance>();
			this.varianceByRbyT.put(recHash, varianceByTxer);
		}

		OnlineVariance variance = varianceByTxer.get(transmitterHash);

		if (variance == null) {
			variance = new OnlineVariance();
			variance.setAgeGap(this.maxSampleAge);
			variance.setMaxHistory(this.maxNumSamples);
		}

		variance.addValue(sample.getRssi());
	}

	public Fingerprint generateFingerprint(byte[] receiverId) {
		HashableByteArray receiverHash = new HashableByteArray(receiverId);

		Fingerprint fingerprint = new Fingerprint();
		fingerprint.setFingerprintName("Variance");
		fingerprint.setReceiverId(receiverId);
		HashMap<HashableByteArray, Float> rssiValues = new HashMap<HashableByteArray, Float>();

		log.debug("Generating fingerprint for {}.", receiverHash.toString());
		
		ConcurrentHashMap<HashableByteArray, OnlineVariance> recVariances = this.varianceByRbyT.get(receiverHash);

		ConcurrentHashMap<HashableByteArray, OnlineVariance> txSamples = this.varianceByRbyT
				.get(receiverHash);

		if (txSamples == null || txSamples.size() == 0) {
			log.debug("No samples available for {}.", NumericUtils
					.toHexString(receiverId));
			return null;
		}
		long now = System.currentTimeMillis();
		long oldestTime = now - this.maxSampleAge;
//		Fingerprint fingerprint = new Fingerprint();
//		fingerprint.setFingerprintName("StdDev");
//		fingerprint.setTransmitterId(transmitterId);
		boolean setPhy = false;

		// Now compute the Std. Dev. of RSSI values by receiver
		HashMap<HashableByteArray, Float> stdDevRssiValues = new HashMap<HashableByteArray, Float>();
		for (HashableByteArray receiver : txSamples.keySet()) {
			float stdDevRssi = 0f;
			int numRssis = 1;
			float meanRssi = 0f;
			OnlineVariance variance = txSamples
					.get(receiver);
			
			stdDevRssiValues.put(receiver, Float.valueOf(variance.getCurrentVariance()));
		}
		if (stdDevRssiValues.size() == 0) {
			log.debug("No values computed.");
			return null;
		}

		fingerprint.setRssiValues(stdDevRssiValues);
		log.debug("Generated {}", fingerprint);
		return fingerprint;
	}

	public Fingerprint[] generateFingerprints() {
		ArrayList<Fingerprint> fingerprints = new ArrayList<Fingerprint>();

		for (HashableByteArray rxer : this.varianceByRbyT.keySet()) {
			Fingerprint fpt = this.generateFingerprint(rxer.getData());
			if (fpt != null) {
				fingerprints.add(fpt);
			}
		}

		Fingerprint[] fingerprintArray = new Fingerprint[fingerprints.size()];
		return fingerprints.toArray(fingerprintArray);
	}

	public long getMaxSampleAge() {
		return maxSampleAge;
	}

	public void setMaxSampleAge(long maxSampleAge) {
		this.maxSampleAge = maxSampleAge;
	}

	public int getMaxNumSamples() {
		return maxNumSamples;
	}

	public void setMaxNumSamples(int maxNumSamples) {
		this.maxNumSamples = maxNumSamples;
	}

}
