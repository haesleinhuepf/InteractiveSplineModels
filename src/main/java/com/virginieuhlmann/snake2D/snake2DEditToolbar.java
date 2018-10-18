package com.virginieuhlmann.snake2D;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * This class encapsulates the interactive and managerial aspects of snakes. It
 * handles objects that implement the <code>Snake2D</code> interface.
 *
 * @version October 31, 2012
 *
 * @author Ricard Delgado-Gonzalo (ricard.delgado@gmail.com)
 * @author Philippe Th&#233;venaz (philippe.thevenaz@epfl.ch)
 */
/*------------------------------------------------------------------*/
public class snake2DEditToolbar extends Canvas implements AdjustmentListener, MouseListener

{ /* begin class snake2DEditToolbar */

	/*
	 * .................................................................... private
	 * variables
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
	protected snake2DEditToolbar(final Toolbar previousToolbar, final Snake2DKeeper keeper) {
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
	 * .................................................................... Canvas
	 * methods ....................................................................
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
	protected void setWindow(final snake2DPointHandler ph, final ImagePlus display) {
		this.ph = ph;
		this.display = display;
	} /* end setWindow */

	/*------------------------------------------------------------------*/
	protected void terminateInteraction(final Snake2D snake, final snake2DPointHandler ph) {
		cleanUpListeners();
		restorePreviousToolbar();
		Toolbar.getInstance().repaint();
		snake.setNodes(ph.getPoints());
		ph.deactivateDisplay();
	} /* end terminateInteraction */

	/*
	 * .................................................................... private
	 * methods ....................................................................
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
		fill3DRect(g, tool * TOOL_SIZE + 1, 1, TOOL_SIZE, TOOL_SIZE - 1, !down[tool]);
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
	private void fill3DRect(final Graphics g, final int x, final int y, final int width, final int height,
			final boolean raised) {
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
