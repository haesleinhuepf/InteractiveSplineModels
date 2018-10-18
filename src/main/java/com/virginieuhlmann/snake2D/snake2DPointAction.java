package com.virginieuhlmann.snake2D;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.measure.Calibration;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.round;

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
public class snake2DPointAction extends ImageCanvas implements KeyListener

{ /* begin class snake2DPointAction */

	/*
	 * .................................................................... private
	 * variables
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
	 * .................................................................... private
	 * variables
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
	protected snake2DPointAction(final ImagePlus display, final snake2DPointHandler ph, final snake2DEditToolbar tb,
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
	 * .................................................................... Canvas
	 * methods ....................................................................
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
					double scale = display.getWindow().getCanvas().getMagnification();
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
				final double currentAngle = atan2(ph.getHullCenter().y - y, ph.getHullCenter().x - x);
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
		IJ.showStatus(display.getLocationAsString(x, y) + getValueAsString(x, y));
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
				double scale = display.getWindow().getCanvas().getMagnification();
				scale = (1.0 < scale) ? (1.0 / scale) : (scale);
				mouse = new Point((int) round(round(x * scale) / scale), (int) round(round(y * scale) / scale));
			} else {
				ph.findClosestPoint(x, y);
			}
			break;
		}
		case RESIZE: {
			double scale = display.getWindow().getCanvas().getMagnification();
			scale = (1.0 < scale) ? (1.0 / scale) : (scale);
			mouse = new Point((int) round(round(x * scale) / scale), (int) round(round(y * scale) / scale));
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
				scaledShiftedY = display.getWindow().getCanvas()
						.screenY(y + (int) ceil(1.0 / display.getWindow().getCanvas().getMagnification()));
				ph.movePoint(scaledX, scaledShiftedY);
				break;
			}
			case KeyEvent.VK_LEFT: {
				scaledShiftedX = display.getWindow().getCanvas()
						.screenX(x - (int) ceil(1.0 / display.getWindow().getCanvas().getMagnification()));
				scaledY = display.getWindow().getCanvas().screenY(y);
				ph.movePoint(scaledShiftedX, scaledY);
				break;
			}
			case KeyEvent.VK_RIGHT: {
				scaledShiftedX = display.getWindow().getCanvas()
						.screenX(x + (int) ceil(1.0 / display.getWindow().getCanvas().getMagnification()));
				scaledY = display.getWindow().getCanvas().screenY(y);
				ph.movePoint(scaledShiftedX, scaledY);
				break;
			}
			case KeyEvent.VK_UP: {
				scaledX = display.getWindow().getCanvas().screenX(x);
				scaledShiftedY = display.getWindow().getCanvas()
						.screenY(y - (int) ceil(1.0 / display.getWindow().getCanvas().getMagnification()));
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
	 * .................................................................... private
	 * methods ....................................................................
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
		IJ.showStatus(display.getLocationAsString(x, y) + getValueAsString(x, y));
	} /* end updateStatus */

} /* end class snake2DPointAction */
