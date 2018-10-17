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

package snake2D;

import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.measure.Calibration;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Observer;

/**
 * This class encapsulates the interactive and managerial aspects of snakes. It
 * handles objects that implement the <code>Snake2D</code> interface.
 * 
 * @version October 31, 2012
 * 
 * @author Ricard Delgado-Gonzalo (ricard.delgado@gmail.com)
 * @author Philippe Th&#233;venaz (philippe.thevenaz@epfl.ch)
 */
public class Snake2DKeeper implements Observer {

	/*
	 * ....................................................................
	 * private variables
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
	 * ....................................................................
	 * public methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	/*********************************************************************
	 * This method overlays handles on the image and lets the user interact with
	 * them. It calls the method <code>Snake2D.getNodes()</code> one times to
	 * initialize the snake-defining nodes, and calls the method
	 * <code>Snake2D.setNodes()</code> repeatedly to update the snake-defining
	 * nodes during the interactive session. The skin of the snake is drawn
	 * according to the data available through the method
	 * <code>Snake2D.getScales()</code>. It is possible to request that the
	 * interaction be aborted by letting the method
	 * <code>Snake2D.isAlive()</code> return <code>false</code>. This request
	 * will be honored at the next user attempt to modify the snake.
	 * 
	 * @param snake
	 *            The snake to Handle.
	 * @param display
	 *            A mandatory <code>ImagePlus</code> object over which the
	 *            handles used to interactively manipulate the snake will be
	 *            overlaid.
	 * @see Snake2D#isAlive
	 * @see Snake2D#getNodes
	 * @see Snake2D#setNodes
	 * @see Snake2D#getScales
	 ********************************************************************/
	public void interact(final Snake2D snake, final ImagePlus display) {
		if (snake == null) {
			return;
		}
		this.display = display;
		canceledByUser = false;
		snakeDied = !snake.isAlive();
		optimalSnakeFound = false;
		final Double energy = null;
		if ((display == null) || snakeDied) {
			snake.updateStatus(canceledByUser, snakeDied, optimalSnakeFound,
					energy);
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
			X[k] = new Snake2DNode(youngSnake[k].x, youngSnake[k].y,
					youngSnake[k].frozen, youngSnake[k].hidden);
		}
		tb = new snake2DEditToolbar(Toolbar.getInstance(), this);
		final snake2DPointHandler ph = new snake2DPointHandler(display, snake,
				X, tb, this);
		final snake2DPointAction pa = new snake2DPointAction(display, ph, tb,
				this);
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
	 * ....................................................................
	 * Observer methods
	 * ....................................................................
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

/*------------------------------------------------------------------*/
class snake2DEditToolbar extends Canvas implements AdjustmentListener,
		MouseListener

{ /* begin class snake2DEditToolbar */

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private Graphics g = null;
	private ImagePlus display = null;
	private ScrollbarWithLabel scrollbar = null;
	private Snake2DKeeper keeper = null;
	private Toolbar previousInstance = null;
	private final boolean[] down = new boolean[TOOLS];
	private int currentTool = snake2DPointAction.MOVE_CROSS;
	private int x = 0;
	private int xOffset = 0;
	private int y = 0;
	private int yOffset = 0;
	private snake2DPointAction pa = null;
	private snake2DPointHandler ph = null;
	private snake2DEditToolbar instance = null;
	private static final Color gray = Color.lightGray;
	private static final Color brighter = gray.brighter();
	private static final Color darker = gray.darker();
	private static final Color evenDarker = darker.darker();
	private static final int OFFSET = 3;
	private static final int TOOL_SIZE = 22;
	private static final int TOOLS = 20;
	private static final long serialVersionUID = 1L;

	/*
	 * ....................................................................
	 * constructor methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected snake2DEditToolbar(final Toolbar previousToolbar,
			final Snake2DKeeper keeper) {
		previousInstance = previousToolbar;
		this.keeper = keeper;
		instance = this;
		final Container container = previousToolbar.getParent();
		final Component component[] = container.getComponents();
		for (int n = 0, N = component.length; (n < N); n++) {
			if (component[n] == previousToolbar) {
				container.remove(previousToolbar);
				container.add(this, n);
				break;
			}
		}
		resetButtons();
		down[currentTool] = true;
		setForeground(evenDarker);
		setBackground(gray);
		addMouseListener(this);
		container.validate();
	} /* end snake2DEditToolbar */

	/*
	 * ....................................................................
	 * AdjustmentListener methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		display.setRoi(ph);
	} /* adjustmentValueChanged */

	/*
	 * ....................................................................
	 * Canvas methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void paint(final Graphics g) {
		for (int i = 0; (i < TOOLS); i++) {
			drawButton(g, i);
		}
	} /* paint */

	/*
	 * ....................................................................
	 * MouseListener methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void mouseClicked(final MouseEvent e) {
	} /* end mouseClicked */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseEntered(final MouseEvent e) {
	} /* end mouseEntered */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseExited(final MouseEvent e) {
	} /* end mouseExited */

	/*------------------------------------------------------------------*/
	@Override
	public void mousePressed(final MouseEvent e) {
		final int x = e.getX() - 2;
		// final int y = e.getY() + 2;
		// final int previousTool = currentTool;
		int newTool = snake2DPointAction.UNDEFINED;
		for (int i = 0; (i < TOOLS); i++) {
			if (((i * TOOL_SIZE) <= x) && (x < (i * TOOL_SIZE + TOOL_SIZE))) {
				newTool = i;
			}
		}
		switch (newTool) {
		case snake2DPointAction.ACCEPT: {
			if (keeper.isOptimizing()) {
				setTool(newTool);
				showMessage(snake2DPointAction.UNDEFINED);
			} else {
				setTool(newTool);
				showMessage(newTool);
				synchronized (keeper) {
					keeper.notify();
				}
			}
			break;
		}
		case snake2DPointAction.CANCEL: {
			keeper.setCanceledByUser(true);
			if (keeper.isOptimizing()) {
				keeper.stopOptimizing(ph);
				setTool(newTool);
				IJ.showStatus("Optimization interrupted");
			} else {
				setTool(newTool);
				showMessage(newTool);
				synchronized (keeper) {
					keeper.notify();
				}
			}
			break;
		}
		case snake2DPointAction.MAGNIFIER: {
			setTool(newTool);
			showMessage(newTool);
			break;
		}
		case snake2DPointAction.MOVE_CROSS: {
			if (keeper.isOptimizing()) {
				keeper.stopOptimizing(ph);
				IJ.showStatus("Optimization interrupted");
			} else {
				setTool(newTool);
				showMessage(newTool);
			}
			break;
		}
		case snake2DPointAction.RESIZE: {
			if (!keeper.isOptimizing()) {
				ph.resetHull();
				setTool(newTool);
				showMessage(newTool);
			} else {
				setTool(newTool);
				showMessage(snake2DPointAction.UNDEFINED);
			}
			break;
		}
		case snake2DPointAction.ROTATE: {
			if (!keeper.isOptimizing()) {
				ph.resetHull();
				setTool(newTool);
				showMessage(newTool);
			} else {
				setTool(newTool);
				showMessage(snake2DPointAction.UNDEFINED);
			}
			break;
		}
		case snake2DPointAction.START: {
			if (!(keeper.isSingleShot() || keeper.isOptimizing())) {
				keeper.startOptimizing();
				setTool(newTool);
				IJ.showStatus("Optimization started");
				synchronized (keeper) {
					keeper.notify();
				}
			} else {
				setTool(newTool);
				showMessage(newTool);
			}
			break;
		}
		case snake2DPointAction.UNDEFINED: {
			showMessage(newTool);
			break;
		}
		default: {
			setTool(newTool);
			showMessage(newTool);
			break;
		}
		}
	} /* mousePressed */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseReleased(final MouseEvent e) {
	} /* end mouseReleased */

	/*
	 * ....................................................................
	 * protected methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected int getCurrentTool() {
		return (currentTool);
	} /* getCurrentTool */

	/*------------------------------------------------------------------*/
	protected void installListeners(snake2DPointAction pa) {
		this.pa = pa;
		final ImageWindow iw = display.getWindow();
		final ImageCanvas ic = iw.getCanvas();
		iw.removeKeyListener(IJ.getInstance());
		ic.removeKeyListener(IJ.getInstance());
		ic.removeMouseListener(ic);
		ic.removeMouseMotionListener(ic);
		ic.addMouseMotionListener(pa);
		ic.addMouseListener(pa);
		ic.addKeyListener(pa);
		iw.addKeyListener(pa);
		if (display.getWindow() instanceof StackWindow) {
			StackWindow sw = (StackWindow) display.getWindow();
			final Component component[] = sw.getComponents();
			for (int n = 0, N = component.length; (n < N); n++) {
				if (component[n] instanceof ScrollbarWithLabel) {
					scrollbar = (ScrollbarWithLabel) component[n];
					scrollbar.addAdjustmentListener(this);
				}
			}
		} else {
			scrollbar = null;
		}
	} /* end installListeners */

	/*------------------------------------------------------------------*/
	protected void setTool(final int tool) {
		if (tool == currentTool) {
			return;
		}
		down[tool] = true;
		down[currentTool] = false;
		final Graphics g = this.getGraphics();
		drawButton(g, currentTool);
		drawButton(g, tool);
		switch (tool) {
		case snake2DPointAction.MOVE_CROSS: {
			if (currentTool != snake2DPointAction.START) {
				drawButton(g, snake2DPointAction.START);
				drawButton(g, snake2DPointAction.RESIZE);
				drawButton(g, snake2DPointAction.ROTATE);
			}
			break;
		}
		case snake2DPointAction.RESIZE: {
			break;
		}
		case snake2DPointAction.ROTATE: {
			break;
		}
		case snake2DPointAction.START: {
			if (currentTool != snake2DPointAction.MOVE_CROSS) {
				drawButton(g, snake2DPointAction.MOVE_CROSS);
				drawButton(g, snake2DPointAction.RESIZE);
				drawButton(g, snake2DPointAction.ROTATE);
			}
			break;
		}
		}
		update(g);
		g.dispose();
		currentTool = tool;
		display.setRoi(ph);
	} /* end setTool */

	/*------------------------------------------------------------------*/
	protected void setWindow(final snake2DPointHandler ph,
			final ImagePlus display) {
		this.ph = ph;
		this.display = display;
	} /* end setWindow */

	/*------------------------------------------------------------------*/
	protected void terminateInteraction(final Snake2D snake,
			final snake2DPointHandler ph) {
		cleanUpListeners();
		restorePreviousToolbar();
		Toolbar.getInstance().repaint();
		snake.setNodes(ph.getPoints());
		ph.deactivateDisplay();
	} /* end terminateInteraction */

	/*
	 * ....................................................................
	 * private methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	private void cleanUpListeners() {
		if (scrollbar != null) {
			scrollbar.removeAdjustmentListener(this);
		}
		final ImageWindow iw = display.getWindow();
		final ImageCanvas ic = iw.getCanvas();
		iw.removeKeyListener(pa);
		ic.removeKeyListener(pa);
		ic.removeMouseListener(pa);
		ic.removeMouseMotionListener(pa);
		ic.addMouseMotionListener(ic);
		ic.addMouseListener(ic);
		ic.addKeyListener(IJ.getInstance());
		iw.addKeyListener(IJ.getInstance());
	} /* end cleanUpListeners */

	/*------------------------------------------------------------------*/
	private void d(int x, int y) {
		x += xOffset;
		y += yOffset;
		g.drawLine(this.x, this.y, x, y);
		this.x = x;
		this.y = y;
	} /* end d */

	/*------------------------------------------------------------------*/
	private void drawButton(final Graphics g, final int tool) {
		fill3DRect(g, tool * TOOL_SIZE + 1, 1, TOOL_SIZE, TOOL_SIZE - 1,
				!down[tool]);
		g.setColor(Color.black);
		int x = tool * TOOL_SIZE + OFFSET;
		int y = OFFSET;
		if (down[tool]) {
			x++;
			y++;
		}
		this.g = g;
		switch (tool) {
		case snake2DPointAction.ACCEPT: {
			if (!keeper.isOptimizing()) {
				xOffset = x;
				yOffset = y;
				m(2, 8);
				d(2, 9);
				m(3, 8);
				d(3, 10);
				m(4, 9);
				d(4, 11);
				m(5, 10);
				d(5, 12);
				m(6, 11);
				d(6, 13);
				m(7, 10);
				d(7, 13);
				m(8, 8);
				d(8, 12);
				m(9, 6);
				d(9, 10);
				m(10, 4);
				d(10, 8);
				m(11, 2);
				d(11, 6);
				m(12, 2);
				d(12, 4);
			}
			break;
		}
		case snake2DPointAction.CANCEL: {
			xOffset = x;
			yOffset = y;
			m(2, 2);
			d(3, 2);
			m(12, 2);
			d(13, 2);
			m(2, 3);
			d(4, 3);
			m(11, 3);
			d(13, 3);
			m(3, 4);
			d(5, 4);
			m(10, 4);
			d(12, 4);
			m(4, 5);
			d(6, 5);
			m(9, 5);
			d(11, 5);
			m(5, 6);
			d(10, 6);
			m(6, 7);
			d(9, 7);
			m(6, 8);
			d(9, 8);
			m(5, 9);
			d(10, 9);
			m(4, 10);
			d(6, 10);
			m(9, 10);
			d(11, 10);
			m(3, 11);
			d(5, 11);
			m(10, 11);
			d(12, 11);
			m(2, 12);
			d(4, 12);
			m(11, 12);
			d(13, 12);
			m(2, 13);
			d(3, 13);
			m(12, 13);
			d(13, 13);
			break;
		}
		case snake2DPointAction.MAGNIFIER: {
			xOffset = x + 2;
			yOffset = y + 2;
			m(3, 0);
			d(3, 0);
			d(5, 0);
			d(8, 3);
			d(8, 5);
			d(7, 6);
			d(7, 7);
			d(6, 7);
			d(5, 8);
			d(3, 8);
			d(0, 5);
			d(0, 3);
			d(3, 0);
			m(8, 8);
			d(9, 8);
			d(13, 12);
			d(13, 13);
			d(12, 13);
			d(8, 9);
			d(8, 8);
			break;
		}
		case snake2DPointAction.MOVE_CROSS: {
			xOffset = x;
			yOffset = y;
			if (keeper.isOptimizing()) {
				m(2, 3);
				d(13, 3);
				m(2, 4);
				d(13, 4);
				m(2, 5);
				d(13, 5);
				m(2, 6);
				d(13, 6);
				m(2, 7);
				d(13, 7);
				m(2, 8);
				d(13, 8);
				m(2, 9);
				d(13, 9);
				m(2, 10);
				d(13, 10);
				m(2, 11);
				d(13, 11);
				m(2, 12);
				d(13, 12);
				m(2, 13);
				d(13, 13);
			} else {
				m(1, 1);
				d(1, 10);
				m(2, 2);
				d(2, 9);
				m(3, 3);
				d(3, 8);
				m(4, 4);
				d(4, 7);
				m(5, 5);
				d(5, 7);
				m(6, 6);
				d(6, 7);
				m(7, 7);
				d(7, 7);
				m(11, 5);
				d(11, 6);
				m(10, 7);
				d(10, 8);
				m(12, 7);
				d(12, 8);
				m(9, 9);
				d(9, 11);
				m(13, 9);
				d(13, 11);
				m(10, 12);
				d(10, 15);
				m(12, 12);
				d(12, 15);
				m(11, 9);
				d(11, 10);
				m(11, 13);
				d(11, 15);
				m(9, 13);
				d(13, 13);
			}
			break;
		}
		case snake2DPointAction.RESIZE: {
			if (!keeper.isOptimizing()) {
				xOffset = x;
				yOffset = y;
				m(0, 2);
				d(0, 12);
				m(1, 3);
				d(1, 11);
				m(2, 4);
				d(2, 10);
				m(3, 5);
				d(3, 9);
				m(4, 6);
				d(4, 9);
				m(6, 8);
				d(6, 9);
				m(7, 9);
				d(7, 9);
				m(5, 3);
				d(5, 12);
				d(14, 12);
				d(14, 3);
				d(5, 3);
				m(9, 2);
				d(10, 2);
				m(9, 13);
				d(10, 13);
				m(15, 7);
				d(15, 8);
			}
			break;
		}
		case snake2DPointAction.ROTATE: {
			if (!keeper.isOptimizing()) {
				xOffset = x;
				yOffset = y;
				m(0, 2);
				d(0, 12);
				m(1, 3);
				d(1, 11);
				m(2, 4);
				d(2, 10);
				m(3, 5);
				d(3, 9);
				m(4, 6);
				d(4, 9);
				m(6, 8);
				d(6, 9);
				m(7, 9);
				d(7, 9);
				m(5, 2);
				d(5, 13);
				d(4, 13);
				m(4, 12);
				d(15, 12);
				d(15, 13);
				m(14, 13);
				d(14, 2);
				d(15, 2);
				m(15, 3);
				d(4, 3);
				d(4, 2);
				m(9, 9);
				d(9, 7);
				d(8, 7);
				m(10, 6);
				d(10, 8);
				d(11, 8);
			}
			break;
		}
		case snake2DPointAction.START: {
			if (!(keeper.isSingleShot() || keeper.isOptimizing())) {
				xOffset = x;
				yOffset = y;
				m(2, 3);
				d(3, 3);
				m(2, 4);
				d(5, 4);
				m(2, 5);
				d(7, 5);
				m(2, 6);
				d(9, 6);
				m(2, 7);
				d(11, 7);
				m(2, 8);
				d(13, 8);
				m(2, 9);
				d(11, 9);
				m(2, 10);
				d(9, 10);
				m(2, 11);
				d(7, 11);
				m(2, 12);
				d(5, 12);
				m(2, 13);
				d(3, 13);
			}
			break;
		}
		}
	} /* end drawButton */

	/*------------------------------------------------------------------*/
	private void fill3DRect(final Graphics g, final int x, final int y,
			final int width, final int height, final boolean raised) {
		if (raised) {
			g.setColor(gray);
		} else {
			g.setColor(darker);
		}
		g.fillRect(x + 1, y + 1, width - 2, height - 2);
		g.setColor((raised) ? (brighter) : (evenDarker));
		g.drawLine(x, y, x, y + height - 1);
		g.drawLine(x + 1, y, x + width - 2, y);
		g.setColor((raised) ? (evenDarker) : (brighter));
		g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
		g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
	} /* end fill3DRect */

	/*------------------------------------------------------------------*/
	private void m(final int x, final int y) {
		this.x = xOffset + x;
		this.y = yOffset + y;
	} /* end m */

	/*------------------------------------------------------------------*/
	private void resetButtons() {
		for (int i = 0; (i < TOOLS); i++) {
			down[i] = false;
		}
	} /* end resetButtons */

	/*------------------------------------------------------------------*/
	private void restorePreviousToolbar() {
		removeMouseListener(this);
		final Container container = instance.getParent();
		final Component component[] = container.getComponents();
		for (int n = 0, N = component.length; (n < N); n++) {
			if (component[n] == instance) {
				container.remove(instance);
				container.add(previousInstance, n);
				container.validate();
				break;
			}
		}
	} /* end restorePreviousToolbar */

	/*------------------------------------------------------------------*/
	private void showMessage(final int tool) {
		switch (tool) {
		case snake2DPointAction.ACCEPT: {
			IJ.showStatus("Done");
			return;
		}
		case snake2DPointAction.CANCEL: {
			IJ.showStatus("Abort");
			return;
		}
		case snake2DPointAction.MAGNIFIER: {
			IJ.showStatus("Magnifying glass");
			return;
		}
		case snake2DPointAction.MOVE_CROSS: {
			IJ.showStatus("Move crosses");
			return;
		}
		case snake2DPointAction.RESIZE: {
			IJ.showStatus("Resize snake");
			return;
		}
		case snake2DPointAction.ROTATE: {
			IJ.showStatus("Rotate snake");
			return;
		}
		default: {
			IJ.showStatus("Undefined operation");
			return;
		}
		}
	} /* end showMessage */

} /* end class snake2DEditToolbar */

/*
 * ==================================================================== |
 * snake2DPointAction
 * \===================================================================
 */

/*------------------------------------------------------------------*/
class snake2DPointAction extends ImageCanvas implements KeyListener

{ /* begin class snake2DPointAction */

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * ....................................................................
	 * protected variables
	 * ....................................................................
	 */
	protected static final int ACCEPT = 19;
	protected static final int CANCEL = 18;
	protected static final int MAGNIFIER = 11;
	protected static final int MOVE_CROSS = 0;
	protected static final int RESIZE = 1;
	protected static final int ROTATE = 2;
	protected static final int START = 3;
	protected static final int UNDEFINED = -1;

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private ImagePlus display = null;
	private Point mouse = null;
	private Snake2DKeeper keeper = null;
	private boolean active = false;
	private double angle = 0.0;
	private snake2DEditToolbar tb = null;
	private snake2DPointHandler ph = null;

	/*
	 * ....................................................................
	 * constructor methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected snake2DPointAction(final ImagePlus display,
			final snake2DPointHandler ph, final snake2DEditToolbar tb,
			final Snake2DKeeper keeper) {
		super(display);
		this.display = display;
		this.ph = ph;
		this.tb = tb;
		this.keeper = keeper;
		tb.setWindow(ph, display);
		tb.installListeners(this);
	} /* end snake2DPointAction */

	/*
	 * ....................................................................
	 * Canvas methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	public void focusGained(final FocusEvent e) {
		active = true;
		display.setRoi(ph);
	} /* end focusGained */

	/*------------------------------------------------------------------*/
	public void focusLost(final FocusEvent e) {
		active = false;
		display.setRoi(ph);
	} /* end focusLost */

	/*
	 * ....................................................................
	 * ImageCanvas methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void mouseClicked(final MouseEvent e) {
		active = true;
	} /* end mouseClicked */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseDragged(final MouseEvent e) {
		active = true;
		if (!keeper.isOptimizing()) {
			final int x = e.getX();
			final int y = e.getY();
			switch (tb.getCurrentTool()) {
			case MOVE_CROSS: {
				if (e.isShiftDown()) {
					double scale = display.getWindow().getCanvas()
							.getMagnification();
					scale = (1.0 < scale) ? (1.0 / scale) : (scale);
					final int xScaled = (int) round(round(x * scale) / scale);
					final int yScaled = (int) round(round(y * scale) / scale);
					ph.translatePoints(xScaled - mouse.x, yScaled - mouse.y);
					mouse.x = xScaled;
					mouse.y = yScaled;
				} else {
					ph.movePoint(x, y);
				}
				display.setRoi(ph);
				keeper.destroyOptimality();
				break;
			}
			case RESIZE: {
				if (e.isShiftDown()) {
					ph.resizePoints(x, y);
				} else {
					ph.stretchPoints(x, y);
				}
				display.setRoi(ph);
				keeper.destroyOptimality();
				break;
			}
			case ROTATE: {
				final double currentAngle = atan2(ph.getHullCenter().y - y,
						ph.getHullCenter().x - x);
				ph.rotatePoints(currentAngle - angle);
				angle = currentAngle;
				display.setRoi(ph);
				keeper.destroyOptimality();
				break;
			}
			}
		}
		mouseMoved(e);
	} /* end mouseDragged */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseEntered(final MouseEvent e) {
		active = true;
		WindowManager.setCurrentWindow(display.getWindow());
		display.getWindow().toFront();
		display.setRoi(ph);
	} /* end mouseEntered */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseExited(final MouseEvent e) {
		active = false;
		display.setRoi(ph);
		IJ.showStatus("");
	} /* end mouseExited */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseMoved(final MouseEvent e) {
		active = true;
		final int x = display.getWindow().getCanvas().offScreenX(e.getX());
		final int y = display.getWindow().getCanvas().offScreenY(e.getY());
		IJ.showStatus(display.getLocationAsString(x, y)
				+ getValueAsString(x, y));
	} /* end mouseMoved */

	/*------------------------------------------------------------------*/
	@Override
	public void mousePressed(final MouseEvent e) {
		active = true;
		final int x = e.getX();
		final int y = e.getY();
		switch (tb.getCurrentTool()) {
		case MAGNIFIER: {
			final int flags = e.getModifiers();
			if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) {
				display.getWindow().getCanvas().zoomOut(x, y);
			} else {
				display.getWindow().getCanvas().zoomIn(x, y);
			}
			break;
		}
		case MOVE_CROSS: {
			if (e.isShiftDown()) {
				double scale = display.getWindow().getCanvas()
						.getMagnification();
				scale = (1.0 < scale) ? (1.0 / scale) : (scale);
				mouse = new Point((int) round(round(x * scale) / scale),
						(int) round(round(y * scale) / scale));
			} else {
				ph.findClosestPoint(x, y);
			}
			break;
		}
		case RESIZE: {
			double scale = display.getWindow().getCanvas().getMagnification();
			scale = (1.0 < scale) ? (1.0 / scale) : (scale);
			mouse = new Point((int) round(round(x * scale) / scale),
					(int) round(round(y * scale) / scale));
			ph.findClosestHandle(x, y);
			break;
		}
		case ROTATE: {
			angle = atan2(ph.getHullCenter().y - y, ph.getHullCenter().x - x);
			break;
		}
		}
		display.setRoi(ph);
	} /* end mousePressed */

	/*------------------------------------------------------------------*/
	@Override
	public void mouseReleased(final MouseEvent e) {
		active = true;
		mouse = null;
	} /* end mouseReleased */

	/*
	 * ....................................................................
	 * KeyListener methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void keyPressed(final KeyEvent e) {
		active = true;
		final Point p = ph.getPoint();
		if (p == null) {
			return;
		}
		final int x = p.x;
		final int y = p.y;
		int scaledX;
		int scaledY;
		int scaledShiftedX;
		int scaledShiftedY;
		switch (tb.getCurrentTool()) {
		case MOVE_CROSS: {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_DOWN: {
				scaledX = display.getWindow().getCanvas().screenX(x);
				scaledShiftedY = display
						.getWindow()
						.getCanvas()
						.screenY(
								y
										+ (int) ceil(1.0 / display.getWindow()
												.getCanvas().getMagnification()));
				ph.movePoint(scaledX, scaledShiftedY);
				break;
			}
			case KeyEvent.VK_LEFT: {
				scaledShiftedX = display
						.getWindow()
						.getCanvas()
						.screenX(
								x
										- (int) ceil(1.0 / display.getWindow()
												.getCanvas().getMagnification()));
				scaledY = display.getWindow().getCanvas().screenY(y);
				ph.movePoint(scaledShiftedX, scaledY);
				break;
			}
			case KeyEvent.VK_RIGHT: {
				scaledShiftedX = display
						.getWindow()
						.getCanvas()
						.screenX(
								x
										+ (int) ceil(1.0 / display.getWindow()
												.getCanvas().getMagnification()));
				scaledY = display.getWindow().getCanvas().screenY(y);
				ph.movePoint(scaledShiftedX, scaledY);
				break;
			}
			case KeyEvent.VK_UP: {
				scaledX = display.getWindow().getCanvas().screenX(x);
				scaledShiftedY = display
						.getWindow()
						.getCanvas()
						.screenY(
								y
										- (int) ceil(1.0 / display.getWindow()
												.getCanvas().getMagnification()));
				ph.movePoint(scaledX, scaledShiftedY);
				break;
			}
			}
			break;
		}
		case RESIZE: {
			break;
		}
		case ROTATE: {
			break;
		}
		}
		display.setRoi(ph);
		updateStatus();
	} /* end keyPressed */

	/*------------------------------------------------------------------*/
	@Override
	public void keyReleased(final KeyEvent e) {
		active = true;
	} /* end keyReleased */

	/*------------------------------------------------------------------*/
	@Override
	public void keyTyped(final KeyEvent e) {
		active = true;
	} /* end keyTyped */

	/*
	 * ....................................................................
	 * protected methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected boolean isActive() {
		return (active);
	} /* end isActive */

	/*
	 * ....................................................................
	 * private methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	private String getValueAsString(final int x, final int y) {
		final Calibration cal = display.getCalibration();
		final int[] v = display.getPixel(x, y);
		switch (display.getType()) {
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16: {
			final double cValue = cal.getCValue(v[0]);
			if (cValue == v[0]) {
				return (", value=" + v[0]);
			} else {
				return (", value=" + IJ.d2s(cValue) + " (" + v[0] + ")");
			}
		}
		case ImagePlus.GRAY32: {
			return (", value=" + Float.intBitsToFloat(v[0]));
		}
		case ImagePlus.COLOR_256: {
			return (", index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
		}
		case ImagePlus.COLOR_RGB: {
			return (", value=" + v[0] + "," + v[1] + "," + v[2]);
		}
		default: {
			return ("");
		}
		}
	} /* end getValueAsString */

	/*------------------------------------------------------------------*/
	private void updateStatus() {
		final Point p = ph.getPoint();
		if (p == null) {
			IJ.showStatus("");
			return;
		}
		final int x = p.x;
		final int y = p.y;
		IJ.showStatus(display.getLocationAsString(x, y)
				+ getValueAsString(x, y));
	} /* end updateStatus */

} /* end class snake2DPointAction */

/*
 * ==================================================================== |
 * snake2DPointHandler
 * \===================================================================
 */

/*------------------------------------------------------------------*/
class snake2DPointHandler extends PolygonRoi

{ /* begin class snake2DPointHandler */

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private ImagePlus display = null;
	private final Point2D.Double hullCenter = new Point2D.Double();
	private final Point2D.Double[] snakeHull = new Point2D.Double[4];
	private Snake2DKeeper keeper = null;
	private Snake2DNode[] point = null;
	private Snake2D snake = null;
	private boolean started = false;
	private int closestHandle = 0;
	private int currentPoint = 0;
	private snake2DEditToolbar tb = null;
	private snake2DPointAction pa = null;
	private static final int CROSS_HALFSIZE = 5;
	private static final long serialVersionUID = 1L;

