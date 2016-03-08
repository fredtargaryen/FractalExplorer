package gui.panels.info;

import gui.FractalDisplay;
import numbers.Complex;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PointSelectionPanel extends JPanel
{
    private JLabel lastSelectedPointField;
    private Complex lastPoint;
    private JButton faveButton;
    private JButton openFaves;

    public PointSelectionPanel()
    {
        super();
        this.add(new JLabel("Last selected point:"));
        this.lastSelectedPointField = new JLabel("");
        this.add(this.lastSelectedPointField);
        this.add(new JLabel("Julia set"));
        this.faveButton = new JButton("Add to favourites");
        this.faveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(PointSelectionPanel.this.lastPoint != null)
                {
                    String favouriteName = JOptionPane.showInputDialog(PointSelectionPanel.this.faveButton,
                            "Please enter a nickname for this point.", "Name Point", JOptionPane.PLAIN_MESSAGE);
                    if (FractalDisplay.getMainWindow().getFavourites().keySet().contains(favouriteName)) {
                        if (JOptionPane.showConfirmDialog(PointSelectionPanel.this.faveButton,
                                "This will overwrite a previous point with the same name. Is this OK?",
                                "Warning", JOptionPane.OK_CANCEL_OPTION)
                                == JOptionPane.OK_OPTION) {
                            FractalDisplay.getMainWindow().getFavourites().put(favouriteName, PointSelectionPanel.this.lastPoint);
                        }
                    } else {
                        FractalDisplay.getMainWindow().getFavourites().put(favouriteName, PointSelectionPanel.this.lastPoint);
                    }
                }
            }
        });

        this.openFaves = new JButton("Open favourite");
        this.openFaves.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] possibleValues = FractalDisplay.getMainWindow().getFavourites().keySet().toArray();
                if(possibleValues.length > 0)
                {
                    String newPointName = (String) JOptionPane.showInputDialog(PointSelectionPanel.this.openFaves, "Please select a point to display.",
                            "Open Favourite", JOptionPane.QUESTION_MESSAGE, null, possibleValues, possibleValues[0]);
                    if(newPointName != null && !newPointName.equals(""))
                    {
                        PointSelectionPanel.this.setPoint(FractalDisplay.getMainWindow().getFavourites().get(newPointName));
                    }
                }
            }
        });

        this.add(this.faveButton);
        this.add(this.openFaves);
    }

    public void setPoint(Complex c)
    {
        this.lastPoint = c;
        this.lastSelectedPointField.setText(this.lastPoint.toString());
        FractalDisplay.getMainWindow().getJuliaImage().repaint();
    }

    public Complex getLastPoint()
    {
        return this.lastPoint;
    }
}
