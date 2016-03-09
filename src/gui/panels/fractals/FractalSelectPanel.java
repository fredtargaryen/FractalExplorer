package gui.panels.fractals;

import exceptions.InvalidInstructionsException;
import gui.FractalDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class FractalSelectPanel extends JPanel
{
    private JButton chooseFractal;
    private JButton createFractal;
    private JButton deleteFractal;

    public FractalSelectPanel()
    {
        super();
        this.setLayout(new GridLayout(3, 1));
        this.chooseFractal = new JButton("Select Fractal...");
        this.chooseFractal.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                FractalDisplay mainWindow = FractalDisplay.getMainWindow();
                HashMap<String, InteractiveFractalPanel> fractalMap = mainWindow.getFractals();
                String chosenName = (String) JOptionPane.showInputDialog(FractalSelectPanel.this.chooseFractal,
                        "Please select a fractal to display.", "Select Fractal", JOptionPane.QUESTION_MESSAGE, null,
                        fractalMap.keySet().toArray(), "Click to select");
                if(chosenName != null && fractalMap.containsKey(chosenName))
                {
                    mainWindow.setLeftPanel(fractalMap.get(chosenName));
                }
            }
        });

        this.createFractal = new JButton("Create new fractal...");
        this.createFractal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String rawInstructions = JOptionPane.showInputDialog(FractalSelectPanel.this.createFractal,
                        "Please enter a formula for the fractal.\n" +
                                "Formula rules:\n" +
                                "*The formula must contain a rule for obtaining the initial term, and a rule for" +
                                " obtaining the next\n term, separated by a semicolon.\n" +
                                "*No spaces are allowed between characters.\n" +
                                "*Exponents are not valid (yet).\n" +
                                "*Simply negating values with - is not valid (yet).\n" +
                                "*To define complex numbers, use the format [x,y], where x is the complex number's real" +
                                " part\n and y is its imaginary part.\n" +
                                "*Defining a complex number while defining a different complex number is not permitted.\n" +
                                "*Up to 10 unique normal numbers and up to 10 unique complex numbers can be defined.\n" +
                                "*Special complex numbers:\n" +
                                " ~f refers to the first term in the iteration sequence (cannot use in the first term" +
                                " rule).\n" +
                                " ~p refers to the previous term in the iteration sequence (cannot use in the first term" +
                                " rule).\n" +
                                " ~u refers to the point on the fractal last pressed by the user.\n" +
                                " ~c refers to the point currently being drawn on the fractal.\n" +
                                "*Unary operators:\n" +
                                " ~a followed by a decimal number gets that decimal's absolute value.\n" +
                                " ~r followed by a complex number gets its real part.\n" +
                                " ~i followed by a complex number gets its imaginary part.\n" +
                                "*Unary operators can be combined.\n" +
                                "*Binary operators: use * to multiply, + to add and - to subtract two values.\n" +
                                " The two values operated on must be either both complex numbers or both decimal" +
                                " numbers.\n" +
                                "*Ordering: Brackets are not valid (yet) so values are evaluated from left to right, i.e.\n" +
                                " 2+3*2 = 10", "Create New Fractal", JOptionPane.PLAIN_MESSAGE);
                if(rawInstructions != null)
                {
                    InteractiveFractalPanel ifp = new InteractiveFractalPanel();
                    ifp.setRawInstructions(rawInstructions);
                    try
                    {
                        ifp.validateInstructions();
                        String newFractalName = JOptionPane.showInputDialog(FractalSelectPanel.this.createFractal,
                                "Please give a name to this fractal.", "Name Fractal", JOptionPane.PLAIN_MESSAGE);
                        HashMap<String, InteractiveFractalPanel> fractalMap = FractalDisplay.getMainWindow().getFractals();
                        int addNewFractal;
                        if(fractalMap.containsKey(newFractalName))
                        {
                            addNewFractal = JOptionPane.showConfirmDialog(FractalSelectPanel.this.createFractal,
                                    "A fractal with this name already exists. OK to overwrite?", "Warning",
                                    JOptionPane.YES_NO_OPTION);
                        }
                        else
                        {
                            addNewFractal = JOptionPane.YES_OPTION;
                        }
                        if(addNewFractal == JOptionPane.YES_OPTION)
                        {
                            fractalMap.put(newFractalName, ifp);
                        }
                    }
                    catch(InvalidInstructionsException iie)
                    {
                        JOptionPane.showMessageDialog(FractalSelectPanel.this.createFractal,
                                iie.getErrorStringForUsers());
                    }
                }
            }
        });

        this.deleteFractal = new JButton("Delete a fractal...");
        this.deleteFractal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HashMap<String, InteractiveFractalPanel> fractalMap = FractalDisplay.getMainWindow().getFractals();
                String chosenName = (String) JOptionPane.showInputDialog(FractalSelectPanel.this.chooseFractal,
                        "Please select a fractal to delete.", "Delete Fractal", JOptionPane.QUESTION_MESSAGE, null,
                        fractalMap.keySet().toArray(), "Click to select");
                if(chosenName != null && fractalMap.containsKey(chosenName))
                {
                    fractalMap.remove(chosenName);
                }
            }
        });

        this.add(this.deleteFractal, 0, 0);
        this.add(this.chooseFractal, 1, 0);
        this.add(this.createFractal, 2, 0);
    }
}