	/*
	 * ....................................................................
	 * constructor methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected snake2DPointHandler(final ImagePlus display, final Snake2D snake,
			final Snake2DNode[] point, final snake2DEditToolbar tb,
			final Snake2DKeeper keeper) {
		super(0, 0, display);
		this.display = display;
		this.snake = snake;
		this.point = point;
		this.keeper = keeper;
		this.tb = tb;
		if (point == null) {
			this.point = new Snake2DNode[0];
		}
		for (int k = 0, K = snakeHull.length; (k < K); k++) {
			snakeHull[k] = new Point2D.Double();
		}
	} /* end snake2DPointHandler */

	/*
	 * ....................................................................
	 * PolygonRoi methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void draw(final Graphics g) {
		if (started) {
			final double mag = ic.getMagnification();
			final int dx = (int) (mag / 2.0);
			final int dy = (int) (mag / 2.0);
			snake.setNodes(point);
			final boolean snakeDied = !snake.isAlive();
			keeper.setSnakeDied(snakeDied);
			if (snakeDied) {
				return;
			}
			Snake2DScale[] skin = snake.getScales();
			if (skin == null) {
				skin = new Snake2DScale[0];
			}
			for (int k = 0, K = skin.length; (k < K); k++) {
				final Color scaleColor = skin[k].bestAttemptColor;
				if (scaleColor == null) {
					g.setColor(ROIColor);
				} else {
					g.setColor(scaleColor);
				}
				final int[] xpoints = skin[k].xpoints;
				final int[] ypoints = skin[k].ypoints;
				final Polygon poly = new Polygon();
				for (int n = 0, N = skin[k].npoints; (n < N); n++) {
					poly.addPoint(ic.screenX(xpoints[n]) + dx,
							ic.screenY(ypoints[n]) + dy);
				}
				if (skin[k].closed && skin[k].filled) {
					g.fillPolygon(poly);
				} else {
					if (skin[k].closed) {
						g.drawPolygon(poly);
					} else {
						g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
					}
				}
			}
			g.setColor(ROIColor);
			if (!keeper.isOptimizing()) {
				switch (tb.getCurrentTool()) {
				case snake2DPointAction.RESIZE: {
					g.drawLine((int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y),
							(int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y));
					g.drawLine((int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y),
							(int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y));
					g.drawLine((int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y),
							(int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y));
					g.drawLine((int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y),
							(int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y));
					g.fillRect(
							(int) round(0.5 * (snakeHull[0].x + snakeHull[1].x)) - 2,
							(int) round(0.5 * (snakeHull[0].y + snakeHull[1].y)) - 2,
							5, 5);
					g.fillRect(
							(int) round(0.5 * (snakeHull[1].x + snakeHull[2].x)) - 2,
							(int) round(0.5 * (snakeHull[1].y + snakeHull[2].y)) - 2,
							5, 5);
					g.fillRect(
							(int) round(0.5 * (snakeHull[2].x + snakeHull[3].x)) - 2,
							(int) round(0.5 * (snakeHull[2].y + snakeHull[3].y)) - 2,
							5, 5);
					g.fillRect(
							(int) round(0.5 * (snakeHull[3].x + snakeHull[0].x)) - 2,
							(int) round(0.5 * (snakeHull[3].y + snakeHull[0].y)) - 2,
							5, 5);
					break;
				}
				case snake2DPointAction.ROTATE: {
					g.drawLine((int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y),
							(int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y));
					g.drawLine((int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y),
							(int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y));
					g.drawLine((int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y),
							(int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y));
					g.drawLine((int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y),
							(int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y));
					g.fillRect((int) round(snakeHull[0].x) - 2,
							(int) round(snakeHull[0].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[1].x) - 2,
							(int) round(snakeHull[1].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[2].x) - 2,
							(int) round(snakeHull[2].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[3].x) - 2,
							(int) round(snakeHull[3].y) - 2, 5, 5);
					break;
				}
				}
			}
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].hidden) {
					final Point p = new Point((int) round(point[k].getX()),
							(int) round(point[k].getY()));

					g.setColor(Color.BLACK);
					g.fillOval(
							ic.screenX(p.x - (int) (0.5 * CROSS_HALFSIZE) - 1)
									+ dx,
							ic.screenY(p.y - (int) (0.5 * CROSS_HALFSIZE) - 1)
									+ dy, CROSS_HALFSIZE, CROSS_HALFSIZE);

					// if (k == currentPoint) {
					// if (pa.isActive()) {
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1)
					// + dx, ic.screenY(p.y - 1) + dy,
					// ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y - 1) + dy);
					// g.drawLine(ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y - 1) + dy,
					// ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					// g.drawLine(ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
					// ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy);
					// g.drawLine(ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE - 1) + dy,
					// ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y - 1) + dy);
					// g.drawLine(ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y - 1) + dy,
					// ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
					// ic.screenY(p.y - 1) + dy);
					// g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1)
					// + dx, ic.screenY(p.y - 1) + dy,
					// ic.screenX(p.x + CROSS_HALFSIZE + 1) + dx,
					// ic.screenY(p.y + 1) + dy);
					// g.drawLine(ic.screenX(p.x + CROSS_HALFSIZE + 1)
					// + dx, ic.screenY(p.y + 1) + dy,
					// ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y + 1) + dy);
					// g.drawLine(ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y + 1) + dy,
					// ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					// g.drawLine(ic.screenX(p.x + 1) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
					// ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy);
					// g.drawLine(ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE + 1) + dy,
					// ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y + 1) + dy);
					// g.drawLine(ic.screenX(p.x - 1) + dx,
					// ic.screenY(p.y + 1) + dy,
					// ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
					// ic.screenY(p.y + 1) + dy);
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE - 1)
					// + dx, ic.screenY(p.y + 1) + dy,
					// ic.screenX(p.x - CROSS_HALFSIZE - 1) + dx,
					// ic.screenY(p.y - 1) + dy);
					// if (1.0 < ic.getMagnification()) {
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE)
					// + dx, ic.screenY(p.y) + dy,
					// ic.screenX(p.x + CROSS_HALFSIZE) + dx,
					// ic.screenY(p.y) + dy);
					// g.drawLine(ic.screenX(p.x) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE) + dy,
					// ic.screenX(p.x) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					// }
					// } else {
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1)
					// + dx, ic.screenY(p.y - CROSS_HALFSIZE + 1)
					// + dy, ic.screenX(p.x + CROSS_HALFSIZE - 1)
					// + dx, ic.screenY(p.y + CROSS_HALFSIZE - 1)
					// + dy);
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE + 1)
					// + dx, ic.screenY(p.y + CROSS_HALFSIZE - 1)
					// + dy, ic.screenX(p.x + CROSS_HALFSIZE - 1)
					// + dx, ic.screenY(p.y - CROSS_HALFSIZE + 1)
					// + dy);
					// }
					// } else {
					// g.drawLine(ic.screenX(p.x - CROSS_HALFSIZE) + dx,
					// ic.screenY(p.y) + dy,
					// ic.screenX(p.x + CROSS_HALFSIZE) + dx,
					// ic.screenY(p.y) + dy);
					// g.drawLine(ic.screenX(p.x) + dx,
					// ic.screenY(p.y - CROSS_HALFSIZE) + dy,
					// ic.screenX(p.x) + dx,
					// ic.screenY(p.y + CROSS_HALFSIZE) + dy);
					// }
				}
			}
			if (updateFullWindow) {
				updateFullWindow = false;
				display.draw();
			}
		}
	} /* end draw */

