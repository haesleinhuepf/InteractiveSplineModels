import ij.IJ;
import ij.gui.GUI;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** 
 * Credits dialog.
 * 
 * @version October 17, 2018
 * 
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 */
public class IHSMCredits extends JDialog implements ActionListener {

	/** Random serial. */
	private static final long serialVersionUID = -3550300560380422419L;
	/** Close button. */
	private JButton bnClose_ = new JButton("Close");
	/** Layout manager. */
	private GridBagLayout layout_ = new GridBagLayout();
	/** Constraints for the interface components. */

	// ============================================================================
	// PUBLIC METHODS

	/**
	 * Constructor.
	 */
	public IHSMCredits(JFrame parentWindow) {
		
		super(parentWindow,"Credits");
		
		setModal(true);
		JTextArea credits = new JTextArea(20, 30);
		credits.append("Virginie Uhlmann\n");
		credits.append("\n");
		credits.append("EMBL-EBI\n");
		credits.append("Wellcome Genome Campus\n");
		credits.append("Cambridge CB10 1SD\n");
		credits.append("United Kingdom\n");
		credits.append("\n");
		credits.append("E-mail: me@virginieuhlmann.com\n");
		credits.append("\n");
		credits.append("Copyright Free:\n");
		credits.append("\n");
		credits.append("You'll be free to use this software for research purposes,\n");
		credits.append("but you should not redistribute it without our consent.\n");
		credits.append("In addition, you undertake to include a citation or\n");
		credits.append("acknowledgment whenever you present or publish results\n");
		credits.append("that are based on it. EMBL-EBI makes no warranties of any\n");
		credits.append("kind on this software and shall in no event be liable for\n"); 
		credits.append("damages of any kind in connection with the use and exploitation\n");
		credits.append("of this technology.");
		credits.setForeground(new Color(0, 32, 128));
		credits.setBackground(this.getBackground());
		credits.setEditable(false);

		JPanel panelCredits = new JPanel();
		panelCredits.setLayout(layout_);
		addComponent(panelCredits, 0, 0, 8, 1, 9, credits);
		panelCredits.setBorder(BorderFactory.createEtchedBorder());
	
		// Add Listeners
		bnClose_.addActionListener(this);
		
		JPanel panel = new JPanel();
		panel.setLayout(layout_);
		addComponent(panel, 0, 0, 8, 1, 9, panelCredits);
		addComponent(panel, 1, 0, 1, 1, 9, new JLabel("  "));
		addComponent(panel, 1, 4, 1, 1, 9, bnClose_);
		addComponent(panel, 1, 5, 1, 1, 9, new JLabel("  "));
	
		// Building the main panel
		setLayout(layout_);
		JPanel pnMain = new JPanel();
		pnMain.setLayout(layout_);
		addComponent(pnMain, 0, 0, 1, 1, 9, panel);
		add(pnMain);
		pack();
		setResizable(false);
		GUI.center(this);
		setVisible(true);
	}

	// ----------------------------------------------------------------------------

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized  void actionPerformed(ActionEvent e) {
	
		if (e.getSource() == bnClose_) {
			dispose();
		}
		notify();
	}

	// ============================================================================
	// PRIVATE METHODS
	
	/**
	 * Add a component in a panel in the northeast of the cell.
	 */
	private void addComponent(JPanel pn, int row, final int col, int width, final int height, int space, JComponent comp) {
		
		GridBagConstraints constraint = new GridBagConstraints();
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout_.setConstraints(comp, constraint);
		pn.add(comp);
	}
}