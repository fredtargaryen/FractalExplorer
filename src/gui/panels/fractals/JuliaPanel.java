package gui.panels.fractals;

import exceptions.InvalidInstructionsException;
import gui.FractalDisplay;
import gui.panels.info.DisplayParameterPanel;
import numbers.Complex;

import java.awt.*;

public class JuliaPanel extends FractalPanel
{
    private DisplayParameterPanel paramPanel;

    public JuliaPanel()
    {
        super();
        //Placeholder
        this.setRawInstructions("p;p*p");
        this.paramPanel = FractalDisplay.getMainWindow().getParamPanel();
    }

    /**
     * Translates an x-y point on the panel to its corresponding complex number.
     */
    @Override
    public Complex getPanelCoordsAsComplex(int panelx, int panely)
    {
        int actualWidth = this.getWidth();
        int actualHeight = this.getHeight();
        //Display parameters should be constant for the Julia Panel.
        //Min real: -2.5; Max real: 1.5; Min imag.: -1.6; Max imag.: 1.6

        double realPart = ((double)panelx / actualWidth) * 4.0;
        double imagPart = ((double)panely / actualHeight) * 3.2;
        //Makes the complex relative to the display range
        if(this.paramPanel.isRInverted())
        {
            realPart = 1.5 - realPart;
        }
        else
        {
            realPart = -2.5 + realPart;
        }
        if(this.paramPanel.isIInverted())
        {
            imagPart = -1.6 + imagPart;
        }
        else
        {
            imagPart = 1.6 - imagPart;
        }
        return new Complex(realPart, imagPart);
    }

    /**
     * Copies the left panel that it is linked to, but sets the added point to the user selected point and the first
     * point to the current point.
     * This makes sure that a proper Julia set can be generated from the first term.
     * It first checks that the current point is added each iteration in the normal set, which allows a Julia set to be
     * generated.
     */
    public void linkPanel(InteractiveFractalPanel panel)
    {
        this.rawInstructions = panel.rawInstructions;
        int rawLength = this.rawInstructions.length();
        if(this.rawInstructions.substring(rawLength - 2).equals("+c"))
        {
            this.rawInstructions = "c;" + this.rawInstructions.split(";")[1].substring(0, rawLength - 4) + "+u";
            try {
                this.validateInstructions();
            }
            catch(InvalidInstructionsException iie)
            {
                this.rawInstructions = "";
                this.processedFirstTerm = "";
                this.processedNextTerm = "";
            }
        }
        else
        {
            this.rawInstructions = "";
            this.processedFirstTerm = "";
            this.processedNextTerm = "";
        }
    }
}