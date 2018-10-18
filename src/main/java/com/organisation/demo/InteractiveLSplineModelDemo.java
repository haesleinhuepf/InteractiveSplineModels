package com.organisation.demo;

import com.organisation.InteractiveESplineModel_;
import com.organisation.InteractiveLSplineModel_;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;

/**
 * InteractiveESplineModelDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 10 2018
 */
public class InteractiveLSplineModelDemo {
    public static void main(String... args) {

        // Start ImageJ
        new ImageJ();

        // Create a test image
        ImagePlus imp = NewImage.createByteImage("test", 512, 512, 3, NewImage.FILL_RAMP);
        imp.show();

        // run the plugin
        InteractiveLSplineModel_ modelPlugin = new InteractiveLSplineModel_();
        modelPlugin.setup("", imp);
        modelPlugin.showDialog(imp, "", null);
        modelPlugin.run(imp.getProcessor());

    }
}
