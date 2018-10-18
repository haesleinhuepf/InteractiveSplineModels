package com.virginieuhlmann;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import com.virginieuhlmann.snake2D.Snake2D;
import com.virginieuhlmann.snake2D.Snake2DNode;
import com.virginieuhlmann.snake2D.Snake2DScale;
import ij.IJ;
import ij.gui.Roi;

/**
 * Landmark spline model
 * 
 * @version October 17, 2018
 * 
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 */

public class InteractiveHSplineModel implements Snake2D {

	/** Snake defining nodes. */
	private Snake2DNode[] coef_ = null;

	/**
	 * LUT with the samples of the first Hermite spline basis function at rate R.
	 */
	private double[] splineFuncPoints_ = null;
	/**
	 * LUT with the samples of the second Hermite spline basis function at rate R.
	 */
	private double[] splineFuncDer_ = null;

	/** Length of the support of the cubic Hermite spline basis functions */
	private static int N = 2;

	/**
	 * LUT with the samples of the x coordinates of the snake contour at rate R.
	 */
	private double[] xPosSkin_ = null;
	/**
	 * LUT with the samples of the y coordinates of the snake contour at rate R.
	 */
	private double[] yPosSkin_ = null;

	/** Width of the original image data. */
	private int width_ = 0;
	/** Height of the original image data. */
	private int height_ = 0;

	/** Initial contour. */
	private Roi initialContour_ = null;

	/** If true indicates that the snake is able to keep being optimized. */
	private boolean alive_ = true;

	/**
	 * If true, indicates that the user chose to interactively abort the processing
	 * of the snake. Otherwise, if false, indicates that the dealings with the snake
	 * were terminated without user assistance.
	 */
	private boolean canceledByUser_ = false;

	/** Number of spline vector coefficients. */
	private int M_ = 0;

	/** PI/M. */
	private double PIM_ = 0;
	/** 2*PI/M. */
	private double PI2M_ = 0;

	/** Sampling rate at which the contours are discretized. */
	private static final int DISCRETIZATIONSAMPLINGRATE = 500;
	/** N*DISCRETIZATIONSAMPLINGRATE. */
	private int NR_ = 0;
	/** M*DISCRETIZATIONSAMPLINGRATE. */
	private int MR_ = 0;

	/** Width of tangent vector arrow for display. */
	private final double ARROWWIDTH = 4.0;
	/** Length of tangent vector arrow for display. */
	private final double ARROWLENGTH = 8.0;

	/** Displayed length of tangent vectors w.r.t actual derivative value. */
	private double tangentWeight_ = 1.0 / 3.0;

	// ============================================================================
	// PUBLIC METHODS

	/**
	 * Constructor.
	 */
	public InteractiveHSplineModel(int M, int width, int height, Roi initialContour) {
		if (M < 2) {
			IJ.error("The minimum number of points for this basis function is two.");
			return;
		}

		M_ = M;
		initialContour_ = initialContour;

		width_ = width;
		height_ = height;

		NR_ = N * DISCRETIZATIONSAMPLINGRATE;
		MR_ = M * DISCRETIZATIONSAMPLINGRATE;
		PIM_ = Math.PI / M;
		PI2M_ = 2 * PIM_;

		xPosSkin_ = new double[MR_];
		yPosSkin_ = new double[MR_];

		buildLUTs();
		initializeContour();
		computePosSkin();
	}

	/**
	 * The purpose of this method is to compute the energy of the snake.
	 */
	@Override
	public double energy() {
		return 0.0;
	}

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to compute the gradient of the snake energy
	 * with respect to the snake-defining nodes.
	 */
	@Override
	public Point2D.Double[] getEnergyGradient() {
		return null;
	}

	// ----------------------------------------------------------------------------

	/**
	 * This method provides an accessor to the snake-defining nodes.
	 */
	@Override
	public Snake2DNode[] getNodes() {
		return (coef_);
	}

	public int getNumNodes() {
		return M_;
	}

	public double getTangentWeight() {
		return tangentWeight_;
	}

