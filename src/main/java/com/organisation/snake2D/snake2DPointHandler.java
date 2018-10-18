package com.organisation.snake2D;

import ij.ImagePlus;
import ij.gui.PolygonRoi;

import java.awt.*;
import java.awt.geom.Point2D;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;

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
public class snake2DPointHandler extends PolygonRoi

{ /* begin class snake2DPointHandler */

	/*
	 * .................................................................... private
	 * variables
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
	protected snake2DPointHandler(final ImagePlus display, final Snake2D snake, final Snake2DNode[] point,
                                  final snake2DEditToolbar tb, final Snake2DKeeper keeper) {
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
					poly.addPoint(ic.screenX(xpoints[n]) + dx, ic.screenY(ypoints[n]) + dy);
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
					g.drawLine((int) round(snakeHull[0].x), (int) round(snakeHull[0].y), (int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y));
					g.drawLine((int) round(snakeHull[1].x), (int) round(snakeHull[1].y), (int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y));
					g.drawLine((int) round(snakeHull[2].x), (int) round(snakeHull[2].y), (int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y));
					g.drawLine((int) round(snakeHull[3].x), (int) round(snakeHull[3].y), (int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y));
					g.fillRect((int) round(0.5 * (snakeHull[0].x + snakeHull[1].x)) - 2,
							(int) round(0.5 * (snakeHull[0].y + snakeHull[1].y)) - 2, 5, 5);
					g.fillRect((int) round(0.5 * (snakeHull[1].x + snakeHull[2].x)) - 2,
							(int) round(0.5 * (snakeHull[1].y + snakeHull[2].y)) - 2, 5, 5);
					g.fillRect((int) round(0.5 * (snakeHull[2].x + snakeHull[3].x)) - 2,
							(int) round(0.5 * (snakeHull[2].y + snakeHull[3].y)) - 2, 5, 5);
					g.fillRect((int) round(0.5 * (snakeHull[3].x + snakeHull[0].x)) - 2,
							(int) round(0.5 * (snakeHull[3].y + snakeHull[0].y)) - 2, 5, 5);
					break;
				}
				case snake2DPointAction.ROTATE: {
					g.drawLine((int) round(snakeHull[0].x), (int) round(snakeHull[0].y), (int) round(snakeHull[1].x),
							(int) round(snakeHull[1].y));
					g.drawLine((int) round(snakeHull[1].x), (int) round(snakeHull[1].y), (int) round(snakeHull[2].x),
							(int) round(snakeHull[2].y));
					g.drawLine((int) round(snakeHull[2].x), (int) round(snakeHull[2].y), (int) round(snakeHull[3].x),
							(int) round(snakeHull[3].y));
					g.drawLine((int) round(snakeHull[3].x), (int) round(snakeHull[3].y), (int) round(snakeHull[0].x),
							(int) round(snakeHull[0].y));
					g.fillRect((int) round(snakeHull[0].x) - 2, (int) round(snakeHull[0].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[1].x) - 2, (int) round(snakeHull[1].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[2].x) - 2, (int) round(snakeHull[2].y) - 2, 5, 5);
					g.fillRect((int) round(snakeHull[3].x) - 2, (int) round(snakeHull[3].y) - 2, 5, 5);
					break;
				}
				}
			}
			for (int k = 0, K = point.length; (k < K); k++) {
				if (!point[k].hidden) {
					final Point p = new Point((int) round(point[k].getX()), (int) round(point[k].getY()));

					g.setColor(Color.BLACK);
					g.fillOval(ic.screenX(p.x) - (int) (0.5 * CROSS_HALFSIZE) - 1 + dx,
							ic.screenY(p.y) - (int) (0.5 * CROSS_HALFSIZE) - 1 + dy, CROSS_HALFSIZE, CROSS_HALFSIZE);
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
		Point2D.Double handle = new Point2D.Double(0.5 * (snakeHull[3].x + snakeHull[0].x),
				0.5 * (snakeHull[3].y + snakeHull[0].y));
		double distanceSq = handle.distanceSq(p);
		for (int k = 0, K = snakeHull.length - 1; (k < K); k++) {
			handle = new Point2D.Double(0.5 * (snakeHull[k].x + snakeHull[k + 1].x),
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
		return (new Point((int) round(point[currentPoint].getX()), (int) round(point[currentPoint].getY())));
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
			poly.addPoint(ic.screenX((int) round(point[k].getX())) + dx, ic.screenY((int) round(point[k].getY())) + dy);
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
				point[k].setLocation(x0 + scale * (point[k].x - x0), y0 + scale * (point[k].y - y0));
			}
		}
		for (int k = 0, K = snakeHull.length; (k < K); k++) {
			snakeHull[k].x = hullCenter.x + scale * (snakeHull[k].x - hullCenter.x);
			snakeHull[k].y = hullCenter.y + scale * (snakeHull[k].y - hullCenter.y);
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
					point[k].setLocation(point[k].x, y0 + scale * (point[k].y - y0));
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
					point[k].setLocation(x0 + scale * (point[k].x - x0), point[k].y);
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
					point[k].setLocation(point[k].x, y0 + scale * (point[k].y - y0));
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
					point[k].setLocation(x0 + scale * (point[k].x - x0), point[k].y);
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
