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

package com.organisation.snake2D;

import java.util.Observable;

/*====================================================================
|	Snake2DOptimizer
\===================================================================*/

/*------------------------------------------------------------------*/
/*********************************************************************
 This class encapsulates the optimization aspects of snakes. It
 handles objects that implement the <code>Snake2D</code> interface.
 @see Snake2D
 ********************************************************************/
public abstract class Snake2DOptimizer
	extends Observable

{ /* begin class Snake2DOptimizer */

/*....................................................................
	public variables
....................................................................*/
/*********************************************************************
 The state of the snake being optimized. Set to <code>false</code>
 before issuing the statement <code>notifyObservers(snake);</code>
 that should precede a measurement of the energy of the snake. Set to
 <code>true</code> before issuing the statement
 <code>notifyObservers(snake);</code> to notify the
 <code>Snake2D.Snake2DKeeper</code> keeper object that the snake did
 indeed improve.
 ********************************************************************/
public boolean isCurrentBest = false;

/*....................................................................
	abstract methods
....................................................................*/
/*------------------------------------------------------------------*/
/*********************************************************************
 This method should perform the optimization of the
 <code>Snake2D.Snake2DNode[]</code> object <code>configuration</code>
 which drives the <code>Snake2D.Snake2D</code> object
 <code>snake</code>. The initial configuration should be retrieved
 from <code>configuration</code>. When the optimization is iterative,
 the iteration loop should contain the following elements:
 <ul>
 <li><code>snake.setNodes(configuration);</code></li>
 <li><code>this.isCurrentBest = false;</code></li>
 <li><code>setChanged();</code></li>
 <li><code>notifyObservers(snake);</code></li>
 <li>Check for <code>snake.isAlive()</code></li>
 <li>Terminate if the snake died</li>
 <li>Else, compute <code>snake.energy();</code></li>
 <li>Check for improvement in the energy of the snake</li>
 <li>Iterate if the snake failed to improve</li>
 <li>Else, <code>this.isCurrentBest = true;</code></li>
 <li><code>setChanged();</code></li>
 <li><code>notifyObservers(snake);</code></li>
 <li>Update <code>configuration</code></li>
 <li>Iterate</li>
 </ul>
 IMPORTANT: Every time it is desired to access either one of the
 <code>energy()</code> or <code>getEnergyGradient()</code> methods of
 the snake, a call to its <code>isAlive()</code> method ought to be
 made to ensure that it is admissible to carry on with the
 optimization. In case the snake died, the optimization should be made
 to terminate immediately.<br>
 @param snake The snake to optimize.
 @see Snake2D
 @see Snake2DKeeper
 ********************************************************************/
abstract public void optimize (
	final Snake2D snake,
	final Snake2DNode[] configuration
);

/*------------------------------------------------------------------*/
/*********************************************************************
 After the optimization completes, this method will be called by the
 keeper to debrief the optimizer.
 @return Return <code>null</code> if the energy could not be computed
 even once. Else, return the best energy that could be observed during
 optimization.
 ********************************************************************/
abstract public Double reportSnakeBestObservedEnergy (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 After the optimization completes, this method will be called by the
 keeper to debrief the optimizer.
 @return Return <code>false</code> if the snake was still alive when
 the optimizer did terminate, or possibly if an eventual death of the
 snake was unrelated to the termination decision. Return
 <code>true</code> if the optimizer did indeed terminate because the
 snake died.
 ********************************************************************/
abstract public boolean reportSnakeDeath (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 After the optimization completes, this method will be called by the
 keeper to debrief the optimizer.
 @return Return <code>false</code> if the optimizer abandoned its task
 before the optimization was complete, whatever the reason may have
 been. Return <code>true</code> if the optimizer could bring its task
 to completion and found a snake that was deemed to be optimal.
 ********************************************************************/
abstract public boolean reportSnakeOptimality (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 A call to this method should result in the optimization being
 currently performed by this object to be interrupted. The method
 <code>optimize</code> should then be made to return as early as
 possible. It is not necessary to restore the optimized snake in any
 particular configuration, because the best configuration will have
 been recorded by the keeper for each call to the method
 <code>notifyObservers(snake)</code>.
 ********************************************************************/
abstract public void stopOptimizing (
);

/*------------------------------------------------------------------*/
/*********************************************************************
 Notifies the keeper that the optimizer was successful in its previous
 attempt to improve the configuration of the snake <code>snake</code>.
 It is assumed that <code>snake.getNodes();</code> is going to return
 unchanged configuration values.
 @param snake The snake that was just called.
 ********************************************************************/
public void wasSuccessfulProbing (
	final Snake2D snake
) {
	isCurrentBest = true;
	setChanged();
	notifyObservers(snake);
} /* end wasSuccessfulProbing */

/*------------------------------------------------------------------*/
/*********************************************************************
 Notifies the keeper that the optimizer is about to measure either the
 energy of the snake <code>snake</code> or its gradient.
 @param snake The snake that will be called.
 ********************************************************************/
public void willProbe (
	final Snake2D snake
) {
	isCurrentBest = false;
	setChanged();
	notifyObservers(snake);
} /* end willProbe */

} /* end class snake2DOptimizer */
