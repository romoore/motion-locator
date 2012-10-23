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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.solver.passivemotion.gui.panels.GraphicalUserInterface;
import com.owlplatform.solver.passivemotion.gui.panels.UserInterfaceAdapter;
import com.owlplatform.worldmodel.Attribute;
import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.client.ClientWorldModelInterface;
import com.owlplatform.worldmodel.client.StepResponse;
import com.owlplatform.worldmodel.client.WorldState;
import com.owlplatform.worldmodel.client.protocol.messages.DataResponseMessage;
import com.owlplatform.worldmodel.client.protocol.messages.IdSearchResponseMessage;
import com.owlplatform.worldmodel.client.protocol.messages.SnapshotRequestMessage;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage.AttributeSpecification;
import com.owlplatform.worldmodel.types.DataConverter;
import com.owlplatform.worldmodel.types.DoubleConverter;
import com.owlplatform.worldmodel.types.StringConverter;
import com.thoughtworks.xstream.XStream;

public class PassiveMotionSolver extends Thread {
  private static final class VarianceHandler extends Thread {
  
    private final PassiveMotionSolver handler;
    private boolean keepRunning = true;
  
    public VarianceHandler(final PassiveMotionSolver handler) {
      this.handler = handler;
    }
  
    @Override
    public void run() {
      main: while (this.keepRunning) {
        log.info("Requesting RSSI variance values.");
        if (this.handler.clientWM == null) {
          log.info("Variance Handler exiting.");
  
          break;
        }
        final StepResponse rssiResponse = this.handler.clientWM.getStreamRequest(
            ".*", System.currentTimeMillis(), 0, "link variance");
  
        WorldState state = null;
        while (!rssiResponse.isComplete() && !rssiResponse.isError()
            && this.keepRunning) {
          try {
            state = rssiResponse.next();
  
            if (state == null) {
              break;
            }
            for (String uri : state.getIdentifiers()) {
  
              int txSensStart = uri.indexOf('.');
              int rxSensStart = uri.lastIndexOf('.');
              String txerSensor = uri.substring(txSensStart + 1, rxSensStart);
              String rxerSensor = uri.substring(rxSensStart + 1);
              Collection<Attribute> attribs = state.getState(uri);
              if (attribs == null || !attribs.iterator().hasNext()) {
                continue;
              }
              Attribute linkAvg = attribs.iterator().next();
              double value = DoubleConverter.get()
                  .decode(linkAvg.getData());
              if (this.handler.algorithm != null) {
                this.handler.algorithm.addVariance(rxerSensor, txerSensor,
                    (float) value, linkAvg.getCreationDate());
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
            continue main;
          }
        }
        rssiResponse.cancel();
      }
    }
  
    public void shutdown() {
      this.keepRunning = false;
    }
  }

  private static final Logger log = LoggerFactory
      .getLogger(PassiveMotionSolver.class);

  /**
   * The name of the attribute this solver will produce.
   */
  public static final String GENERATED_ATTRIBUTE_NAME = "passive motion.tile";

  /**
   * The name of this solver.
   */
  public static final String SOLVER_ORIGIN_STRING = "java.solver.passive_motion.v2";

  /**
   * How often to send solutions (if they are available).
   */
  public static final long UPDATE_FREQUENCY = 1000l;

  /**
   * For producing the passive motion results.
   */
  private PassiveMotionAlgorithm algorithm;;

  /**
   * For displaying results to the user in realtime (optional).
   */
  protected UserInterfaceAdapter userInterface = null;

  /**
   * URL of the region image.
   */
  protected String regionImageUrl = null;

  /**
   * Image of the region (for the GUI).
   */
  protected BufferedImage regionImage = null;

  /**
   * Connection to the world model as a solver.
   */
  protected final SolverWorldConnection solverWM = new SolverWorldConnection();

  /**
   * Connection to the world model as a client.
   */
  protected final ClientWorldConnection clientWM = new ClientWorldConnection();

  /**
   * Accepts 4 required parameters and launches a new solver thread.
   * 
   * @param args
   *          world model host, solver port, client port, region name, algorithm
   *          config.
   */
  public static void main(String[] args) {

    if (args.length < 5) {
      printUsageInfo();
      return;
    }

    XStream xstream = new XStream();

    AlgorithmConfig config = (AlgorithmConfig) xstream
        .fromXML(new File(args[4]));

    PassiveMotionSolver solver = new PassiveMotionSolver(args[0],
        Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3], config);

    if (args.length > 4) {
      for (int i = 4; i < args.length; ++i) {
        if (args[i].equals("--gui")) {
          solver.setUserInterface(new GraphicalUserInterface());
        }
      }
    }

    solver.start();
  }