	/*
	 * ....................................................................
	 * protected methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected void activateDisplay() {
		started = true;
		display.setRoi(this);
	} /* end activateDisplay */

	/*------------------------------------------------------------------*/
	protected void deactivateDisplay() {
		display.killRoi();
		started = false;
	} /* end deactivateDisplay */

	/*------------------------------------------------------------------*/
	protected void findClosestHandle(final int x, final int y) {
		final Point2D.Double p = new Point2D.Double(x, y);
		closestHandle = 3;
		Point2D.Double handle = new Point2D.Double(
				0.5 * (snakeHull[3].x + snakeHull[0].x),
				0.5 * (snakeHull[3].y + snakeHull[0].y));
		double distanceSq = handle.distanceSq(p);
		for (int k = 0, K = snakeHull.length - 1; (k < K); k++) {
			handle = new Point2D.Double(
					0.5 * (snakeHull[k].x + snakeHull[k + 1].x),
					0.5 * (snakeHull[k].y + snakeHull[k + 1].y));
			final double candidateSq = handle.distanceSq(p);
			if (candidateSq < distanceSq) {
				distanceSq = candidateSq;
				closestHandle = k;
			}
		}
	} /* end findClosestHandle */

	/*------------------------------------------------------------------*/
	protected void findClosestPoint(int x, int y) {
		if (point.length == 0) {
			return;
		}
		x = ic.offScreenX(x);
		y = ic.offScreenY(y);
		double distanceSq = point[currentPoint].distanceSq(x, y);
		for (int k = 0, K = point.length; (k < K); k++) {
			final double candidateSq = point[k].distanceSq(x, y);
			if (candidateSq < distanceSq) {
				distanceSq = candidateSq;
				currentPoint = k;
			}
		}
	} /* end findClosestPoint */

