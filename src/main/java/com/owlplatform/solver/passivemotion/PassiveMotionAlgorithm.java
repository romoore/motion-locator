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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.attribute.standard.Finishings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.common.util.NumericUtils;
import com.owlplatform.solver.passivemotion.StdDevFingerprintGenerator;

public class PassiveMotionAlgorithm {

  private static final Logger log = LoggerFactory
      .getLogger(PassiveMotionAlgorithm.class);

  private static final char[] MOTION_SYMBOLS = { ' ', '.', ':', '+', '#' };

  protected String regionUri;

  protected float regionXMax;

  protected float regionYMax;

  protected int numXTiles;

  protected int numYTiles;

  protected AlgorithmConfig config;

  protected ConcurrentHashMap<String, Receiver> receivers = new ConcurrentHashMap<String, Receiver>();

  protected ConcurrentHashMap<String, Transmitter> transmitters = new ConcurrentHashMap<String, Transmitter>();

  protected ScoredTile[] tiles;

  protected StdDevFingerprintGenerator stdDevFingerprinter = new StdDevFingerprintGenerator();

  /*
  static float[][] tileFilterKernel3x3a = new float[3][3];
  static float[][] tileFilterKernel3x3b = new float[3][3];
  static float[][] tileFilterKernel3x3c = new float[3][3];
  static float[][] tileFilterKernel5x5a = new float[5][5];
  static {
    tileFilterKernel3x3a[0] = new float[] { -.25f, -.25f, -.25f };
    tileFilterKernel3x3a[1] = new float[] { -.25f, 2.0f, -.25f };
    tileFilterKernel3x3a[2] = new float[] { -.25f, -.25f, -.25f };

    tileFilterKernel3x3b[0] = new float[] { 0.05f, 0.05f, 0.05f };
    tileFilterKernel3x3b[1] = new float[] { 0.05f, 0.25f, 0.05f };
    tileFilterKernel3x3b[2] = new float[] { 0.05f, 0.05f, 0.05f };

    tileFilterKernel3x3c[0] = new float[] { -.25f, -.15f, -.25f };
    tileFilterKernel3x3c[1] = new float[] { -.15f, 2f, -.15f };
    tileFilterKernel3x3c[2] = new float[] { -.25f, -.15f, -.25f };

    tileFilterKernel5x5a[0] = new float[] { -1f, -1f, -1f, -1f, -1f };
    tileFilterKernel5x5a[1] = new float[] { -1f, 2f, 2f, 2f, -1f };
    tileFilterKernel5x5a[2] = new float[] { -1f, 2f, 2f, 2f, -1f };
    tileFilterKernel5x5a[3] = new float[] { -1f, 2f, 2f, 2f, -1f };
    tileFilterKernel5x5a[4] = new float[] { -1f, -1f, -1f, -1f, -1f };
  }

  private float[][] userKernel = new float[][] { { -.1f, -.1f, -.1f, -.1f, -.1f }, 
                                                    { -.1f, -.2f, -.2f, -.2f, -.1f },
                                                    { -.1f, -.2f, 2.0f, -.2f, -.1f },
                                                    { -.1f, -.2f, -.2f, -.2f, -.1f},
                                                    { -.1f, -.1f, -.1f, -.1f, -.1f} };
  */
  public PassiveMotionAlgorithm(AlgorithmConfig config) {
    super();
    this.config = config;
    this.stdDevFingerprinter.setMaxNumSamples(3);
    this.stdDevFingerprinter.setMaxSampleAge(5000l);
  }

  public void addVariance(final String receiver, final String transmitter,
      final float variance, final long timestamp) {
    // log.debug("{}/{}: {}",transmitter,receiver,variance);
    this.stdDevFingerprinter.addVariance(transmitter, receiver, variance,
        timestamp);
  }

  public String getRegionId() {
    return regionUri;
  }

  public void setRegionUri(String regionUri) {
    this.regionUri = regionUri;
  }

