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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.solver.SolverAggregatorInterface;
import com.owlplatform.solver.listeners.ConnectionListener;
import com.owlplatform.solver.listeners.SampleListener;
import com.owlplatform.solver.passivemotion.gui.panels.GraphicalUserInterface;
import com.owlplatform.solver.passivemotion.gui.panels.UserInterfaceAdapter;
import com.owlplatform.solver.protocol.messages.SubscriptionMessage;
import com.owlplatform.solver.rules.SubscriptionRequestRule;
import com.owlplatform.worldmodel.Attribute;
import com.owlplatform.worldmodel.client.ClientWorldModelInterface;
import com.owlplatform.worldmodel.client.listeners.DataListener;
import com.owlplatform.worldmodel.client.protocol.messages.AbstractRequestMessage;
import com.owlplatform.worldmodel.client.protocol.messages.AttributeAliasMessage;
import com.owlplatform.worldmodel.client.protocol.messages.DataResponseMessage;
import com.owlplatform.worldmodel.client.protocol.messages.IdSearchResponseMessage;
import com.owlplatform.worldmodel.client.protocol.messages.OriginAliasMessage;
import com.owlplatform.worldmodel.client.protocol.messages.OriginPreferenceMessage;
import com.owlplatform.worldmodel.client.protocol.messages.SnapshotRequestMessage;
import com.owlplatform.worldmodel.solver.SolverWorldModelInterface;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage.AttributeSpecification;
import com.owlplatform.worldmodel.solver.protocol.messages.StartOnDemandMessage;
import com.owlplatform.worldmodel.solver.protocol.messages.StopOnDemandMessage;
import com.owlplatform.worldmodel.types.DataConverter;

