package com.organisation;

import java.awt.Checkbox;
import java.awt.TextField;
import java.io.File;
import java.nio.file.FileSystems;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.organisation.hsnake2D.HSnake2DKeeper;
import com.organisation.snake2D.Snake2DNode;
import com.organisation.snake2D.Snake2DScale;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * Implements ExtendedPlugInFilter to use InteractiveESplineModel as a plugin
 * for ImageJ
 * 
 * @version October 17, 2018
 * 
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 */
public class InteractiveHSplineModel_ implements ExtendedPlugInFilter {

	/** Image to process. */
	private ImagePlus imp_ = null;

	/** Initial dialog. */
	private final GenericDialog dialog_ = new GenericDialog("Interactive E-Spline Model");

	/** Input image types allowed. */
	private static final int CAPABILITIES = DOES_8G | DOES_16 | DOES_32 | CONVERT_TO_FLOAT;

	/** Default number of snake control points. */
	private static final int DEFAULT_NUM_NODES = 5;

	/** Label for the number of snake control points. */
	private static final String NUM_NODES = "Control_points";
	/** Label for the saving into the RoiManager of ImageJ. */
	private static final String SAVE = "Save_ROI";
	/** Label for the saving as XML file. */
	private static final String SAVEXML = "Save_XML";
	/** Textfield for the XML source file. */
	private static final String XMLSOURCE = "XML_Source";
	/** Textfield for the XML output file. */
	private static final String XMLDEST = "XML_Output";

	/** Number of control points. */
	private static int M_ = DEFAULT_NUM_NODES;
	/** If true, the result is stored in the RoiManager of ImageJ. */
	private static boolean saveROI_ = true;
	/** If true, the result is saved as XML file. */
	private static boolean saveXML_ = false;

	/** Path to the XML source file. */
	private static String xmlSource_ = "";
	/** Path to the XML output file. */
	private static String xmlDest_ = "myspline.xml";

	// ============================================================================
	// PUBLIC METHODS