	/*------------------------------------------------------------------*/
	protected Point2D.Double getHullCenter() {
		return (hullCenter);
	} /* end getHullCenter */

	/*------------------------------------------------------------------*/
	protected Point getPoint() {
		return (new Point((int) round(point[currentPoint].getX()),
				(int) round(point[currentPoint].getY())));
	} /* end getPoint */

	/*------------------------------------------------------------------*/
	protected Snake2DNode[] getPoints() {
		return (point);
	} /* end getPoints */

	/*------------------------------------------------------------------*/
	protected void movePoint(int x, int y) {
		if (!point[currentPoint].frozen) {
			x = ic.offScreenX(x);
			y = ic.offScreenY(y);
			x = (x < 0) ? (0) : (x);
			x = (display.getWidth() <= x) ? (display.getWidth() - 1) : (x);
			y = (y < 0) ? (0) : (y);
			y = (display.getHeight() <= y) ? (display.getHeight() - 1) : (y);
			point[currentPoint].setLocation(x, y);
		}
	} /* end movePoint */

	/*------------------------------------------------------------------*/
	protected void resetHull() {
		final double mag = ic.getMagnification();
		final int dx = (int) (mag / 2.0);
		final int dy = (int) (mag / 2.0);
		final Polygon poly = new Polygon();
		for (int k = 0, K = point.length; (k < K); k++) {
			poly.addPoint(ic.screenX((int) round(point[k].getX())) + dx,
					ic.screenY((int) round(point[k].getY())) + dy);
		}
		final Rectangle bounds = poly.getBounds();
		if (bounds.width == 0) {
			final int W = ic.screenX(display.getWidth());
			if (bounds.x < 0) {
				bounds.add(W / 2, bounds.y);
			} else {
				if (bounds.x < W) {
					if (bounds.x < (W / 2)) {
						bounds.add((bounds.x + W) / 2, bounds.y);
					} else {
						bounds.add(bounds.x / 2, bounds.y);
					}
				} else {
					bounds.add(W / 2, bounds.y);
				}
			}
		}
		if (bounds.height == 0) {
			final int H = ic.screenY(display.getHeight());
			if (bounds.y < 0) {
				bounds.add(bounds.x, H / 2);
			} else {
				if (bounds.y < H) {
					if (bounds.y < (H / 2)) {
						bounds.add(bounds.y, (bounds.y + H) / 2);
					} else {
						bounds.add(bounds.y, bounds.y / 2);
					}
				} else {
					bounds.add(bounds.y, H / 2);
				}
			}
		}
		snakeHull[0].x = bounds.x;
		snakeHull[0].y = bounds.y;
		snakeHull[1].x = bounds.x + bounds.width;
		snakeHull[1].y = bounds.y;
		snakeHull[2].x = bounds.x + bounds.width;
		snakeHull[2].y = bounds.y + bounds.height;
		snakeHull[3].x = bounds.x;
		snakeHull[3].y = bounds.y + bounds.height;
		hullCenter.x = 0.5 * (snakeHull[0].x + snakeHull[2].x);
		hullCenter.y = 0.5 * (snakeHull[0].y + snakeHull[2].y);
	} /* end resetHull */

