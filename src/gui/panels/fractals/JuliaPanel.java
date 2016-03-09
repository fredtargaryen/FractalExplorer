package gui.panels.fractals;

import gui.FractalDisplay;
import gui.panels.info.DisplayParameterPanel;
import numbers.Complex;

import java.awt.*;

public class JuliaPanel extends FractalPanel
{
    private InteractiveFractalPanel panelToDeriveFrom;
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
        DisplayParameterPanel paramPanel = FractalDisplay.getMainWindow().getParamPanel();

        double realPart = ((double)panelx / actualWidth) * 4.0;
        double imagPart = ((double)panely / actualHeight) * 3.2;
        //Makes the complex relative to the display range
        if(paramPanel.isRInverted())
        {
            realPart = 1.5 - realPart;
        }
        else
        {
            realPart = -2.5 + realPart;
        }
        if(paramPanel.isIInverted())
        {
            imagPart = -1.6 + imagPart;
        }
        else
        {
            imagPart = 1.6 - imagPart;
        }
        return new Complex(realPart, imagPart);
    }

    public void linkPanel(InteractiveFractalPanel panel)
    {
        this.panelToDeriveFrom = panel;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        if (FractalDisplay.getMainWindow().getTopDisplay().getLastPoint() != null) {
            super.paintComponent(g);
        }
    }

    /**
     * Copies the left panel that it is linked to, but sets the first term to the current point.
     * This makes sure that a proper Julia set can be generated from the first term.
     * Checks first that the current point is added each iteration in the normal set, which allows a Julia set to be
     * generated.
     */
    public String getProcessedFirstTerm()
    {
        String otherPanelNextTerm = this.panelToDeriveFrom.getProcessedNextTerm();
        if(otherPanelNextTerm.substring(otherPanelNextTerm.length() - 2).equals("+c"))
        {
            return "c";
        }
        else
        {
            return "";
        }
    }

    /**
     * Copies the left panel that it is linked to, but sets the added point to the current point.
     * This makes sure that a proper Julia set can be generated from the first term.
     * A "+c" at the end is changed to a "+u", to add the user selected point instead of the current point.
     * Checks first that the current point is added each iteration in the normal set, which allows a Julia set to be
     * generated.
     */
    public String getProcessedNextTerm()
    {
        String otherPanelNextTerm = this.panelToDeriveFrom.getProcessedNextTerm();
        int nextTermLength = otherPanelNextTerm.length();
        if(otherPanelNextTerm.substring(nextTermLength - 2).equals("+c"))
        {
            return otherPanelNextTerm.substring(0, nextTermLength - 2) + "+u";
        }
        else
        {
            return "";
        }
    }
}