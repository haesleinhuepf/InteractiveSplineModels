package com.virginieuhlmann;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/** 
 * Wrapper of the credits button.
 * 
 * @version October 17, 2018
 * 
 * @author Virginie Uhlmann (me@virginieuhlmann.com)
 */
public class IHSMCreditsButton extends Panel implements ActionListener {

	/** Random serial. */
	private static final long serialVersionUID = -3295940863867673109L;

	// ============================================================================
	// PUBLIC METHODS

	/**
	 * Constructor.
	 */
	protected IHSMCreditsButton() {
	
		super();
		final JButton creditsButton = new JButton("Credits");
		creditsButton.addActionListener(this);
		add(creditsButton);
	}
	
	// ----------------------------------------------------------------------------
	
	/**
	 * Invoked when an action occurs.
	 */
	public void actionPerformed (final ActionEvent e) {
		new IHSMCredits(null);
	}
}