  public float getRegionXMax() {
    return regionXMax;
  }

  public void setRegionXMax(float regionXMax) {
    this.regionXMax = regionXMax;
  }

  public float getRegionYMax() {
    return regionYMax;
  }

  public void setRegionYMax(float regionYMax) {
    this.regionYMax = regionYMax;
  }

  public int getNumXTiles() {
    return numXTiles;
  }

  public void setNumXTiles(int numXTiles) {
    this.numXTiles = numXTiles;
  }

  public int getNumYTiles() {
    return numYTiles;
  }

  public void setNumYTiles(int numYTiles) {
    this.numYTiles = numYTiles;
  }

  public ScoredTile[] getTiles() {
    return tiles;
  }

  public void setTiles(ScoredTile[] tiles) {
    this.tiles = tiles;
  }

  public void addReceiver(Receiver receiver) {

    this.receivers.put(receiver.getDeviceId(), receiver);
    log.debug("Added {}", receiver);
  }

  public void addTransmitter(Transmitter transmitter) {

    this.transmitters.put(transmitter.getDeviceId(), transmitter);
    log.debug("Added {}", transmitter);
  }

  public FilteredTileResultSet generateResults() {
    if (this.regionXMax == 0 || this.regionYMax == 0) {
      return null;
    }

    if (this.config.desiredTileWidth > 0) {
      this.numXTiles = (int) Math.ceil(this.regionXMax
          / this.config.desiredTileWidth);
      this.numXTiles += (this.numXTiles - 1);
    }
    if (this.config.desiredTileHeight > 0) {
      this.numYTiles = (int) Math.ceil(this.regionYMax
          / this.config.desiredTileWidth);
      this.numYTiles += (this.numYTiles - 1);
    }

    FilteredTileResultSet resultSet = new FilteredTileResultSet();

    // Calculate fingerprints
    ArrayList<Fingerprint> fingerprints = this.calculateFingerprints();

    // Create RSSI lines
    ArrayList<RSSILine> allLines = this.createRSSILines(fingerprints);

    resultSet.setLines(allLines);

    FilteredTileResult result;

    ScoredTile[][] baseRaw = this.createUnscoredTiles();
    result = new FilteredTileResult();
    result.setTiles(baseRaw);
    resultSet.setTiles("base-raw-0", result);

    ScoredTile[][] finalTiles = this.createUnscoredTiles();
    result.setTiles(finalTiles);

    int tileRound = 0;

    ArrayList<ScoredTile> totalTiles = new ArrayList<ScoredTile>();
    ArrayList<ScoredTile> tempTiles = this.calculateTileScores(baseRaw,
        allLines);
    // Now seek-out the maximum area and any neighbors adhering to the
    // configuration

    List<RSSILine> remainLines = new ArrayList<RSSILine>();
    remainLines.addAll(allLines);
    while (tempTiles != null && !tempTiles.isEmpty()) {

      ++tileRound;
      this.mergeTiles(finalTiles, baseRaw);
      for (ScoredTile t : tempTiles) {
        totalTiles.add(t.clone());
      }

      this.removeLines(remainLines, tempTiles);
      if (remainLines.isEmpty()) {
        break;
      }
      tempTiles = this.calculateTileScores(baseRaw, remainLines);
      if (tempTiles != null && !tempTiles.isEmpty()) {
        result = new FilteredTileResult();
        result.setTiles(this.cloneTiles(baseRaw));
        resultSet.setTiles("base-raw-" + (++tileRound), result);
      }
    }

    log.debug("Detected {} areas of motion.", Integer.valueOf(tileRound));
    log.info("\n" + this.printFancyMap(finalTiles));
    if (!totalTiles.isEmpty()) {
      resultSet.setTilesToPublish(totalTiles);
    }

    return resultSet;

  }

