package com.virginieuhlmann.demo;

import com.virginieuhlmann.InteractiveHSplineModel_;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;

/**
 * InteractiveHSplineModelDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class InteractiveHSplineModelDemo {
    public static void main(String... args) {

        // Start ImageJ
        new ImageJ();

        // Create a test image
        ImagePlus imp = NewImage.createByteImage("test", 512, 512, 3, NewImage.FILL_RAMP);
        imp.show();

        // run the plugin
        InteractiveHSplineModel_ modelPlugin = new InteractiveHSplineModel_();
        modelPlugin.setup("", imp);
        modelPlugin.showDialog(imp, "", null);
        modelPlugin.run(imp.getProcessor());

    }
}