  public PassiveMotionSolver(String wmHost, int solverPort, int clientPort,
      String region, AlgorithmConfig config) {

    this.solverWM.setHost(wmHost);
    this.solverWM.setPort(solverPort);
    AttributeSpecification spec = new AttributeSpecification();
    spec.setAttributeName(GENERATED_ATTRIBUTE_NAME);
    spec.setIsOnDemand(false);
    this.solverWM.addAttribute(spec);

    this.clientWM.setHost(wmHost);
    this.clientWM.setPort(clientPort);

    // Configure the fingerprinter
    StdDevFingerprintGenerator fingerprinter = new StdDevFingerprintGenerator();
    fingerprinter.setMaxNumSamples(3);
    fingerprinter.setMaxSampleAge(5000l);

    this.algorithm = new PassiveMotionAlgorithm(config);
    this.algorithm.setStdDevFingerprinter(fingerprinter);
    this.algorithm.setRegionUri(region);
  }

  public void run() {

    // Connect to aggregator, distributor, and world server
    if (!this.startConnections()) {
      log.error("Unable to connect to the world model.");
      return;
    }

    if (!this.launchWorkers()) {
      this.shutdown();
      return;
    }

    long lastUpdateTime = 0l;
    while (true) {
      long now = System.currentTimeMillis();
      if (now - lastUpdateTime > 1000l) {
        FilteredTileResultSet resultSet = this.algorithm.generateResults();
        if (this.userInterface != null) {
          this.userInterface.solutionGenerated(resultSet);
        }
        if (resultSet != null && resultSet.getTilesToPublish() != null
            && resultSet.getTilesToPublish().size() > 0) {
          Collection<ScoredTile> tiles = resultSet.getTilesToPublish();

          ByteBuffer solutionBytes = ByteBuffer.allocate(tiles.size() * 20);
          for (ScoredTile tile : tiles) {
            // X1, Y1, X2, Y2, Score
            solutionBytes.putFloat(tile.getTile().x);
            solutionBytes.putFloat(tile.getTile().y);
            solutionBytes.putFloat(tile.getTile().x + tile.getTile().width);
            solutionBytes.putFloat(tile.getTile().y + tile.getTile().height);
            solutionBytes.putFloat(tile.getScore());
          }

          Attribute solution = new Attribute();
          solution.setData(solutionBytes.array());
          solution.setId(this.algorithm.getRegionUri());
          solution.setAttributeName(GENERATED_ATTRIBUTE_NAME);

          ArrayList<Attribute> solutions = new ArrayList<Attribute>();
          // TODO: Send attributes

        }
        lastUpdateTime = now;
      }
      if (500l - (now - lastUpdateTime) > 2) {
        try {
          Thread.sleep(500l - (now - lastUpdateTime));
        } catch (InterruptedException ie) {
          // Ignored
        }
      }
    }
  }
  
  
  
  private boolean launchWorkers() {
    VarianceHandler handler = new VarianceHandler(this);

    return false;
  }
  
  void updateReceiver(){
    
  }
  
  void updateTransmitter(){
    
  }
  
  void updateRssi(){
    
  }

  private void shutdown() {
    this.solverWM.disconnect();
    this.clientWM.disconnect();
  }