	/*------------------------------------------------------------------*/
	protected void resizePoints(final int x, final int y) {
		double scale = 0.0;
		switch (closestHandle) {
		case 0: {
			if ((hullCenter.y - 1.0) < y) {
				return;
			}
			scale = ((y - hullCenter.y) / (snakeHull[0].y - hullCenter.y));
			break;
		}
		case 1: {
			if (x < (hullCenter.x + 1.0)) {
				return;
			}
			scale = ((x - hullCenter.x) / (snakeHull[1].x - hullCenter.x));
			break;
		}
		case 2: {
			if (y < (hullCenter.y + 1.0)) {
				return;
			}
			scale = ((y - hullCenter.y) / (snakeHull[2].y - hullCenter.y));
			break;
		}
		case 3: {
			if ((hullCenter.x - 1.0) < x) {
				return;
			}
			scale = ((x - hullCenter.x) / (snakeHull[3].x - hullCenter.x));
			break;
		}
		}
		final double mag = ic.getMagnification();
		final Rectangle srcRect = ic.getSrcRect();
		final double x0 = hullCenter.x / mag + srcRect.x;
		final double y0 = hullCenter.y / mag + srcRect.y;
		for (int k = 0, K = point.length; (k < K); k++) {
			if (!point[k].frozen) {
				point[k].setLocation(x0 + scale * (point[k].x - x0), y0 + scale
						* (point[k].y - y0));
			}
		}
		for (int k = 0, K = snakeHull.length; (k < K); k++) {
			snakeHull[k].x = hullCenter.x + scale
					* (snakeHull[k].x - hullCenter.x);
			snakeHull[k].y = hullCenter.y + scale
					* (snakeHull[k].y - hullCenter.y);
		}
	} /* end resizePoints */