public class PassiveMotionSolver extends Thread implements SampleListener,
    ConnectionListener,
    com.owlplatform.worldmodel.solver.listeners.ConnectionListener,
    com.owlplatform.worldmodel.client.listeners.ConnectionListener,
    DataListener, com.owlplatform.worldmodel.solver.listeners.DataListener {
  private static final Logger log = LoggerFactory
      .getLogger(PassiveMotionSolver.class);

  public static final String SOLUTION_URI_NAME = "passive motion.tile";
  public static final String SOLVER_ORIGIN_STRING = "java.solver.passive_motion.v1";

  private SolverWorldModelInterface solverWMI;

  private SolverAggregatorInterface aggregator;

  private ClientWorldModelInterface clientWMI;

  private PassiveMotionAlgorithm algorithm = new PassiveMotionAlgorithm();

  private boolean canSendSolutions = false;

  protected UserInterfaceAdapter userInterface = null;

  protected String regionImageUri = null;

  protected BufferedImage regionImage = null;

  public static void main(String[] args) {
    if (args.length < 6) {
      printUsageInfo();
      return;
    }

    PassiveMotionSolver solver = new PassiveMotionSolver(args[0],
        Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), args[4],
        Integer.valueOf(args[5]));

    if (args.length > 6) {
      for (int i = 6; i < args.length; ++i) {
        if (args[i].equals("--gui")) {
          solver.setUserInterface(new GraphicalUserInterface());
        }
      }
    }

    solver.start();
  }

  public PassiveMotionSolver(String aggHost, int aggPort, String distHost,
      int distPort, String worldHost, int worldPort) {

    // Configure the aggregator
    this.aggregator = new SolverAggregatorInterface();
    this.aggregator.setHost(aggHost);
    this.aggregator.setPort(aggPort);
    this.aggregator.addSampleListener(this);
    this.aggregator.addConnectionListener(this);

    // Configure the distributor
    this.solverWMI = new SolverWorldModelInterface();
    this.solverWMI.setHost(distHost);
    this.solverWMI.setPort(distPort);
    this.solverWMI.addConnectionListener(this);
    this.solverWMI.addDataListener(this);
    this.solverWMI.setOriginString(SOLVER_ORIGIN_STRING);

    // Configure the World Server
    this.clientWMI = new ClientWorldModelInterface();
    this.clientWMI.setHost(worldHost);
    this.clientWMI.setPort(worldPort);
    this.clientWMI.addDataListener(this);
    this.clientWMI.addConnectionListener(this);

    // Configure the fingerprinter
    StdDevFingerprintGenerator fingerprinter = new StdDevFingerprintGenerator();
    fingerprinter.setMaxNumSamples(3);
    fingerprinter.setMaxSampleAge(5000l);
    this.algorithm.setStdDevFingerprinter(fingerprinter);

    // Configure the algorithm
    this.algorithm.setLineLengthPower(1.1f);
    this.algorithm.setDesiredTileHeight(20f);
    this.algorithm.setDesiredTileWidth(20f);
    // this.algorithm.setNumXTiles(20);
    // this.algorithm.setNumYTiles(20);
    this.algorithm.setRadiusThreshold(90f);
    this.algorithm.setStdDevNoiseThreshold(1.2f);
    this.algorithm.setTileScoreThreshold(.50f);
    this.algorithm.setRegionUri("winlab");
  }

  public void run() {
    // Request as many samples as possible
    SubscriptionRequestRule allTxersRule = SubscriptionRequestRule
        .generateGenericRule();
    allTxersRule.setPhysicalLayer((byte) 1);
    allTxersRule.setUpdateInterval(0l);
    this.aggregator.setRules(new SubscriptionRequestRule[] { allTxersRule });

    // Aggregator connection parameters
    this.aggregator.setConnectionRetryDelay(10000l);
    this.aggregator.setConnectionTimeout(10000l);
    this.aggregator.setStayConnected(true);
    this.aggregator.setDisconnectOnException(true);

    // Tell the distributor the name of this solver's solution
    AttributeSpecification spec = new AttributeSpecification();
    spec.setIsOnDemand(false);
    spec.setAttributeName(SOLUTION_URI_NAME);
    this.solverWMI.addAttribute(spec);

    // Distributor connection parameters
    this.solverWMI.setConnectionRetryDelay(10000l);
    this.solverWMI.setConnectionTimeout(10000l);
    this.solverWMI.setStayConnected(true);
    this.solverWMI.setDisconnectOnException(true);
    this.solverWMI.setOriginString(SOLVER_ORIGIN_STRING);

    // World Server connection parameters
    this.clientWMI.setConnectionRetryDelay(10000l);
    this.clientWMI.setConnectionTimeout(10000l);
    this.clientWMI.setStayConnected(true);
    this.clientWMI.setDisconnectOnException(true);

    // Connect to aggregator, distributor, and world server
    this.startConnections();

    long lastUpdateTime = 0l;
    while (true) {
      long now = System.currentTimeMillis();
      if (now - lastUpdateTime > 500l) {
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
          solution.setAttributeName(SOLUTION_URI_NAME);

          ArrayList<Attribute> solutions = new ArrayList<Attribute>();

          this.solverWMI.updateAttributes(solutions);

        }
        lastUpdateTime = now;
      }
      try {
        Thread.sleep(500l - (now - lastUpdateTime));
      } catch (InterruptedException ie) {
        // Ignored
      }

    }
  }

  public void setRegionImageUri(String regionImageUri) {
    if (this.userInterface == null)
      return;
    this.regionImageUri = regionImageUri;

    if (this.regionImageUri.indexOf("http://") == -1) {
      this.regionImageUri = "http://" + this.regionImageUri;
    }

    try {
      BufferedImage origImage = ImageIO.read(new URL(this.regionImageUri));
      BufferedImage invert = PassiveMotionSolver.negative(origImage);
      this.userInterface.setBackground(invert);

    } catch (MalformedURLException e) {
      log.warn("Invalid region URI: {}", this.regionImageUri);
      e.printStackTrace();
    } catch (IOException e) {
      log.warn("Could not load region URI at {}.", this.regionImageUri);
      e.printStackTrace();
    }

  }

  public static void printUsageInfo() {
    System.out
        .println("One or more parameters is missing or invalid: <aggregator host> <aggregator port> <distributor host> <distributor port> <world server host> <world server port>");
  }

  public void startConnections() {

    if (!this.aggregator.doConnectionSetup()) {
      log.error("Could not establish connection to the aggregator at {}:{}.",
          this.aggregator.getHost(), this.aggregator.getPort());
      System.exit(1);
    }
    if (!this.solverWMI.connect(10000l)) {
      log.error("Could not establish connection to the distributor at {}:{}.",
          this.solverWMI.getHost(), this.solverWMI.getPort());
      System.exit(1);
    }
    if (!this.clientWMI.connect(10000l)) {
      log.error("Could not establish connection to the world server at {}:{}.",
          this.clientWMI.getHost(), this.clientWMI.getPort());
      System.exit(1);
    }

  }

  public void sampleReceived(SolverAggregatorInterface aggregator,
      SampleMessage sample) {
    this.algorithm.addSample(sample);
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

  public void connectionEnded(SolverAggregatorInterface aggregator) {
    log.error("Connection to {} has ended.  Exiting solver.", aggregator);
    this.solverWMI.disconnect();
    this.clientWMI.disconnect();
    System.exit(1);
  }

  public void connectionEstablished(SolverAggregatorInterface aggregator) {
    // TODO Auto-generated method stub

  }

  public void connectionInterrupted(SolverAggregatorInterface aggregator) {
    // TODO Auto-generated method stub

  }

  public UserInterfaceAdapter getUserInterface() {
    return userInterface;
  }

  public void setUserInterface(UserInterfaceAdapter userInterface) {
    this.userInterface = userInterface;
  }

  @Override
  public void requestCompleted(ClientWorldModelInterface worldModel,
      AbstractRequestMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
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
          byte[] deviceId = new byte[16];
          for (int i = 0; i < 8; ++i) {
            deviceId[deviceId.length - 1 - i] = (byte) (id >> (8 * i));
          }
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
          Long id = (Long) DataConverter.decode(field.getAttributeName(),
              field.getData());
          // FIXME: Shouldn't hard-code, where do we consolidate the
          // device ID size?
          byte[] deviceId = new byte[16];
          for (int i = 0; i < 8; ++i) {
            deviceId[deviceId.length - 1 - i] = (byte) (id >> (8 * i));
          }
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

  @Override
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
      this.clientWMI.sendMessage(request);
    }
  }

  @Override
  public void attributeAliasesReceived(
      ClientWorldModelInterface clientWorldModelInterface,
      AttributeAliasMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void originAliasesReceived(
      ClientWorldModelInterface clientWorldModelInterface,
      OriginAliasMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connectionInterrupted(SolverWorldModelInterface worldModel) {
    this.canSendSolutions = false;
  }

  @Override
  public void connectionEnded(SolverWorldModelInterface worldModel) {
    log.error("Connection to {} has ended.  Exiting solver.", worldModel);
    this.aggregator.disconnect();
    this.clientWMI.disconnect();
    System.exit(1);
  }

  @Override
  public void connectionEstablished(SolverWorldModelInterface worldModel) {
    // TODO Auto-generated method stub

  }

  @Override
  public void startOnDemandReceived(SolverWorldModelInterface worldModel,
      StartOnDemandMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void stopOnDemandReceived(SolverWorldModelInterface worldModel,
      StopOnDemandMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void attributeSpecificationsSent(SolverWorldModelInterface worldModel,
      AttributeAnnounceMessage message) {
    // Once the solution spec message is sent, we can generate solutions
    // TODO: Is this overcautious?
    this.canSendSolutions = true;
  }

  @Override
  public void connectionInterrupted(ClientWorldModelInterface worldModel) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connectionEnded(ClientWorldModelInterface worldModel) {
    log.error("Connection to {} has ended.  Exiting solver.", worldModel);
    this.aggregator.disconnect();
    this.solverWMI.disconnect();
    System.exit(1);
  }

  @Override
  public void connectionEstablished(ClientWorldModelInterface worldModel) {
    this.clientWMI.searchIdRegex("region.winlab");
    this.clientWMI.searchIdRegex("winlab.*.transmitter.*");
    this.clientWMI.searchIdRegex("winlab.*.receiver.*");

  }

  @Override
  public void originPreferenceSent(ClientWorldModelInterface worldModel,
      OriginPreferenceMessage message) {
    // TODO Auto-generated method stub

  }

  @Override
  public void subscriptionReceived(SolverAggregatorInterface aggregator,
      SubscriptionMessage response) {
    // TODO Auto-generated method stub

  }

}