  public void setRegionImageUri(String regionImageUri) {
    if (this.userInterface == null)
      return;
    this.regionImageUrl = regionImageUri;

    if (this.regionImageUrl.indexOf("http://") == -1) {
      this.regionImageUrl = "http://" + this.regionImageUrl;
    }

    try {
      BufferedImage origImage = ImageIO.read(new URL(this.regionImageUrl));
      BufferedImage invert = PassiveMotionSolver.negative(origImage);
      this.userInterface.setBackground(invert);

    } catch (MalformedURLException e) {
      log.warn("Invalid region URI: {}", this.regionImageUrl);
      e.printStackTrace();
    } catch (IOException e) {
      log.warn("Could not load region URI at {}.", this.regionImageUrl);
      e.printStackTrace();
    }

  }

  public static void printUsageInfo() {
    System.out
        .println("Usage: <world model host> <solver port> <client port> <region name> [--gui]");
  }

  /**
   * Connects to the world model as a solver and client.
   * 
   * @return {@code true} on connection success, else {@code false}.
   */
  public boolean startConnections() {

    if (!this.solverWM.connect(10000)) {
      log.error("Unable to connect to the world model as a solver.");
      return false;
    }
    if (!this.clientWM.connect(10000)) {
      log.error("Unable to connect to the world model as a client.");
      return false;
    }
    return true;
  }

  /**
   * Originally by <a
   * href="http://www.dreamincode.net/code/snippet4860.htm">erik.price of
   * dreamincode.net</a> (2011/08/04).
   * 
   * @param img
   *          image to negate.
   * @return photo negative version of the image.
   */
  public static BufferedImage negative(BufferedImage img) {
    Color col;
    for (int x = 0; x < img.getWidth(); x++) { // width
      for (int y = 0; y < img.getHeight(); y++) { // height

        int RGBA = img.getRGB(x, y); // gets RGBA data for the specific
        // pixel

        col = new Color(RGBA, true); // get the color data of the
        // specific pixel

        col = new Color(Math.abs(col.getRed() - 255),
            Math.abs(col.getGreen() - 255), Math.abs(col.getBlue() - 255)); // Swaps
        // values
        // i.e. 255, 255, 255 (white)
        // becomes 0, 0, 0 (black)

        img.setRGB(x, y, col.getRGB()); // set the pixel to the altered
        // colors
      }
    }
    return img;
  }

  public UserInterfaceAdapter getUserInterface() {
    return userInterface;
  }

  public void setUserInterface(UserInterfaceAdapter userInterface) {
    this.userInterface = userInterface;
  }