	/*------------------------------------------------------------------*/
	protected void rotatePoints(final double angle) {
		final double c = cos(angle);
		final double s = sin(angle);
		for (int k = 0, K = snakeHull.length; (k < K); k++) {
			final double x = snakeHull[k].x - hullCenter.x;
			final double y = snakeHull[k].y - hullCenter.y;
			snakeHull[k].x = x * c - y * s + hullCenter.x;
			snakeHull[k].y = x * s + y * c + hullCenter.y;
		}
		final double mag = ic.getMagnification();
		final Rectangle srcRect = ic.getSrcRect();
		final double x0 = hullCenter.x / mag + srcRect.x;
		final double y0 = hullCenter.y / mag + srcRect.y;
		for (int k = 0, K = point.length; (k < K); k++) {
			if (!point[k].frozen) {
				final double x = point[k].x - x0;
				final double y = point[k].y - y0;
				point[k].setLocation(x * c - y * s + x0, x * s + y * c + y0);
			}
		}
	} /* end rotatePoints */

	/*------------------------------------------------------------------*/
	protected void setPointAction(final snake2DPointAction pa) {
		this.pa = pa;
	} /* end setPointAction */

	/*------------------------------------------------------------------*/
	protected void stretchPoints(final int x, final int y) {
		final double mag = ic.getMagnification();
		final Rectangle srcRect = ic.getSrcRect();
		switch (closestHandle) {
		case 0: {
			if ((snakeHull[2].y - 1.0) < y) {
				break;
			}
			final double scale = ((y - snakeHull[2].y) / (snakeHull[1].y - snakeHull[2].y));
			final double y0 = snakeHull[2].y / mag + srcRect.y;
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].frozen) {
					point[k].setLocation(point[k].x, y0 + scale
							* (point[k].y - y0));
				}
			}
			snakeHull[0].y = y;
			snakeHull[1].y = y;
			break;
		}
		case 1: {
			if (x < (snakeHull[0].x + 1.0)) {
				break;
			}
			final double scale = ((x - snakeHull[0].x) / (snakeHull[1].x - snakeHull[0].x));
			final double x0 = snakeHull[0].x / mag + srcRect.x;
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].frozen) {
					point[k].setLocation(x0 + scale * (point[k].x - x0),
							point[k].y);
				}
			}
			snakeHull[1].x = x;
			snakeHull[2].x = x;
			break;
		}
		case 2: {
			if (y < (snakeHull[1].y + 1.0)) {
				break;
			}
			final double scale = ((y - snakeHull[1].y) / (snakeHull[2].y - snakeHull[1].y));
			final double y0 = snakeHull[1].y / mag + srcRect.y;
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].frozen) {
					point[k].setLocation(point[k].x, y0 + scale
							* (point[k].y - y0));
				}
			}
			snakeHull[2].y = y;
			snakeHull[3].y = y;
			break;
		}
		case 3: {
			if ((snakeHull[1].x - 1.0) < x) {
				break;
			}
			final double scale = ((x - snakeHull[1].x) / (snakeHull[0].x - snakeHull[1].x));
			final double x0 = snakeHull[1].x / mag + srcRect.x;
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].frozen) {
					point[k].setLocation(x0 + scale * (point[k].x - x0),
							point[k].y);
				}
			}
			snakeHull[3].x = x;
			snakeHull[0].x = x;
			break;
		}
		}
	} /* end stretchPoints */

	/*------------------------------------------------------------------*/
	protected void translatePoints(int dx, int dy) {
		dx = (int) round(dx / ic.getMagnification());
		dy = (int) round(dy / ic.getMagnification());
		for (int k = 0, K = point.length; (k < K); k++) {
			if (!point[k].frozen) {
				point[k].setLocation(point[k].x + dx, point[k].y + dy);
			}
		}
	} /* end translatePoints */

} /* end class snake2DPointHandler */