  /**
   * Merges the highest scores of the two tile sets and updates
   * {@code tilesToUpdate}.
   */
  protected void mergeTiles(ScoredTile[][] tilesToUpdate,
      ScoredTile[][] tilesToSource) {
    for (int x = 0; x < tilesToUpdate.length; ++x) {
      for (int y = 0; y < tilesToUpdate[0].length; ++y) {
        tilesToUpdate[x][y].setScore(Math.max(tilesToUpdate[x][y].getScore(),
            tilesToSource[x][y].getScore()));
      }
    }
  }

  protected ScoredTile[][] cloneTiles(ScoredTile[][] origTiles) {
    if (origTiles == null || origTiles.length == 0 || origTiles[0] == null
        || origTiles[0].length == 0) {
      return null;
    }

    ScoredTile[][] clone = new ScoredTile[origTiles.length][origTiles[0].length];
    for (int x = 0; x < origTiles.length; ++x) {
      for (int y = 0; y < origTiles[0].length; ++y) {
        clone[x][y] = origTiles[x][y].clone();
      }
    }

    return clone;
  }

  protected void applyHighPass(final ScoredTile[][] tiles, final float minScore) {
    for (int i = 0; i < tiles.length; ++i) {
      for (int j = 0; j < tiles[i].length; ++j) {
        if (tiles[i][j].getScore() <= minScore) {
          tiles[i][j].setScore(0f);
        }
      }
    }
  }

  protected ScoredTile[][] createMicroTiles(ScoredTile[][] macroTiles) {
    ScoredTile[][] microTiles = new ScoredTile[macroTiles.length + 1][];
    for (int i = 0; i < microTiles.length; ++i)
      microTiles[i] = new ScoredTile[macroTiles[0].length + 1];

    for (int x = 0; x < microTiles.length; ++x) {
      for (int y = 0; y < microTiles[x].length; ++y) {
        microTiles[x][y] = new ScoredTile();
        microTiles[x][y].setScore(0f);
        Rectangle2D dimensions = new Rectangle2D.Float();

        // Check odd or even x, y
        // even means first microtile
        // odd mean second microtile

        int macroX = x;
        int macroY = y;
        if (x >= macroTiles.length) {
          macroX = macroTiles.length - 1;
        }
        if (y >= macroTiles[macroX].length) {
          macroY = macroTiles[macroX].length - 1;
        }

        double newX = macroTiles[macroX][macroY].getTile().getX();
        double newY = macroTiles[macroX][macroY].getTile().getY();
        double newWidth = macroTiles[macroX][macroY].getTile().getWidth() / 2;
        double newHeight = macroTiles[macroX][macroY].getTile().getHeight() / 2;

        if (x == microTiles.length - 1) {
          newX += newWidth;
        }
        if (y == microTiles[x].length - 1) {
          newY += newHeight;
        }

        microTiles[x][y].setTile(new Rectangle2D.Float((float) newX,
            (float) newY, (float) newWidth, (float) newHeight));

        float tileScore = 0;

        if (x < macroTiles.length - 1 && y < macroTiles[x].length) {
          tileScore += macroTiles[x][y].getScore();
        }
        if (x - 1 >= 0 && y < macroTiles[x - 1].length) {
          tileScore += macroTiles[x - 1][y].getScore();
        }
        if (x < macroTiles.length && y - 1 >= 0) {
          tileScore += macroTiles[x][y - 1].getScore();
        }
        if (x - 1 >= 0 && y - 1 >= 0) {
          tileScore += macroTiles[x - 1][y - 1].getScore();
        }
        // microTiles[x][y].setScore((float)Math.log10(tileScore));
        microTiles[x][y].setScore(tileScore);
        if (Float.isInfinite(microTiles[x][y].getScore())) {
          microTiles[x][y].setScore(0);
        }
        if (microTiles[x][y].getScore() < 0) {
          microTiles[x][y].setScore(0);
        }

      }
    }

    return microTiles;
  }

