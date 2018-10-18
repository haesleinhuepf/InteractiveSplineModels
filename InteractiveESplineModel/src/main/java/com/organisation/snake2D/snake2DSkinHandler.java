package com.organisation.snake2D;

import ij.ImagePlus;
import ij.gui.PolygonRoi;

import java.awt.*;

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
public class snake2DSkinHandler extends PolygonRoi

{ /* begin class snake2DSkinHandler */

	/*
	 * .................................................................... private
	 * variables
	 * ....................................................................
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * .................................................................... private
	 * variables
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
	protected snake2DSkinHandler(final ImagePlus display, final Snake2D snake, final Snake2DKeeper keeper) {
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
						poly.addPoint(ic.screenX(xpoints[n]) + dx, ic.screenY(ypoints[n]) + dy);
					}
					if (bestSkin[k].closed && bestSkin[k].filled) {
						g.fillPolygon(poly);
					} else {
						if (bestSkin[k].closed) {
							g.drawPolygon(poly);
						} else {
							g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
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
			this.bestSkin[k] = new Snake2DScale(bestSkin[k].bestAttemptColor, bestSkin[k].currentAttemptColor,
					bestSkin[k].closed, bestSkin[k].filled);
			for (int n = 0, N = bestSkin[k].npoints; (n < N); n++) {
				this.bestSkin[k].addPoint(bestSkin[k].xpoints[n], bestSkin[k].ypoints[n]);
			}
		}
	} /* end setBestSkin */

} /* end class snake2DSkinHandler */
