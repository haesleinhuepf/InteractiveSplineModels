package com.virginieuhlmann.hsnake2D;

import static java.lang.Math.round;

import java.util.Observable;
import java.util.Observer;

import com.virginieuhlmann.snake2D.Snake2D;
import com.virginieuhlmann.snake2D.Snake2DNode;
import com.virginieuhlmann.snake2D.Snake2DOptimizer;
import ij.ImagePlus;
import ij.gui.Toolbar;

/**
 * This class encapsulates the interactive and managerial aspects of Hermite
 * snakes. It handles objects that implement the <code>Snake2D</code> interface.
 *
 * @version February 24, 2018
 *
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 * @author Ricard Delgado-Gonzalo (ricard.delgado@gmail.com)
 * @author Philippe Th&#233;venaz (philippe.thevenaz@epfl.ch)
 */
public class HSnake2DKeeper implements Observer {

	/*
	 * .................................................................... private
	 * variables
	 * ....................................................................
	 */
	private ImagePlus display = null;
	private snake2DEditToolbar tb = null;
	private snake2DSkinHandler sh = null;
	private boolean canceledByUser = false;
	private boolean optimalSnakeFound = false;
	private boolean optimizing = false;
	private boolean singleShot = true;
	private boolean snakeDied = false;

	/*
	 * .................................................................... public
	 * methods ....................................................................
	 */
	/*------------------------------------------------------------------*/
	/*********************************************************************
	 * This method overlays handles on the image and lets the user interact with
	 * them. It calls the method <code>Snake2D.getNodes()</code> one times to
	 * initialize the snake-defining nodes, and calls the method
	 * <code>Snake2D.setNodes()</code> repeatedly to update the snake-defining nodes
	 * during the interactive session. The skin of the snake is drawn according to
	 * the data available through the method <code>Snake2D.getScales()</code>. It is
	 * possible to request that the interaction be aborted by letting the method
	 * <code>Snake2D.isAlive()</code> return <code>false</code>. This request will
	 * be honored at the next user attempt to modify the snake.
	 *
	 * @param snake   The snake to Handle.
	 * @param display A mandatory <code>ImagePlus</code> object over which the
	 *                handles used to interactively manipulate the snake will be
	 *                overlaid.
	 * @see Snake2D#isAlive
	 * @see Snake2D#getNodes
	 * @see Snake2D#setNodes
	 * @see Snake2D#getScales
	 ********************************************************************/
	public void interact(final Snake2D snake, final ImagePlus display, final double tangentWeight) {
		if (snake == null) {
			return;
		}
		this.display = display;
		canceledByUser = false;
		snakeDied = !snake.isAlive();
		optimalSnakeFound = false;
		final Double energy = null;
		if ((display == null) || snakeDied) {
			snake.updateStatus(canceledByUser, snakeDied, optimalSnakeFound, energy);
			return;
		}
		display.killRoi();
		optimizing = false;
		singleShot = true;
		sh = null;
		final Snake2DNode[] youngSnake = snake.getNodes();
		final int K = youngSnake.length;
		final Snake2DNode[] X = new Snake2DNode[K];
		for (int k = 0; (k < K); k++) {
			X[k] = new Snake2DNode(youngSnake[k].x, youngSnake[k].y, youngSnake[k].frozen, youngSnake[k].hidden);
		}
		tb = new snake2DEditToolbar(Toolbar.getInstance(), this);
		final snake2DPointHandler ph = new snake2DPointHandler(display, snake, X, tb, this, tangentWeight);
		final snake2DPointAction pa = new snake2DPointAction(display, ph, tb, this);
		ph.setPointAction(pa);
		ph.activateDisplay();
		try {
			synchronized (this) {
				display.setRoi(ph);
				wait();
			}
		} catch (InterruptedException e) {
		}
		tb.terminateInteraction(snake, ph);
		snake.updateStatus(canceledByUser, snakeDied, optimalSnakeFound, energy);
	} /* end interact */

	/*
	 * ....................................................................
	 * protected methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected synchronized void destroyOptimality() {
		optimalSnakeFound = false;
	} /* end destroyOptimality */

	/*------------------------------------------------------------------*/
	protected synchronized boolean isOptimizing() {
		return (optimizing);
	} /* end isOptimizing */

	/*------------------------------------------------------------------*/
	protected synchronized boolean isSingleShot() {
		return (singleShot);
	} /* end isSingleShot */

	/*------------------------------------------------------------------*/
	protected synchronized void setCanceledByUser(final boolean canceledByUser) {
		this.canceledByUser = canceledByUser;
	} /* end setCanceledByUser */

	/*------------------------------------------------------------------*/
	protected synchronized void setSnakeDied(final boolean snakeDied) {
		this.snakeDied = snakeDied;
	} /* end setSnakeDied */

	/*------------------------------------------------------------------*/
	protected synchronized void startOptimizing() {
		optimizing = true;
	} /* end startOptimizing */

	/*------------------------------------------------------------------*/
	protected synchronized void stopOptimizing(final snake2DPointHandler ph) {
		optimizing = false;
		tb.setTool(snake2DPointAction.MOVE_CROSS);
		if (display != null) {
			display.setRoi(ph);
		}
	} /* end stopOptimizing */

	/*
	 * .................................................................... Observer
	 * methods ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void update(final Observable observable, final Object object) {
		if (display != null) {
			if (((Snake2DOptimizer) observable).isCurrentBest) {
				display.setRoi(sh);
			} else {
				sh.setBestSkin(((Snake2D) object).getScales());
			}
		}
	} /* end update */

} /* end class Snake2DKeeper */

/*
 * ==================================================================== |
 * snake2DEditToolbar
 * \===================================================================
 */

/*
 * ==================================================================== |
 * snake2DPointAction
 * \===================================================================
 */

/*
 * ==================================================================== |
 * snake2DPointHandler
 * \===================================================================
 */

/*
 * ==================================================================== |
 * snake2DSkinHandler
 * \===================================================================
 */