  /**
   * Applies a kernel to a set of tiles.
   * 
   * @param kernel
   *          the kernel to apply.
   * @param inTiles
   *          the tiles to apply the kernel to.
   * @param outTiles
   *          the tiles after applying the kernel
   * @return a list of scored tiles that have a non-zero score.
   */
  protected ArrayList<ScoredTile> applyKernel(final float[][] kernel,
      final ScoredTile[][] inTiles, ScoredTile[][] outTiles) {
    ArrayList<ScoredTile> solutionTiles = new ArrayList<ScoredTile>();
    if (outTiles.length != inTiles.length) {
      throw new IllegalArgumentException(
          "Must provide same-sized arrays for inTiles and outTiles.");
    }

    float kernelSum = 0;
    for (int i = 0; i < kernel.length; ++i) {
      for (int j = 0; j < kernel[i].length; ++j) {
        kernelSum += kernel[i][j];
      }
    }

    // Assume kernel to be square, and really 3x3 right now
    int kernelMidX = kernel.length / 2;
    int kernelMidY = kernel[0].length / 2;
    for (int x = 0; x < inTiles.length; ++x) {
      if (outTiles[x].length != inTiles[x].length) {
        throw new IllegalArgumentException(
            "Must provide same-sized arrays for inTiles and outTiles.");
      }

      for (int y = 0; y < inTiles[x].length; ++y) {
        outTiles[x][y].setScore(0f);

        for (int i = 0; i < kernel.length; ++i) {
          // allTiles[x + (i - kernelMidX)][] * kernel[i][]
          for (int j = 0; j < kernel[i].length; ++j) {
            int tilesX = x + i - kernelMidX;
            int tilesY = y + j - kernelMidY;

            // Extend edge cells out to infinity
            if (tilesX < 0) {
              tilesX = 0;
            }
            if (tilesX >= inTiles.length) {
              tilesX = inTiles.length - 1;
            }

            if (tilesY < 0) {
              tilesY = 0;
            }
            if (tilesY >= inTiles[x].length) {
              tilesY = inTiles[x].length - 1;
            }

            outTiles[x][y].score += inTiles[tilesX][tilesY].getScore()
                * kernel[i][j];
          }
        }

        // if (Math.abs(kernelSum) > 1) {
        // outTiles[x][y].score /= kernelSum;
        // }

        if (outTiles[x][y].getScore() < 0) {
          outTiles[x][y].setScore(0f);
        }
        // if(outTiles[x][y].getScore() < this.tileScoreThreshold)
        // {
        // outTiles[x][y].setScore(0f);
        // }

        if (outTiles[x][y].getScore() > 0) {
          solutionTiles.add(outTiles[x][y].clone());
        }

      }
    }

    return solutionTiles;
  }

