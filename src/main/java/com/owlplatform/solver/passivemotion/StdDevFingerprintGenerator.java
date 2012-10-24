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

  protected ConcurrentHashMap<String, ConcurrentHashMap<String, TimestampedFloat>> varianceByRbyT = new ConcurrentHashMap<String, ConcurrentHashMap<String, TimestampedFloat>>();

  public void addVariance(String transmitter, String receiver, float variance,
      long timestamp) {

    if (transmitter == null || receiver == null)
      return;
    ConcurrentHashMap<String, TimestampedFloat> varianceByTxer = this.varianceByRbyT
        .get(receiver);
    if (varianceByTxer == null) {
      varianceByTxer = new ConcurrentHashMap<String, TimestampedFloat>();
      this.varianceByRbyT.put(receiver, varianceByTxer);
    }

    TimestampedFloat value = varianceByTxer.get(transmitter);
    if(value == null){
      value = new TimestampedFloat(variance,timestamp);
      varianceByTxer.put(transmitter, value);
    }else {
      value.value = variance;
      value.timestamp = timestamp;
    }
    
  }

  public Fingerprint generateFingerprint(String receiverId) {

    Fingerprint fingerprint = new Fingerprint();
    fingerprint.setFingerprintName("Variance");
    fingerprint.setReceiverId(receiverId);
    HashMap<String, Float> rssiValues = new HashMap<String, Float>();

    log.debug("Generating fingerprint for {}.", receiverId);

    ConcurrentHashMap<String, TimestampedFloat> receiverVariance = this.varianceByRbyT
        .get(receiverId);

    if (receiverVariance == null || receiverVariance.size() == 0) {
      log.debug("No samples available for {}.", receiverId);
      return null;
    }
    long now = System.currentTimeMillis();
    long oldestTime = now - this.maxSampleAge;
    // Fingerprint fingerprint = new Fingerprint();
    // fingerprint.setFingerprintName("StdDev");
    // fingerprint.setTransmitterId(transmitterId);
    boolean setPhy = false;

    // Now compute the Std. Dev. of RSSI values by receiver
    HashMap<String, Float> stdDevRssiValues = new HashMap<String, Float>();
    for (String receiver : receiverVariance.keySet()) {
      float stdDevRssi = 0f;
      int numRssis = 1;
      float meanRssi = 0f;
      TimestampedFloat variance = receiverVariance.get(receiver);
      if(variance.timestamp < oldestTime){
        receiverVariance.remove(receiver);
        continue;
      }
      stdDevRssiValues.put(receiver,
          Float.valueOf(variance.value));
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

    for (String rxer : this.varianceByRbyT.keySet()) {
      Fingerprint fpt = this.generateFingerprint(rxer);
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