	public Point2D getTangents(int k) {
		if (k > M_ - 1 || k < 0) {
			IJ.error("Index outside snake bound.");
			return null;
		}

		Point2D tan = new Point2D.Double(coef_[M_ + k].x, coef_[M_ + k].y);
		return tan;
	}

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to determine what to draw on screen, given the
	 * current configuration of nodes.
	 */
	@Override
	public Snake2DScale[] getScales() {
		int offset = 1;

		Snake2DScale[] skin;
		skin = new Snake2DScale[offset + (2 * M_)];

		skin[0] = new Snake2DScale(Color.RED, new Color(0, 0, 0, 0), true, false);
		// Set points
		int rxt, ryt;
		for (int k = 0; k < MR_; k++) {
			rxt = (int) Math.round(xPosSkin_[k] + 0.5);
			ryt = (int) Math.round(yPosSkin_[k] + 0.5);

			if (rxt < 0) {
				rxt = 0;
			} else if (rxt >= width_) {
				rxt = width_ - 1;
			}

			if (ryt < 0) {
				ryt = 0;
			} else if (ryt >= height_) {
				ryt = height_ - 1;
			}

			skin[0].addPoint(rxt, ryt);
		}

		// Set tangents
		for (int k = 0; k < M_; k++) {
			skin[offset + k] = new Snake2DScale(Color.BLACK, new Color(0, 0, 0, 0), true, false);

			skin[offset + k].addPoint((int) Math.round(coef_[k].x), (int) Math.round(coef_[k].y));
			skin[offset + k].addPoint((int) Math.round(coef_[k].x + tangentWeight_ * coef_[M_ + k].x),
					(int) Math.round(coef_[k].y + tangentWeight_ * coef_[M_ + k].y));
		}

		// Set arrowheads
		for (int k = 0; k < M_; k++) {
			skin[offset + M_ + k] = getArrowHead(coef_[k], coef_[M_ + k]);
		}
		return (skin);
	}

	public Snake2DScale getArrowHead(Snake2DNode c, Snake2DNode d) {
		Snake2DScale arrowhead = new Snake2DScale(Color.BLACK, new Color(0, 0, 0, 0), true, true);

		arrowhead.addPoint((int) Math.round(c.x + tangentWeight_ * d.x), (int) Math.round(c.y + tangentWeight_ * d.y));

		double dist = tangentWeight_ * Math.sqrt((d.x * d.x) + (d.y * d.y));
		double l = dist - ARROWLENGTH;
		Snake2DNode t = new Snake2DNode(tangentWeight_ * d.x / dist, tangentWeight_ * d.y / dist);

		arrowhead.addPoint((int) Math.round(c.x + (t.x * l) + (-t.y * ARROWWIDTH)),
				(int) Math.round(c.y + (t.y * l) + (t.x * ARROWWIDTH)));
		arrowhead.addPoint((int) Math.round(c.x + (t.x * l) - (-t.y * ARROWWIDTH)),
				(int) Math.round(c.y + (t.y * l) - (t.x * ARROWWIDTH)));

		return arrowhead;
	}

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to monitor the status of the snake.
	 */
	@Override
	public boolean isAlive() {
		return alive_;
	}

	// ----------------------------------------------------------------------------

	/**
	 * This method provides a mutator to the snake-defining nodes.
	 */
	@Override
	public void setNodes(Snake2DNode[] node) {
		for (int i = 0; i < 2 * M_; i++) {
			coef_[i].x = node[i].x;
			coef_[i].y = node[i].y;
		}
		computePosSkin();
	}

	// ----------------------------------------------------------------------------

	/**
	 * If true, indicates that the user chose to interactively abort the processing
	 * of the snake. Otherwise, if false, indicates that the dealings with the snake
	 * were terminated without user assistance.
	 */
	public boolean isCanceledByUser() {
		return (canceledByUser_);
	}

	// ----------------------------------------------------------------------------

	/**
	 * This method is called when the methods Snake2DKeeper.interact(),
	 * Snake2DKeeper.interactAndOptimize(), and Snake2DKeeper.optimize() are about
	 * to terminate. It provides a report on the current status of this snake.
	 */
	@Override
	public void updateStatus(boolean canceledByUser, boolean snakeDied, boolean optimalSnakeFound, Double energy) {
		canceledByUser_ = canceledByUser;
	}

	// ============================================================================
	// PRIVATE METHODS
	// ----------------------------------------------------------------------------