/*
 * ==================================================================== |
 * snake2DSkinHandler
 * \===================================================================
 */

/*------------------------------------------------------------------*/
class snake2DSkinHandler extends PolygonRoi

{ /* begin class snake2DSkinHandler */

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * ....................................................................
	 * private variables
	 * ....................................................................
	 */
	private ImagePlus display = null;
	private Snake2DKeeper keeper = null;
	private Snake2DScale[] bestSkin = null;
	private Snake2D snake = null;
	private boolean started = false;

	/*
	 * ....................................................................
	 * constructor methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected snake2DSkinHandler(final ImagePlus display, final Snake2D snake,
			final Snake2DKeeper keeper) {
		super(0, 0, display);
		this.display = display;
		this.snake = snake;
		this.keeper = keeper;
	} /* end snake2DSkinHandler */

	/*
	 * ....................................................................
	 * PolygonRoi methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	@Override
	public void draw(final Graphics g) {
		if (started) {
			final boolean snakeDied = !snake.isAlive();
			keeper.setSnakeDied(snakeDied);
			if (snakeDied) {
				return;
			}
			Snake2DScale[] skin = snake.getScales();
			if (skin == null) {
				skin = new Snake2DScale[0];
			}
			final double mag = ic.getMagnification();
			final int dx = (int) (mag / 2.0);
			final int dy = (int) (mag / 2.0);
			for (int k = 0, K = skin.length; (k < K); k++) {
				Color scaleColor = skin[k].currentAttemptColor;
				if (scaleColor == null) {
					if (bestSkin == null) {
						g.setColor(ROIColor);
					} else {
						final int R = (0x7F + ROIColor.getRed()) & 0xFF;
						final int G = (0x7F + ROIColor.getGreen()) & 0xFF;
						final int B = (0x7F + ROIColor.getBlue()) & 0xFF;
						g.setColor(new Color(R, G, B));
					}
				} else {
					if (bestSkin == null) {
						scaleColor = skin[k].bestAttemptColor;
						if (scaleColor == null) {
							g.setColor(ROIColor);
						} else {
							g.setColor(scaleColor);
						}
					} else {
						g.setColor(scaleColor);
					}
				}
				final int[] xpoints = skin[k].xpoints;
				final int[] ypoints = skin[k].ypoints;
				final Polygon poly = new Polygon();
				for (int n = 0, N = skin[k].npoints; (n < N); n++) {
					poly.addPoint(ic.screenX(xpoints[n]) + dx,
							ic.screenY(ypoints[n]) + dy);
				}
				if (skin[k].closed && skin[k].filled) {
					g.fillPolygon(poly);
				} else {
					if (skin[k].closed) {
						g.drawPolygon(poly);
					} else {
						g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
					}
				}
			}
			if (bestSkin != null) {
				for (int k = 0, K = bestSkin.length; (k < K); k++) {
					final Color scaleColor = bestSkin[k].bestAttemptColor;
					if (scaleColor == null) {
						g.setColor(ROIColor);
					} else {
						g.setColor(scaleColor);
					}
					final int[] xpoints = bestSkin[k].xpoints;
					final int[] ypoints = bestSkin[k].ypoints;
					final Polygon poly = new Polygon();
					for (int n = 0, N = bestSkin[k].npoints; (n < N); n++) {
						poly.addPoint(ic.screenX(xpoints[n]) + dx,
								ic.screenY(ypoints[n]) + dy);
					}
					if (bestSkin[k].closed && bestSkin[k].filled) {
						g.fillPolygon(poly);
					} else {
						if (bestSkin[k].closed) {
							g.drawPolygon(poly);
						} else {
							g.drawPolyline(poly.xpoints, poly.ypoints,
									poly.npoints);
						}
					}
				}
			}
			if (updateFullWindow) {
				updateFullWindow = false;
				display.draw();
			}
		}
	} /* end draw */

	/*
	 * ....................................................................
	 * protected methods
	 * ....................................................................
	 */
	/*------------------------------------------------------------------*/
	protected void activateDisplay() {
		started = true;
		display.setRoi(this);
	} /* end activateDisplay */

	/*------------------------------------------------------------------*/
	protected void deactivateDisplay() {
		display.killRoi();
		started = false;
	} /* end deactivateDisplay */

	/*------------------------------------------------------------------*/
	protected void setBestSkin(final Snake2DScale[] bestSkin) {
		if (bestSkin == null) {
			this.bestSkin = null;
			return;
		}
		this.bestSkin = new Snake2DScale[bestSkin.length];
		for (int k = 0, K = bestSkin.length; (k < K); k++) {
			this.bestSkin[k] = new Snake2DScale(bestSkin[k].bestAttemptColor,
					bestSkin[k].currentAttemptColor, bestSkin[k].closed,
					bestSkin[k].filled);
			for (int n = 0, N = bestSkin[k].npoints; (n < N); n++) {
				this.bestSkin[k].addPoint(bestSkin[k].xpoints[n],
						bestSkin[k].ypoints[n]);
			}
		}
	} /* end setBestSkin */

} /* end class snake2DSkinHandler */
