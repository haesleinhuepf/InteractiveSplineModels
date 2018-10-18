package com.organisation;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import ij.IJ;
import ij.gui.Roi;
import com.organisation.snake2D.Snake2D;
import com.organisation.snake2D.Snake2DNode;
import com.organisation.snake2D.Snake2DScale;

/**
 * Exponential spline model
 * 
 * @version October 17, 2018
 * 
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 */
class InteractiveLSplineModel implements Snake2D {

	/** Snake defining nodes. */
	private Snake2DNode[] coef_ = null;

	/** LUT with the samples of the B-spline basis function at rate R. */
	private double[] splineFunc_ = null;
	/** Length of the support of the linear B-spline basis function */
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

	/** Number of coefficients. */
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

	// ============================================================================
	// PUBLIC METHODS

	/**
	 * Constructor.
	 */
	public InteractiveLSplineModel(int M, int width, int height, Roi initialContour) {
		if (M < Math.max(3, N)) {
			IJ.error("The minimum number of points for this basis function is " + Math.max(3, N));
			return;
		}

		M_ = M;
		initialContour_ = initialContour;

		NR_ = N * DISCRETIZATIONSAMPLINGRATE;
		MR_ = M * DISCRETIZATIONSAMPLINGRATE;
		PIM_ = Math.PI / M;
		PI2M_ = 2 * PIM_;

		width_ = width;
		height_ = height;

		xPosSkin_ = new double[MR_];
		yPosSkin_ = new double[MR_];

		buildLUTs();
		initializeContour();
		computePosSkin();
	}

	// ----------------------------------------------------------------------------

	/**
	 * Initializes the snake control points. If the input ImagePlus contains an area
	 * Roi, the snake will adapt to it.
	 */
	private void initializeContour() {
		coef_ = new Snake2DNode[M_];

		if (initialContour_ != null) {
			IJ.log(initialContour_.getTypeAsString() + " Roi detected.");
			int type = initialContour_.getType();
			if (type == Roi.RECTANGLE || type == Roi.OVAL || type == Roi.POLYGON || type == Roi.FREEROI
					|| type == Roi.TRACED_ROI) {
				IJ.log("Parsing...");
				Polygon p = initialContour_.getPolygon();
				if (p != null) {
					Point2D.Double[] resampledContour = arcLengthResampling(p, M_);
					coef_ = getSplineKnots(resampledContour);
				}
				return;
			} else {
				IJ.log("This type of Roi does not enclose any area.");
			}
		}

		IJ.log("Initializing default shape...");
		int radius = (int) (Math.min((double) width_ / 6, (double) height_ / 6));
		int x0 = width_ / 2;
		int y0 = height_ / 2;

		double K = 2 * (1 - Math.cos(PI2M_)) / (Math.cos(PIM_) - Math.cos(3 * PIM_));

		for (int i = 0; i < M_; i++) {
			coef_[i] = new Snake2DNode((int) (x0 + radius * K * Math.cos(PIM_ * (2 * i + 3))),
					(int) (y0 + radius * K * Math.sin(PIM_ * (2 * i + 3))));
		}
	}

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to compute the energy of the snake.
	 */
	@Override
	public double energy() {
		return (0.0);
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

	// ----------------------------------------------------------------------------

	/**
	 * This method provides an accessor to the number of nodes.
	 */
	public int getNumNodes() {
		return M_;
	}

	// ----------------------------------------------------------------------------

	/**
	 * The purpose of this method is to detemine what to draw on screen, given the
	 * current configuration of nodes.
	 */
	@Override
	public Snake2DScale[] getScales() {

		Snake2DScale[] skin = new Snake2DScale[1];
		skin[0] = new Snake2DScale(Color.RED, new Color(0, 0, 0, 0), true, false);

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
		return (skin);
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
		for (int i = 0; i < M_; i++) {
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
	 * Initializes all LUTs of the class.
	 */
	private void buildLUTs() {
		double currentVal;
		splineFunc_ = new double[NR_];

		for (int i = 0; i < NR_; i++) {
			currentVal = (double) i / (double) DISCRETIZATIONSAMPLINGRATE;
			splineFunc_[i] = BSpline1(currentVal);
		}
	}

	// ----------------------------------------------------------------------------

	/**
	 * Computes the contour of the snake from the control points.
	 */
	private void computePosSkin() {
		int index;

		double aux, xPosVal, yPosVal;
		for (int i = 0; i < MR_; i++) {
			xPosVal = 0.0;
			yPosVal = 0.0;
			for (int k = 0; k < M_; k++) {
				index = i - k * DISCRETIZATIONSAMPLINGRATE;

				while (index < 0) {
					index += MR_;
				}

				while (index >= MR_) {
					index -= MR_;
				}

				if (index >= NR_) {
					continue;
				} else {
					aux = splineFunc_[index];
				}
				xPosVal += coef_[k].x * aux;
				yPosVal += coef_[k].y * aux;
			}
			xPosSkin_[i] = xPosVal;
			yPosSkin_[i] = yPosVal;
		}
	}

	// ----------------------------------------------------------------------------

	/**
	 * Exponential B-spline of order three.
	 */
	private double BSpline1(double t) {
		double BSplineValue = 0.0;
		if ((t >= 0) & (t <= 1)) {
			BSplineValue = t;
		} else if ((t > 1) & (t <= 2)) {
			BSplineValue = 2 - t;
		}
		return (BSplineValue);
	}

	// ----------------------------------------------------------------------------

	/**
	 * Reparameterizes a curve to arc length parameterization with a given number of
	 * points.
	 */
	private Point2D.Double[] arcLengthResampling(Polygon p, int nPoints) {

		p.addPoint(p.xpoints[0], p.ypoints[0]);

		double[] arcLength = new double[p.npoints];
		arcLength[0] = 0;
		for (int i = 1; i < p.npoints; i++) {
			arcLength[i] = arcLength[i - 1]
					+ Math.sqrt((p.xpoints[i] - p.xpoints[i - 1]) * (p.xpoints[i] - p.xpoints[i - 1])
							+ (p.ypoints[i] - p.ypoints[i - 1]) * (p.ypoints[i] - p.ypoints[i - 1]));
		}

		Point2D.Double[] resampledCurve = new Point2D.Double[nPoints];
		double delta = arcLength[p.npoints - 1] / nPoints;
		int index = 0;
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
		}
		return resampledCurve;
	}

	/**
	 * Computes the location of the spline coefficients given an array of points the
	 * spline must interpolate using linear B-splines.
	 */
	private Snake2DNode[] getSplineKnots(Point2D.Double[] contour) {
		Snake2DNode[] newCoeff = new Snake2DNode[M_];
		for (int i = 0; i < M_; i++) {
			newCoeff[i] = new Snake2DNode(contour[i].x, contour[i].y);
		}
		return newCoeff;
	}
}