  /**
   * Calculates tile scores based on the intersecting lines. This method will
   * modify the scores of the allTiles parameter.
   * 
   * @param allTiles
   * @param allLines
   * @return an ArrayList of ScoredTile objects that have non-zero scores.
   */
  protected ArrayList<ScoredTile> calculateTileScores(
      final ScoredTile[][] allTiles, final Collection<RSSILine> allLines) {
    ArrayList<ScoredTile> solutionTiles = new ArrayList<ScoredTile>();
    // Calculate raw scores for each tile
    for (int x = 0; x < allTiles.length; ++x) {
      for (int y = 0; y < allTiles[x].length; ++y) {
        // Number of terminating lines in this tile
        int numTerminating = 0;
        // Clear the score to 0
        allTiles[x][y].setScore(0);
        for (RSSILine line : allLines) {
          // Make sure line isn't too far away
          Rectangle2D.Float theTile = allTiles[x][y].getTile();
          if (theTile.contains(line.getLine().getP1())) {
            ++numTerminating;
          }
          if (theTile.contains(line.getLine().getP2())) {
            ++numTerminating;
          }
          Point2D.Float tileCenter = new Point2D.Float(theTile.x
              + theTile.width / 2, theTile.y + theTile.height / 2);
          double d1 = Math.sqrt(Math.pow(tileCenter.x - line.getLine().x1, 2)
              + Math.pow(tileCenter.y - line.getLine().y1, 2));

          // Check P1 distance (receiver)
          if (d1 > this.config.radiusThreshold) {
            continue;
          }
          double d2 = Math.sqrt(Math.pow(tileCenter.x - line.getLine().x2, 2)
              + Math.pow(tileCenter.y - line.getLine().y2, 2));
          // Check P2 distance (transmitter)
          if (d2 > this.config.radiusThreshold) {
            continue;
          }

          float lineLength = (float) Math.sqrt(Math.pow(line.getLine().x1
              - line.getLine().x2, 2)
              + Math.pow(line.getLine().y1 - line.getLine().y2, 2));
          if (lineLength < this.config.linkMinDistance) {
            continue;
          }

          // Check intersection
          if (!line.getLine().intersects(theTile)) {
            continue;
          }

          float numerator = line.getValue() - this.config.stdDevNoiseThreshold;
          allTiles[x][y].score += (float) (numerator / (Math.pow(lineLength,
              this.config.lineLengthPower)));

        }
        // if(numTerminating > 1){
        // allTiles[x][y].score = allTiles[x][y].score /
        // (float)Math.sqrt(numTerminating);
        // }

        // allTiles[x][y].score = allTiles[x][y].score *100f /
        // (allTiles[x][y].tile.height*allTiles[x][y].tile.width);

        // Make sure the tile score is above the threshold
        if (allTiles[x][y].getScore() <= this.config.tileScoreThreshold) {
          // log.debug("{} score is below threshold {}.  Skipping...",allTiles[x][y],this.tileScoreThreshold);
          allTiles[x][y].setScore(0f);
          continue;
        }

        solutionTiles.add(allTiles[x][y]);

      }
    }

    this.findMaxAreas(allTiles);

    for (Iterator<ScoredTile> iter = solutionTiles.iterator(); iter.hasNext();) {
      ScoredTile t = iter.next();
      if (t.getScore() < this.config.tileScoreThreshold) {
        iter.remove();
      }
    }

    return solutionTiles;
  }

  protected ArrayList<RSSILine> createRSSILines(
      final Collection<Fingerprint> fingerprints) {
    // Create RSSI lines
    ArrayList<RSSILine> allLines = new ArrayList<RSSILine>();

    for (Fingerprint fingerprint : fingerprints) {
      // FIXME: What the hell is this? Left over from old code, but is it
      // receiver or transmitter?
      Receiver receiver = this.receivers.get(fingerprint.getReceiverId());
      if (receiver == null) {
        log.debug("Unknown receiver {}. Skipping...",
            fingerprint.getReceiverId());
        continue;
      }

      for (String transmitterId : fingerprint.getRssiValues().keySet()) {
        RSSILine line = new RSSILine();
        Transmitter transmitter = this.transmitters.get(transmitterId);
        if (transmitter == null) {
          continue;
        }

        Float value = fingerprint.getRssiValues().get(transmitterId);
        if (value == null) {
          log.warn("Missing Std. Dev. Value for Tx: {}, Rx: {}. Skipping...",
              fingerprint.getReceiverId(), transmitter.getDeviceId());
          continue;
        }
        if (value.floatValue() <= this.config.stdDevNoiseThreshold) {
          continue;
        }
        Line2D.Float theLine = new Line2D.Float();
        theLine.setLine(receiver.getxLocation(), receiver.getyLocation(),
            transmitter.getxLocation(), transmitter.getyLocation());

        line.setLine(theLine);
        line.setReceiver(receiver);
        line.setTransmitter(transmitter);
        line.setValue(value.floatValue());
        allLines.add(line);

      }
    }

    log.debug("Generated {} lines.", allLines.size());
    return allLines;
  }