	/**
	 * Initializes the snake control points. If the input ImagePlus contains an area
	 * ROI, the method computes the snake control points to fit to the shape.
	 */
	private void initializeContour() {
		coef_ = new Snake2DNode[2 * M_];
		double alpha = 6;
		int radius = (int) (Math.min((width_ / 2.0) * (alpha * 20.0), (height_ / 2.0) * (alpha / 20.0)));
		int x0 = width_ / 2;
		int y0 = height_ / 2;

		if (initialContour_ != null) {
			IJ.log(initialContour_.getTypeAsString() + " Roi detected.");
			int type = initialContour_.getType();
			if (type == Roi.OVAL || type == Roi.FREEROI || type == Roi.TRACED_ROI) {
				IJ.log("Parsing...");
				Polygon p = initialContour_.getPolygon();
				if (p != null) {
					Point2D.Double[][] resampledContour = arcLengthResampling(p, M_);
					for (int i = 0; i < M_; i++) {
						coef_[i] = new Snake2DNode(resampledContour[0][i].x, resampledContour[0][i].y);
						coef_[i + M_] = new Snake2DNode(resampledContour[1][i].x, resampledContour[1][i].y);
					}
				}
				return;
			} else if (type == Roi.RECTANGLE || type == Roi.POLYGON) {
				IJ.log("Parsing polygon...");
				Polygon p = initialContour_.getPolygon();
				if (p != null) {
					if (M_ % p.npoints != 0) {
						IJ.log("ERROR: number of control points (" + M_ + ") do not match polygon settings ("
								+ p.npoints + " points). Ignoring input ROI.");
					} else {
						int inter = M_ / p.npoints;
						for (int i = 0; i < p.npoints; i++) {
							coef_[inter * i] = new Snake2DNode(p.xpoints[i], p.ypoints[i]);
							coef_[(inter * i) + M_] = new Snake2DNode(0.0, 0.0);

							double dx, dy;
							if (i < p.npoints - 1) {
								dx = p.xpoints[i + 1] - p.xpoints[i];
								dy = p.ypoints[i + 1] - p.ypoints[i];
							} else {
								dx = p.xpoints[0] - p.xpoints[i];
								dy = p.ypoints[0] - p.ypoints[i];
							}
							for (int j = 1; j < inter; j++) {
								coef_[(inter * i) + j] = new Snake2DNode(p.xpoints[i] + j * (dx / inter),
										p.ypoints[i] + j * (dy / inter));
								coef_[(inter * i) + j + M_] = new Snake2DNode(0.0, 0.0);
							}
						}
						return;
					}
				}
			} else if (type == Roi.POINT) {
				Polygon p = initialContour_.getPolygon();
				if (p != null && p.npoints == 1) {
					x0 = p.xpoints[0];
					y0 = p.ypoints[0];
				}
			} else {
				IJ.log("This type of Roi cannot be used to initialize a closed snake.");
			}
		}

		IJ.log("Initializing default snake...");
		if (M_ > 2) {
			for (int i = 0; i < M_; i++) {
				coef_[i] = new Snake2DNode((int) (x0 + radius * Math.cos(PIM_ * (2 * i + 2))),
						(int) (y0 + radius * Math.sin(PIM_ * (2 * i + 2))));
				coef_[M_ + i] = new Snake2DNode((PIM_ * 2 * radius * -Math.sin(PIM_ * (2 * i + 2))),
						(PIM_ * 2 * radius * Math.cos(PIM_ * (2 * i + 2))));
			}
		} else {
			coef_[0] = new Snake2DNode((int) ((double) x0 + radius), y0);
			coef_[1] = new Snake2DNode((int) ((double) x0 - radius), y0);

			coef_[M_] = new Snake2DNode(0.0, 2.0 * radius);
			coef_[M_ + 1] = new Snake2DNode(0.0, -2.0 * radius);
		}
	}

