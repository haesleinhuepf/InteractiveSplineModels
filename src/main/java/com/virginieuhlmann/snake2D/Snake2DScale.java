/*******************************************************************************
 * Copyright (c) 2012-2013 Biomedical Image Group (BIG), EPFL, Switzerland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Ricard Delgado-Gonzalo (ricard.delgado@gmail.com)
 *     Virginie Uhlmann (virginie.uhlmann@epfl.ch)
 *     Zsuzsanna Puspoki (zsuzsanna.puspoki@epfl.ch)
 ******************************************************************************/
/*====================================================================
| Version: October 24, 2013
\===================================================================*/

/*====================================================================
| Philippe Thevenaz
| EPFL/STI/IMT/LIB/BM.4.137
| Station 17
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.68.10
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

package com.virginieuhlmann.snake2D;

import java.awt.Color;
import java.awt.Polygon;

/*====================================================================
|	Snake2DScale
\===================================================================*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This class is used to store the scales that are used to draw the skin
 of the snake. It extends the capabilities of the class
 <code>Polygon</code> by additional state variables.
 @see Snake2D
 ********************************************************************/
public class Snake2DScale
	extends
		Polygon

{ /* begin class Snake2DScale */

/*....................................................................
	public variables
....................................................................*/
/*********************************************************************
 This is the color with which this scale will be drawn if it is part
 of the optimal skin found so far in the course of the optimization.
 It is also the color with which it will be drawn during the
 interactive session. If <code>bestAttempt</code> is
 <code>null</code>, the color returned by the method
 <code>ij.gui.PolygonRoi.getColor()</code> will be used instead.
 ********************************************************************/
public Color bestAttemptColor = null;

/*********************************************************************
 This is the color with which this scale will be drawn if it is part
 of the skin being currently examined in the course of the
 optimization. It is ignored during the interactive session. If
 <code>currentAttempt</code> is <code>null</code>, the color that is
 most complementary to the color returned by the method
 <code>ij.gui.PolygonRoi.getColor()</code> will be used instead.
 ********************************************************************/
public Color currentAttemptColor = null;

/*********************************************************************
 This flag is used to determine how to draw the outline of this scale.
 If it is set to <code>true</code>, then the first and the last point
 of the polygon are joined. Otherwise, if it is set to
 <code>false</code>, then the first and the last point of the polygon
 are not joined. A closed scale can be filled.
 @see #filled
 ********************************************************************/
public boolean closed = true;

/*********************************************************************
 This flag is used to determine how to draw the interior of this
 scale. If it is set to <code>true</code>, then the scale is filled.
 Otherwise, if it is set to <code>false</code>, then only the outline
 of the scale is drawn. The status of this flag is honored only when
 the scale is closed, as indicated by the <code>closed</code> flag.
 @see #closed
 ********************************************************************/
public boolean filled = false;

/*....................................................................
	private variables
....................................................................*/
private static final long serialVersionUID = 1L;

/*....................................................................
	constructor methods
....................................................................*/
/*------------------------------------------------------------------*/
/*********************************************************************
 This constructor builds a scale with default values. Points can be
 added with the method <code>add()</code> of the class
 <code>Polygon</code>. The default colors <code>bestAttempt</code>
 and <code>currentAttempt</code> are both set to <code>null</code>.
 The scale is closed by default, and it is not filled.
 ********************************************************************/
public Snake2DScale (
) {
	super();
} /* end Snake2DScale */

/*------------------------------------------------------------------*/
/*********************************************************************
 This constructor builds a scale with the provided colors and flags.
 ********************************************************************/
public Snake2DScale (
	final Color bestAttemptColor,
	final Color currentAttemptColor,
	final boolean closed,
	final boolean filled
) {
	super();
	this.bestAttemptColor = bestAttemptColor;
	this.currentAttemptColor = currentAttemptColor;
	this.closed = closed;
	this.filled = filled;
} /* end Snake2DScale */

/*------------------------------------------------------------------*/
/*********************************************************************
 This constructor builds a scale with the provided colors and flags.
 Points are provided according to the conventions of the class
 <code>Polygon</code>.
 ********************************************************************/
public Snake2DScale (
	final int[] xpoints,
	final int[] ypoints,
	final int npoints,
	final Color bestAttemptColor,
	final Color currentAttemptColor,
	final boolean closed,
	final boolean filled
) {
	super(xpoints, ypoints, npoints);
	this.bestAttemptColor = bestAttemptColor;
	this.currentAttemptColor = currentAttemptColor;
	this.closed = closed;
	this.filled = filled;
} /* end Snake2DScale */

/*....................................................................
	Object methods
....................................................................*/
/*------------------------------------------------------------------*/
/*********************************************************************
 This method returns text-based information about this object.
 ********************************************************************/
@Override public String toString (
) {
	return("[" + super.toString()
		+ ", bestAttemptColor: " + bestAttemptColor
		+ ", currentAttemptColor: " + currentAttemptColor
		+ ", closed: " + closed
		+ ", filled: " + filled
		+ "]"
	);
} /* end toString */

} /* end class Snake2DScale */