  protected void findMaxAreas(ScoredTile[][] tiles) {
    float maxVal = -1;
    int maxTileX = 0;
    int maxTileY = 0;
    // Determine max value
    for (int x = 0; x < tiles.length; ++x) {
      for (int y = 0; y < tiles[0].length; ++y) {
        if (tiles[x][y].getScore() > maxVal) {
          maxVal = tiles[x][y].getScore();
          maxTileX = x;
          maxTileY = y;
        }
      }
    }

    log.debug("MAX VALUE: {}", maxVal);

    float minScore = maxVal * this.config.peakRatio;
    // Remove tiles below half of max
    for (int x = 0; x < tiles.length; ++x) {
      for (int y = 0; y < tiles[0].length; ++y) {
        if (tiles[x][y].getScore() < minScore) {
          tiles[x][y].setScore(0);
        }
      }
    }

    // Now seek out the maxTile's neighbors, seeking "gradual" reductions
    // nearby, trimming significant drops in score
    if (maxVal > 0) {
      this.trimNeighbors(tiles, maxTileX, maxTileY, maxVal, (byte) (MASK_N
          | MASK_S | MASK_E | MASK_W));
    }
  }

  private static final byte MASK_N = 0x08;
  private static final byte MASK_S = 0x04;
  private static final byte MASK_E = 0x02;
  private static final byte MASK_W = 0x01;

  /*
   * Direction = NSEW (bitfield)
   * NE = 1010
   * SW = 0101
   * N  = 1000
   * S  = 0100
   */
  protected void trimNeighbors(ScoredTile[][] tiles, int x, int y,
      float prevNeighborScore, byte direction) {
    float currScore = tiles[x][y].getScore();

    if (prevNeighborScore < 0.01 || currScore > prevNeighborScore
        || currScore < prevNeighborScore * this.config.neighborRatio) {

      tiles[x][y].setScore(0);
      currScore = 0;
    }
    // float nScore = currScore * this.config.neighborRatio;
    // Can only go north if north bit set
    if ((direction & MASK_N) != 0 && y < tiles[x].length - 1) {
      // Can only go NW if north + west bits set
      if ((direction & MASK_W) != 0 && x > 0) {
        trimNeighbors(tiles, x - 1, y + 1, currScore, (byte) (MASK_N | MASK_W));
      }
      // Can only go NE if north+east bits set
      if ((direction & MASK_E) != 0 && x < tiles.length - 1) {
        trimNeighbors(tiles, x + 1, y + 1, currScore, (byte) (MASK_N | MASK_E));
      }
      // Go north
      trimNeighbors(tiles, x, y + 1, currScore, MASK_N);
    }
    // Can only go south if south bit set
    if ((direction & MASK_S) != 0 && y > 0) {
      if ((direction & MASK_W) != 0 && x > 0) {
        trimNeighbors(tiles, x - 1, y - 1, currScore, (byte) (MASK_S | MASK_W));
      }
      if ((direction & MASK_E) != 0 && x < tiles.length - 1) {
        trimNeighbors(tiles, x + 1, y - 1, currScore, (byte) (MASK_S | MASK_E));
      }
      trimNeighbors(tiles, x, y - 1, currScore, MASK_S);
    }
    if ((direction & MASK_E) != 0 && x < tiles.length - 1) {
      trimNeighbors(tiles, x + 1, y, currScore, MASK_E);
    }
    if ((direction & MASK_W) != 0 && x > 0) {
      trimNeighbors(tiles, x - 1, y, currScore, MASK_W);
    }
  }

  protected ArrayList<Fingerprint> calculateFingerprints() {
    ArrayList<Fingerprint> fingerprints = new ArrayList<Fingerprint>();
    for (Receiver receiver : this.receivers.values()) {
      Fingerprint fingerprint = this.stdDevFingerprinter
          .generateFingerprint(receiver.getDeviceId());
      if (fingerprint != null) {
        fingerprints.add(fingerprint);
      } else {
      }
    }

    log.debug("Generated {}/{} fingerprints.", fingerprints.size(),
        this.transmitters.size());

    return fingerprints;
  }

