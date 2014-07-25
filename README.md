                       Owl Platform Motion Locator Solver

Version 1.0.0-BETA
Last updated June 11, 2012

Project Website: <https://github.com/romoore/motion-locator>

Copyright (C) 2012 Robert Moore and the Owl Platform

This application is free software according to the terms and conditions of
the GNU General Purpose License, version 2.0 (or higher at your discretion).
You should have received a copy of the GNU General Purpose License v2.0 along
with this software as the file LICENSE.  If not, you may download a copy from
<http://www.gnu.org/licenses/gpl-2.0.txt>.

# About #
  The Owl Platform Motion Locator Solver (motion-locator) is a "solver"
  according to the GRAIL platform definitions.  It is also an ongoing research
  project, and so may contain non-essential components (GUIs, logging, etc.)
  which are intended for algorithmic analysis.  The solver requires both a
  World Model server and a "fingerprint solver" (to generate transient RSSI
  statistics) in order to operate correctly.

  More information about the GRAIL architecture can be found at
  the GRAIL developer wiki (<http://grailrtls.sf.net>) or the GRAIL@Rutgers
  homepage (<http://grail.rutgers.edu>).

# Compiling #
  Motion Locator should be compiled using the Apache Maven project management
  tool.  The project is currently compatible with Apache Maven version 3,
  which can be downloaded for free at <http://maven.apache.org/>.  To build
  the static JAR file output, the following command should be run from the
  project root (where the pom.xml file is located):

    mvn clean install -U

  If everything compiles correctly, then near the end of the Maven output,
  this line should appear:

    [INFO] BUILD SUCCESS

  In this case, the JAR file will be located in the ``target'' subdirectory.
  If not, please visit the project website listed at the top of this
  document for support.

# Running #

  Motion Locator must be run directly from the Java launcher (java or
  java.exe).

  To run via the Java launcher directly, you only need to include the path to
  the Jar file:

    java -jar path/to/motion-locator-1.0.0-SNAPSHOT-jar-with-dependencies.jar

	Motion Locator takes a required set of parameters to connect to a remote
	Owl/GRAIL World Model server on startup.  To do so, specify the World Model
	host/IP, client port, and a region name as commandline parameters:

    java -jar path/to/motion-locator-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
      grail.mydomain.com 7009 7010 myregion config.xml --gui

# Data Format #

  Motion Locator produces Attribute values in the Owl Platform World Model with
  a name of "passive motion.tile" and an Origin string of
  "java.solver.pass_motion.v2".  The data in the Attribute is of the form
  {&lt;X1&gt;&lt;Y1&gt;&lt;X2&gt;&lt;Y2&gt;&lt;Score&gt;}<sup>+</sup>, where X1
  and Y1 are the x- and y-coordinates, respectively, of the upper-left corner
  of the rectangle in graphical coordinates (origin is in the upper-left).  X2
  and Y2 are the x- and y-coordinates, respectively, of the lower-right corner
  of the rectangle, and Score is the weighted score of the tile.  A higher
  score is a more likely point of motion.  Each Attribute value may have 1 or
  more of these rectangle-score tuples, and they may overlap.
