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

import java.util.HashMap;

import org.grailrtls.libcommon.util.HashableByteArray;
import org.grailrtls.libcommon.util.NumericUtils;

public class Fingerprint {

    private String fingerprintName;
    private byte physicalLayer;
    private byte[] receiverId;
    private HashMap<HashableByteArray, Float> rssiValues = new HashMap<HashableByteArray, Float>();

    public byte getPhysicalLayer() {
        return physicalLayer;
    }

    public void setPhysicalLayer(byte physicalLayer) {
        this.physicalLayer = physicalLayer;
    }

    public byte[] getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(byte[] transmitterId) {
        this.receiverId = transmitterId;
    }

    public HashMap<HashableByteArray, Float> getRssiValues() {
        return rssiValues;
    }

    public void setRssiValues(HashMap<HashableByteArray, Float> rssiValues) {
        this.rssiValues = rssiValues;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if(this.fingerprintName != null)
        {
            sb.append(this.fingerprintName).append(' ');
        }
        
        sb.append("Fingerprint [").append(physicalLayer).append("] (").append(
                NumericUtils.toHexString(this.receiverId)).append(")");
        for (HashableByteArray receiver : this.rssiValues.keySet()) {
            sb.append("\n\t").append(
                    NumericUtils.toHexString(receiver.getData())).append(": ")
                    .append(this.rssiValues.get(receiver));
        }

        return sb.toString();
    }

    public String getFingerprintName() {
        return fingerprintName;
    }

    public void setFingerprintName(String fingerprintName) {
        this.fingerprintName = fingerprintName;
    }
}