  protected ScoredTile[][] createUnscoredTiles() {
    // Create unscored tiles
    ScoredTile[][] allTiles = new ScoredTile[this.numXTiles][this.numYTiles];

    float tileXStep = this.getRegionXMax() / (this.numXTiles + 1.0f);
    float tileYStep = this.getRegionYMax() / (this.numYTiles + 1.0f);

    for (int x = 0; x < allTiles.length; ++x) {
      for (int y = 0; y < allTiles[x].length; ++y) {
        ScoredTile theTile = new ScoredTile();
        Rectangle2D.Float dimensions = new Rectangle2D.Float();
        dimensions.setRect(tileXStep * x, tileYStep * y, tileXStep * 2f,
            tileYStep * 2f);
        theTile.setTile(dimensions);
        theTile.setScore(0f);
        allTiles[x][y] = theTile;
      }
    }

    log.debug("Created tiles {} x {}", allTiles.length, allTiles[0].length);
    return allTiles;
  }

  protected List<RSSILine> removeLines(final List<RSSILine> origLines,
      final List<ScoredTile> origTiles) {
    ArrayList<RSSILine> returnedLines = new ArrayList<RSSILine>();
    returnedLines.addAll(origLines);

    for (Iterator<RSSILine> iter = origLines.iterator(); iter.hasNext();) {
      RSSILine l = iter.next();
      for (ScoredTile t : origTiles) {
        if (t.tile.intersectsLine(l.getLine())) {
          iter.remove();
          break;
        }
      }
    }

    return returnedLines;
  }

  private final String printScoreMap(ScoredTile[][] allTiles) {
    StringBuffer sb = new StringBuffer();
    for (int y = allTiles[0].length - 1; y >= 0; --y) {
      for (int x = 0; x < allTiles.length; ++x) {
        sb.append(String.format("[%05.2f]", allTiles[x][y].score));
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  private final String printFancyMap(ScoredTile[][] allTiles) {
    StringBuffer sb = new StringBuffer();
    sb.append('+');
    for (int x = 0; x < allTiles.length; ++x) {
      sb.append("--");
    }
    sb.append("+\n");
    for (int y = allTiles[0].length - 1; y >= 0; --y) {
      sb.append('|');
      for (int x = 0; x < allTiles.length; ++x) {
        char motionSymbol = MOTION_SYMBOLS[0];
        float score = allTiles[x][y].getScore();
        if (score > 2f * this.config.tileScoreThreshold) {
          motionSymbol = MOTION_SYMBOLS[4];
        } else if (score > 1.66f * this.config.tileScoreThreshold) {
          motionSymbol = MOTION_SYMBOLS[3];
        } else if (score > 1.33f * this.config.tileScoreThreshold) {
          motionSymbol = MOTION_SYMBOLS[2];
        } else if (score > this.config.tileScoreThreshold) {
          motionSymbol = MOTION_SYMBOLS[1];
        }
        sb.append(motionSymbol).append(motionSymbol);
      }
      if (y == 0) {
        sb.append(String.format("| %4.2f\n", allTiles[0][0].getTile().height));
      } else {
        sb.append("|\n");
      }
    }
    sb.append('+');
    for (int x = 0; x < allTiles.length; ++x) {
      sb.append("--");
    }
    sb.append("+\n");
    sb.append(String.format(" %4.2f\n", allTiles[0][0].getTile().width));
    return sb.toString();
  }

  public StdDevFingerprintGenerator getStdDevFingerprinter() {
    return stdDevFingerprinter;
  }

  public void setStdDevFingerprinter(
      StdDevFingerprintGenerator stdDevFingerprinter) {
    this.stdDevFingerprinter = stdDevFingerprinter;
  }
  /*
    public float[][] getCustomKernel() {
      return this.userKernel;
    }

    public void setCustomKernel(float[][] customKernel) {
      this.userKernel = customKernel;
    }
    */
}