	/**
	 * Reparameterizes a curve to arc length parameterization with a given number of
	 * points.
	 */
	private Point2D.Double[][] arcLengthResampling(Polygon p, int nPoints) {
		double[] arcLength = new double[p.npoints];
		arcLength[0] = 0;
		for (int i = 1; i < p.npoints; i++) {
			arcLength[i] = arcLength[i - 1]
					+ Math.sqrt((p.xpoints[i] - p.xpoints[i - 1]) * (p.xpoints[i] - p.xpoints[i - 1])
							+ (p.ypoints[i] - p.ypoints[i - 1]) * (p.ypoints[i] - p.ypoints[i - 1]));
		}

		Point2D.Double[] resampledCurve = new Point2D.Double[nPoints];
		Point2D.Double[] resampledDerivatives = new Point2D.Double[nPoints];
		double delta = arcLength[p.npoints - 1] / nPoints;
		double eps = 0.1;
		int index = 0;
		int indexleft = 0;
		int indexright = 0;
		for (int i = 0; i < nPoints; i++) {
			double t = delta * i;
			boolean found = false;
			for (; index < (p.npoints - 1) && !found; index++) {
				if (arcLength[index] <= t && arcLength[index + 1] >= t) {
					found = true;
				}
			}
			index--;
			resampledCurve[i] = new Point2D.Double(
					((arcLength[index + 1] - t) * p.xpoints[index] + (t - arcLength[index]) * p.xpoints[index + 1])
							/ (arcLength[index + 1] - arcLength[index]),
					((arcLength[index + 1] - t) * p.ypoints[index] + (t - arcLength[index]) * p.ypoints[index + 1])
							/ (arcLength[index + 1] - arcLength[index]));

			double alpha = 1.0 / (2.0 * eps);
			Point2D.Double left;
			Point2D.Double right;

			double tleft = t - (eps * delta);
			if (tleft < 0) {
				left = resampledCurve[i];
				alpha = 1.0;
			} else {
				found = false;
				for (; indexleft < (p.npoints - 1) && !found; indexleft++) {
					if (arcLength[indexleft] <= tleft && arcLength[indexleft + 1] >= tleft) {
						found = true;
					}
				}
				indexleft--;

				left = new Point2D.Double(
						((arcLength[indexleft + 1] - tleft) * p.xpoints[indexleft]
								+ (tleft - arcLength[indexleft]) * p.xpoints[indexleft + 1])
								/ (arcLength[indexleft + 1] - arcLength[indexleft]),
						((arcLength[indexleft + 1] - tleft) * p.ypoints[indexleft]
								+ (tleft - arcLength[indexleft]) * p.ypoints[indexleft + 1])
								/ (arcLength[indexleft + 1] - arcLength[indexleft]));
			}

			double tright = t + (eps * delta);
			if (tright > arcLength[p.npoints - 1]) {
				right = resampledCurve[i];
				alpha = 1.0;
			} else {
				found = false;
				for (; indexright < (p.npoints - 1) && !found; indexright++) {
					if (arcLength[indexright] <= tright && arcLength[indexright + 1] >= tright) {
						found = true;
					}
				}
				indexright--;

				right = new Point2D.Double(
						((arcLength[indexright + 1] - tright) * p.xpoints[indexright]
								+ (tright - arcLength[indexright]) * p.xpoints[indexright + 1])
								/ (arcLength[indexright + 1] - arcLength[indexright]),
						((arcLength[indexright + 1] - tright) * p.ypoints[indexright]
								+ (tright - arcLength[indexright]) * p.ypoints[indexright + 1])
								/ (arcLength[indexright + 1] - arcLength[indexright]));
			}

			resampledDerivatives[i] = new Point2D.Double(alpha * (right.getX() - left.getX()),
					alpha * (right.getY() - left.getY()));
		}
		return new Point2D.Double[][] { resampledCurve, resampledDerivatives };
	}

	// ----------------------------------------------------------------------------

	/**
	 * Initializes all LUTs of the class.
	 */
	private void buildLUTs() {
		double currentVal;

		splineFuncPoints_ = new double[NR_];
		splineFuncDer_ = new double[NR_];

		for (int i = 0; i < NR_; i++) {
			currentVal = (double) i / (double) DISCRETIZATIONSAMPLINGRATE;
			splineFuncPoints_[i] = HSpline31(currentVal);
			splineFuncDer_[i] = HSpline32(currentVal);
		}
	}

	// ----------------------------------------------------------------------------

	/**
	 * Computes the contour of the snake from the control points.
	 */
	private void computePosSkin() {
		int index;

		double aux, aux2, xPosVal, yPosVal;
		for (int i = 0; i < MR_; i++) {
			xPosVal = 0.0;
			yPosVal = 0.0;
			for (int k = 0; k < M_; k++) {
				index = i - k * DISCRETIZATIONSAMPLINGRATE + DISCRETIZATIONSAMPLINGRATE;

				while (index < 0) {
					index += MR_;
				}

				while (index >= MR_) {
					index -= MR_;
				}

				if (index >= NR_) {
					continue;
				} else {
					aux = splineFuncPoints_[index];
					aux2 = splineFuncDer_[index];
				}
				xPosVal += (coef_[k].x * aux) + (coef_[k + M_].x * aux2);
				yPosVal += (coef_[k].y * aux) + (coef_[k + M_].y * aux2);
			}
			xPosSkin_[i] = xPosVal;
			yPosSkin_[i] = yPosVal;
		}
	}

	// ----------------------------------------------------------------------------

	/**
	 * First Cubic Hermite spline.
	 */
	private double HSpline31(double x) {
		x = x - 1;

		double val = 0.0;
		if (x >= 0 && x <= 1) {
			val = (1.0 + (2.0 * x)) * (x - 1) * (x - 1);
		} else if (x < 0 && x >= -1) {
			val = (1.0 - (2.0 * x)) * (x + 1) * (x + 1);
		}
		return val;
	}

	// ----------------------------------------------------------------------------

	/**
	 * Second Cubic Hermite spline.
	 */
	private double HSpline32(double x) {
		x = x - 1;

		double val = 0.0;
		if (x >= 0 && x <= 1) {
			val = x * (x - 1) * (x - 1);
		} else if (x < 0 && x >= -1) {
			val = x * (x + 1) * (x + 1);
		}
		return val;
	}
}