	@Override
	public void run(ImageProcessor ip) {
		@SuppressWarnings("unchecked")
		final Vector<TextField> numbers = dialog_.getNumericFields();
		@SuppressWarnings("unchecked")
		final Vector<Checkbox> checkboxes = dialog_.getCheckboxes();

		xmlSource_ = dialog_.getNextString();
		M_ = (new Integer(numbers.elementAt(0).getText())).intValue();
		saveROI_ = checkboxes.elementAt(0).getState();
		saveXML_ = checkboxes.elementAt(1).getState();
		xmlDest_ = dialog_.getNextString();

		Recorder.setCommand("InteractiveESplineModel ");
		Recorder.recordOption(XMLSOURCE, xmlSource_);
		Recorder.recordOption(NUM_NODES, "" + M_);
		Recorder.recordOption(SAVE, "" + saveROI_);
		Recorder.recordOption(SAVEXML, "" + saveXML_);
		Recorder.recordOption(XMLDEST, xmlDest_);

		if (saveROI_)
			Recorder.saveCommand();

		Snake2DNode[] priorNodes = null;
		if (!xmlSource_.isEmpty()) {
			try {
				priorNodes = loadModelFromXML(xmlSource_);
				M_ = priorNodes.length / 2;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		InteractiveHSplineModel myModel = new InteractiveHSplineModel(M_, ip.getWidth(), ip.getHeight(), imp_.getRoi());

		if (priorNodes != null) {
			IJ.log("Setting initial nodes from XML...");
			myModel.setNodes(priorNodes);
		}

		HSnake2DKeeper keeper = new HSnake2DKeeper();
		keeper.interact(myModel, imp_, myModel.getTangentWeight());

		if (!myModel.isCanceledByUser()) {
			if (saveROI_) {
				RoiManager roiManager = RoiManager.getInstance();
				if (roiManager == null)
					roiManager = new RoiManager();

				Snake2DScale[] skin = myModel.getScales();
				PolygonRoi roi = new PolygonRoi(skin[0], Roi.TRACED_ROI);
				if (saveROI_)
					roiManager.addRoi(roi);
				imp_.setRoi(roi);
			}

			if (saveXML_ && !xmlDest_.isEmpty()) {
				try {
					saveModelToXML(xmlDest_, myModel, imp_);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// ----------------------------------------------------------------------------

	@Override
	public void setNPasses(int nPasses) {
	}

	// ----------------------------------------------------------------------------

	@Override
	public int setup(String arg, ImagePlus imp) {
		imp_ = imp;
		return (CAPABILITIES);
	}

	// ----------------------------------------------------------------------------

	@Override
	public int showDialog(final ImagePlus imp, final String command, final PlugInFilterRunner pfr) {
		dialog_.addStringField(XMLSOURCE, xmlSource_, 30);
		dialog_.addNumericField(NUM_NODES, M_, 0);
		dialog_.addCheckbox(SAVE, saveROI_);
		dialog_.addCheckbox(SAVEXML, saveXML_);
		dialog_.addStringField(XMLDEST, xmlDest_, 30);

		dialog_.addPanel(new IHSMCreditsButton());

		if (Macro.getOptions() != null) {
			activateMacro(imp);
			return (CAPABILITIES);
		}
		dialog_.showDialog();
		if (dialog_.wasCanceled()) {
			return (DONE);
		}
		if (dialog_.wasOKed()) {
			return (CAPABILITIES);
		} else {
			return (DONE);
		}
	}

	// ============================================================================
	// PRIVATE METHODS

	/**
	 * Prepares the plugin for running in Macro mode.
	 */
	private void activateMacro(final ImagePlus imp) {
		@SuppressWarnings("unchecked")
		final Vector<TextField> numbers = dialog_.getNumericFields();
		@SuppressWarnings("unchecked")
		final Vector<Checkbox> checkboxes = dialog_.getCheckboxes();
		@SuppressWarnings("unchecked")
		final Vector<TextField> stringfields = dialog_.getStringFields();

		final TextField xmlSource = stringfields.elementAt(0);
		final TextField numNodes = numbers.elementAt(0);
		final Checkbox saveState = checkboxes.elementAt(0);
		final Checkbox saveXMLState = checkboxes.elementAt(1);
		final TextField xmlDest = stringfields.elementAt(2);

		final String options = Macro.getOptions();

		xmlSource.setText(Macro.getValue(options, XMLSOURCE, xmlSource_));
		numNodes.setText(Macro.getValue(options, NUM_NODES, "" + M_));
		String s2 = new String(Macro.getValue(options, SAVE, "" + saveROI_));
		if (s2.equals("true")) {
			saveState.setState(true);
		} else {
			saveState.setState(false);
		}
		String s3 = new String(Macro.getValue(options, SAVEXML, "" + saveXML_));
		if (s3.equals("true")) {
			saveXMLState.setState(true);
		} else {
			saveXMLState.setState(false);
		}
		xmlDest.setText(Macro.getValue(options, XMLDEST, xmlDest_));
	}

	private void saveModelToXML(String filename, InteractiveHSplineModel model, ImagePlus imp) throws Exception {
		if (imp == null) {
			throw new Exception("Source image is null in saveSnakeToXML.");
		}
		if (model == null) {
			throw new Exception("Model is null in saveSnakeToXML.");
		}
		if (filename.isEmpty()) {
			throw new Exception("Filename is null in saveSnakeToXML.");
		}

		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			// Root
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement("root");
			doc.appendChild(root);

			// Name
			Element name = doc.createElement("name");
			name.appendChild(doc.createTextNode(imp.getTitle()));
			root.appendChild(name);

			// Model
			// In case there should be several of them
			Element rois = doc.createElement("rois");
			root.appendChild(rois);

			// Add model
			Element roi = doc.createElement("roi");
			rois.appendChild(roi);

			Element params = doc.createElement("snake_parameters");
			roi.appendChild(params);

			Element M = doc.createElement("M");
			M.appendChild(doc.createTextNode(String.valueOf(model.getNumNodes())));
			params.appendChild(M);

			Element ctrlpts = doc.createElement("control_points");
			params.appendChild(ctrlpts);
			DecimalFormat df = new DecimalFormat(".####");
			for (int k = 0; k < 2 * model.getNumNodes(); k++) {
				Element point = doc.createElement("control_point");
				ctrlpts.appendChild(point);

				Element x = doc.createElement("x");
				String number = df.format(model.getNodes()[k].x);
				x.appendChild(doc.createTextNode(number));
				point.appendChild(x);

				Element y = doc.createElement("y");
				number = df.format(model.getNodes()[k].y);
				y.appendChild(doc.createTextNode(number));
				point.appendChild(y);

				Element isfrozen = doc.createElement("frozen");
				String bool = "" + (model.getNodes()[k].frozen);
				isfrozen.appendChild(doc.createTextNode(bool));
				point.appendChild(isfrozen);

				Element ishidden = doc.createElement("hidden");
				bool = "" + (model.getNodes()[k].hidden);
				ishidden.appendChild(doc.createTextNode(bool));
				point.appendChild(ishidden);
			}

			// Write file
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(
					new File(FileSystems.getDefault().getPath(filename).normalize().toAbsolutePath().toString()));

			transformer.transform(source, result);
			System.out.println("File saved!");

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}

	private Snake2DNode[] loadModelFromXML(String filename) throws Exception {
		if (filename.isEmpty()) {
			throw new Exception("Filename is null in loadSnakeFromXML.");
		}

		Snake2DNode[] output = null;

		try {
			File xml = new File(FileSystems.getDefault().getPath(filename).normalize().toAbsolutePath().toString());

			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(xml);
			doc.getDocumentElement().normalize();

			Node roiNode = doc.getElementsByTagName("roi").item(0);
			Node snakeParams = ((Element) roiNode).getElementsByTagName("snake_parameters").item(0);

			String M = ((Element) snakeParams).getElementsByTagName("M").item(0).getChildNodes().item(0).getNodeValue();
			output = new Snake2DNode[2 * Integer.valueOf(M)];

			NodeList ctrlPtsList = ((Element) ((Element) snakeParams).getElementsByTagName("control_points").item(0))
					.getElementsByTagName("control_point");

			if (2 * Integer.valueOf(M) != ctrlPtsList.getLength()) {
				throw new Exception("Parameter M does not match number of control points in loadModelFromXML.");
			}

			for (int k = 0; k < ctrlPtsList.getLength(); ++k) {
				Node ctrlPoint = ctrlPtsList.item(k);

				if (ctrlPoint.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) ctrlPoint;
					String x = e.getElementsByTagName("x").item(0).getChildNodes().item(0).getNodeValue();
					String y = e.getElementsByTagName("y").item(0).getChildNodes().item(0).getNodeValue();

					String frozen = e.getElementsByTagName("frozen").item(0).getChildNodes().item(0).getNodeValue();
					String hidden = e.getElementsByTagName("hidden").item(0).getChildNodes().item(0).getNodeValue();

					output[k] = new Snake2DNode(Double.valueOf(x), Double.valueOf(y), Boolean.valueOf(frozen),
							Boolean.valueOf(hidden));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output;
	}
}