  // FIXME: Got a response from the world model, parse it!
  public void dataResponseReceived(ClientWorldModelInterface worldModel,
      DataResponseMessage message) {
    String uri = message.getId();

    // Region response
    if (uri.equalsIgnoreCase("winlab")) {
      Attribute[] fields = message.getAttributes();
      if (fields == null) {
        log.warn("No fields available for {}.", uri);
        return;
      }
      String mapURL = null;
      for (Attribute field : fields) {
        if ("width".equalsIgnoreCase(field.getAttributeName())) {
          Double width = (Double) DataConverter.decode(
              field.getAttributeName(), field.getData());
          this.algorithm.setRegionXMax(width.floatValue());
        } else if ("height".equalsIgnoreCase(field.getAttributeName())) {
          Double height = (Double) DataConverter.decode(
              field.getAttributeName(), field.getData());
          this.algorithm.setRegionYMax(height.floatValue());
        } else if ("map url".equals(field.getAttributeName())) {
          mapURL = (String) DataConverter.decode(field.getAttributeName(),
              field.getData());
          continue;
        }
      }
      this.setRegionImageUri(mapURL);
    }

    // Handle interesting regions
    if ("winlab".equals(message.getId())) {
      Attribute[] fields = message.getAttributes();
      if (fields == null) {
        log.warn("No information about {} available.", "winlab");
        return;
      }

      Dimension regionDimensions = new Dimension();
      double width = 0.0;
      double height = 0.0;
      String mapURL = null;
      for (Attribute field : fields) {
        if ("width".equals(field.getAttributeName())) {
          width = ((Double) DataConverter.decode(field.getAttributeName(),
              field.getData()));
          continue;
        } else if ("height".equals(field.getAttributeName())) {
          height = ((Double) DataConverter.decode(field.getAttributeName(),
              field.getData()));
          continue;
        } else if ("map url".equals(field.getAttributeName())) {
          mapURL = (String) DataConverter.decode(field.getAttributeName(),
              field.getData());
          continue;
        }
      }
      regionDimensions.setSize(width, height);
      this.setRegionImageUri(mapURL);
    }

    // Check if this is a transmitter
    if (uri.indexOf("transmitter") != -1) {
      Transmitter txer = new Transmitter();
      Attribute[] fields = message.getAttributes();
      if (fields == null) {
        log.warn("No fields available for {}.", uri);
        return;
      }
      for (Attribute field : fields) {
        // X-coordinate
        if ("location.x".equalsIgnoreCase(field.getAttributeName())) {
          Double xPos = (Double) DataConverter.decode(field.getAttributeName(),
              field.getData());
          txer.setxLocation(xPos.floatValue());
        } else if ("location.y".equalsIgnoreCase(field.getAttributeName())) {
          Double yPos = (Double) DataConverter.decode(field.getAttributeName(),
              field.getData());
          txer.setyLocation(yPos.floatValue());
        } else if ("id".equalsIgnoreCase(field.getAttributeName())) {
          Long id = (Long) DataConverter.decode(field.getAttributeName(),
              field.getData());
          // FIXME: Shouldn't hard-code, where do we consolidate the
          // device ID size?
          String deviceId = StringConverter.get().decode(field.getData()).substring(1); 
          
          txer.setDeviceId(deviceId);
        } else if ("region_uri".equalsIgnoreCase(field.getAttributeName())) {
          String region = (String) DataConverter.decode(
              field.getAttributeName(), field.getData());
          txer.setRegionUri(region);
        }
      }
      log.info("Added {} ", txer);
      this.algorithm.addTransmitter(txer);
    } else if (uri.indexOf("receiver") != -1) {
      Receiver rxer = new Receiver();

      Attribute[] fields = message.getAttributes();
      if (fields == null) {
        log.warn("No fields available for {}.", uri);
        return;
      }
      for (Attribute field : fields) {
        // X-coordinate
        if ("location.x".equalsIgnoreCase(field.getAttributeName())) {
          Double xPos = (Double) DataConverter.decode(field.getAttributeName(),
              field.getData());
          rxer.setxLocation(xPos.floatValue());
        } else if ("location.y".equalsIgnoreCase(field.getAttributeName())) {
          Double yPos = (Double) DataConverter.decode(field.getAttributeName(),
              field.getData());
          rxer.setyLocation(yPos.floatValue());
        } else if ("id".equalsIgnoreCase(field.getAttributeName())) {
          String deviceId = StringConverter.get().decode(field.getData()).substring(1); 
          rxer.setDeviceId(deviceId);
        } else if ("region_uri".equalsIgnoreCase(field.getAttributeName())) {
          String region = (String) DataConverter.decode(
              field.getAttributeName(), field.getData());
        }
      }

      log.info("Added {}", rxer);
      this.algorithm.addReceiver(rxer);
    }
  }

  // TODO: Got a search response from the world model.
  public void idSearchResponseReceived(ClientWorldModelInterface worldModel,
      IdSearchResponseMessage message) {
    // Is this the receiver search?
    String[] uriValues = message.getMatchingIds();
    if (uriValues == null || uriValues.length == 0) {
      log.error("No results returned for a URI search request.");
      System.exit(1);
    }

    for (String uri : uriValues) {
      SnapshotRequestMessage request = new SnapshotRequestMessage();
      request.setBeginTimestamp(0l);
      request.setEndTimestamp(System.currentTimeMillis());
      request.setIdRegex(uri);
      request.setAttributeRegexes(new String[] { ".*" });
      // TODO: Get a streaming request for transmitter/receiver locations
    }
  }

}
