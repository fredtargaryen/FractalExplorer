package gui.panels.fractals;

import gui.FractalDisplay;

import java.awt.*;

public class JuliaPanel extends FractalPanel
{
    private InteractiveFractalPanel panelToDeriveFrom;

    public JuliaPanel()
    {
        super();
        //Placeholder
        this.setRawInstructions("p;p*p");
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
     * Adjusts the instruction string of any valid sequence to generate Julia sets.
     * A "&c" at the end is changed to a "&u", to add the user selected point instead of the current point.
     * Before returning, the first term is set to the current point.
     */
    public String getProcessedInstructions()
    {
        String otherPanelInstructions = this.panelToDeriveFrom.getProcessedInstructions();
        String nextTermRule = otherPanelInstructions.split(";")[1];
        if(nextTermRule.substring(nextTermRule.length() - 2).equals("&c"))
        {
            nextTermRule = nextTermRule.substring(0, nextTermRule.length() - 2)+"&u";
            return "c;" + nextTermRule;
        }
        else
        {
            return "";
        }
    }
}