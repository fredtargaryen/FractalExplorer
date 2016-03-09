package gui;

import exceptions.InvalidInstructionsException;
import gui.panels.fractals.*;
import gui.panels.info.DisplayParameterPanel;
import gui.panels.info.PointSelectionPanel;
import numbers.Complex;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * The initial window
 */
public class FractalDisplay extends JFrame
{
    /**
     * Singleton of the main window, so that components can be put in different places and still get a reference to it
     */
    private static FractalDisplay programInstance;

    /**
     * Left image in the window; displays the Mandelbrot set by default
     */
    private InteractiveFractalPanel leftImage;

    /**
     * Right image in the window; displays the Julia set for any selected point in mandelImage
     */
    private JuliaPanel juliaImage;

    /**
     * Supplies parameters for drawing: see gui.panels.info.DisplayParameterPanel.
     */
    private DisplayParameterPanel bottomDisplay;

    /**
     * Shows respective names of sets displayed, the last selected point in mandelImage, and options for adding and
     * viewing favourite Julia sets
     */
    private PointSelectionPanel topDisplay;

    /**
     * Maps the name of each favourite point to its complex number in the Mandelbrot set
     */
    private HashMap<String, Complex> favourites;

    /**
     * Maps the name of each fractal to its display panel. Before adding a panel to this HashMap, it is best to call
     * setRawInstructions and validateInstructions, so that the panel can be constructed, initialized and validated
     * just once.
     */
    private HashMap<String, InteractiveFractalPanel> fractals;

    private FractalSelectPanel fractSelect;

    public FractalDisplay()
    {
        super("Fractal Explorer");
        programInstance = this;
        this.favourites = new HashMap<String, Complex>();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.fractals = new HashMap<String, InteractiveFractalPanel>();

        //No real exception handling because these strings are known to be valid
        InteractiveFractalPanel mandel = new InteractiveFractalPanel();
        mandel.setRawInstructions("c;p*p+c");
        try{mandel.validateInstructions();}catch(InvalidInstructionsException iie){}
        this.fractals.put("Mandelbrot", mandel);

        InteractiveFractalPanel ship = new InteractiveFractalPanel();
        ship.setRawInstructions("c;[arp,aip]*[arp,aip]+c");
        try{ship.validateInstructions();}catch(InvalidInstructionsException iie){}
        this.fractals.put("Burning Ship", ship);

        InteractiveFractalPanel buff = new InteractiveFractalPanel();
        buff.setRawInstructions("c;[arp,aip]*[arp,aip]-[arp,aip]+c");
        try{buff.validateInstructions();}catch(InvalidInstructionsException iie){}
        this.fractals.put("Buffalo", buff);

        //Construct member components
        this.bottomDisplay = new DisplayParameterPanel();
        this.juliaImage = new JuliaPanel();
        this.setLeftPanel(ship);
        this.topDisplay = new PointSelectionPanel();
        this.fractSelect = new FractalSelectPanel();

        //Set up layout
        this.setLayout(new BorderLayout());
        Container contentPane = this.getContentPane();
        contentPane.add(this.leftImage, BorderLayout.CENTER);
        contentPane.add(this.bottomDisplay, BorderLayout.SOUTH);
        contentPane.add(this.topDisplay, BorderLayout.NORTH);
        contentPane.add(this.juliaImage, BorderLayout.EAST);
        contentPane.add(this.fractSelect, BorderLayout.WEST);

        this.pack();
        this.setVisible(true);
    }

    public void setLeftPanel(InteractiveFractalPanel panel) {
        Container contentPane = this.getContentPane();
        if(this.leftImage != null)
        {
            contentPane.remove(this.leftImage);
        }
        this.leftImage = panel;
        contentPane.add(this.leftImage, BorderLayout.CENTER);
        this.juliaImage.linkPanel(this.leftImage);
        contentPane.revalidate();
        contentPane.repaint();
    }

    //BASIC GETTERS AND SETTERS BELOW HERE

    public static FractalDisplay getMainWindow()
    {
        return programInstance;
    }

    public PointSelectionPanel getTopDisplay() { return this.topDisplay; }

    public DisplayParameterPanel getParamPanel() { return this.bottomDisplay; }

    public HashMap<String, Complex> getFavourites()
    {
        return this.favourites;
    }

    public HashMap<String, InteractiveFractalPanel> getFractals() { return this.fractals; }

    public JuliaPanel getJuliaImage() { return this.juliaImage; }
}
