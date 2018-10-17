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

package hsnake2D;

import java.awt.geom.Point2D;

/**
 * This abstract class encapsulates the number-crunching aspect of snakes.
 * 
 * @version October 31, 2012
 * 
 * @author Ricard Delgado-Gonzalo (ricard.delgado@gmail.com)
 * @author Philippe Th&#233;venaz (philippe.thevenaz@epfl.ch)
 */
public interface Snake2D {

	// ============================================================================
	// PUBLIC METHODS

	/**
	 * The purpose of this method is to compute the energy of the snake. This
	 * energy is usually made of three additive terms: 1) the image energy,
	 * which gives the driving force associated to the data; 2) the internal
	 * energy, which favors smoothness of the snake; and 3) the constraint
	 * energy, which incorporates a priori knowledge. This method is called
	 * repeatedly during the optimization of the snake, but only as long as the
	 * method <code>isAlive()</code> returns <code>true</code>. It is imperative
	 * that this function be everywhere differentiable with respect to the
	 * snake-defining nodes.
	 */
	public double energy();

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to compute the gradient of the snake energy
	 * with respect to the snake-defining nodes. This method is called
	 * repeatedly during the optimization of the snake, but only as long as the
	 * method <code>isAlive()</code> returns <code>true</code>. Returns an array
	 * that contains the gradient values associated to each node. They predict
	 * the variation of the energy for a horizontal or vertical displacement of
	 * one pixel. The ordering of the nodes must follow that of
	 * <code>getNodes()</code>. If <code>null</code> is returned, the optimizer
	 * within the class <code>Snake2DKeeper</code> will attempt to estimate the
	 * gradient by a finite-difference approach.
	 */

	public Point2D.Double[] getEnergyGradient();

	// ----------------------------------------------------------------------------

	/**
	 * This method provides an accessor to the snake-defining nodes. It may be
	 * called unconditionally, whether the method <code>isAlive()</code> returns
	 * <code>true</code> or <code>false</code>.
	 */
	public Snake2DNode[] getNodes();

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to determine what to draw on screen, given
	 * the current configuration of nodes. This method is called repeatedly
	 * during the user interaction. Collectively, the array of scales forms the
	 * skin of the snake. Returns an array of <code>Snake2DScale</code> objects.
	 * Straight lines will be drawn between the apices of each polygon, in the
	 * specified color. It is not necessary to maintain a constant number of
	 * polygons in the array, or a constant number of apices in a given polygon.
	 */
	public Snake2DScale[] getScales();

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to monitor the status of the snake. One of
	 * its uses is to unconditionally abort the evolution of the snake by
	 * returning <code>false</code>, which provides an easy way to limit the
	 * number of optimization steps by counting the number of calls to the
	 * method <code>energy()</code>. Returns <code>true</code> if the snake
	 * could be properly initialized, if the skin contain valid data, and if the
	 * energy and its gradient can be computed; else, return <code>false</code>.
	 */
	public boolean isAlive();

	// ----------------------------------------------------------------------------

	/**
	 * This method provides a mutator to the snake-defining nodes. It will be
	 * called repeatedly by the methods <code>Snake2DKeeper.interact()</code>
	 * and <code>Snake2DKeeper.optimize()</code>. These calls are unconditional
	 * and may happen whether the method <code>isAlive()</code> returns
	 * <code>true</code> or <code>false</code>.
	 */
	public void setNodes(Snake2DNode[] node);

	// ----------------------------------------------------------------------------

	/**
	 * This method is called when the methods
	 * <code>Snake2DKeeper.interact()</code>,
	 * <code>Snake2DKeeper.interactAndOptimize()</code>, and
	 * <code>Snake2DKeeper.optimize()</code> are about to terminate. It provides
	 * a report on the current status of this snake.
	 */
	public void updateStatus(boolean canceledByUser, boolean snakeDied,
			boolean optimalSnakeFound, Double energy);
}
