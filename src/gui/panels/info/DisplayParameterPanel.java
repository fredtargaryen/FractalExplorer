package gui.panels.info;

import gui.FractalDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Contains minimum and maximum real and imaginary values to display, as well as number of iterations to perform
 * when drawing. Components which are painting obtain information from here with
 * FractalDisplay.getMainWindow().getParamPanel().get...
 */
public class DisplayParameterPanel extends JPanel
{
    private JTextField leftRealBox;
    private JTextField rightRealBox;
    private JTextField downImagBox;
    private JTextField upImagBox;
    private JTextField iterBox;
    private JButton okButton;
    private JButton resetButton;
    private JCheckBox invertR;
    private JCheckBox invertI;

    public DisplayParameterPanel()
    {
        super();
        //Construct all member variable components, with default values
        this.leftRealBox = new JTextField("-2.5");
        this.rightRealBox = new JTextField("1.5");
        this.downImagBox = new JTextField("-1.6");
        this.upImagBox = new JTextField("1.6");
        this.iterBox = new JTextField("100");
        this.okButton = new JButton("OK");
        this.okButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean doErrorBox = false;
                double leftReal = 0;
                double rightReal = 0;
                double downImag = 0;
                double upImag = 0;
                int iterations = 0;
                //Performs validation on the text box information.
                //leftReal, rightReal, downImag and upImag must all be valid doubles.
                //leftReal must not be equal to rightReal, and downImag must not be equal to upImag.
                //iterations must be an integer greater than 0.
                try
                {
                    leftReal = Double.parseDouble(DisplayParameterPanel.this.leftRealBox.getText());
                    rightReal = Double.parseDouble(DisplayParameterPanel.this.rightRealBox.getText());
                    downImag = Double.parseDouble(DisplayParameterPanel.this.downImagBox.getText());
                    upImag = Double.parseDouble(DisplayParameterPanel.this.upImagBox.getText());
                    iterations = Integer.parseInt(DisplayParameterPanel.this.iterBox.getText());
                    if(iterations <= 0 || leftReal == rightReal || downImag == upImag)
                    {
                        doErrorBox = true;
                    }
                }
                catch(NumberFormatException nfe)
                {
                    doErrorBox = true;
                }
                if(doErrorBox)
                {
                    DisplayParameterPanel.this.doErrorBox();
                }
                else
                {
                    DisplayParameterPanel.this.setMinMax(leftReal, rightReal, downImag, upImag);
                    DisplayParameterPanel.this.setIterations(iterations);
                }
            }
        });
        this.resetButton = new JButton("Reset");
        this.resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //Changes the min and max values back to the default, as there currently is no zoom-out feature
                DisplayParameterPanel.this.setMinMax(-2.5, 1.5, -1.6, 1.6);
                DisplayParameterPanel.this.okButton.doClick();
            }
        });
        this.invertR = new JCheckBox("Invert");
        this.invertR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                DisplayParameterPanel.this.swapR();
                DisplayParameterPanel.this.invertR.setSelected(DisplayParameterPanel.this.isRInverted());
            }
        });
        this.invertI = new JCheckBox("Invert");
        this.invertI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                DisplayParameterPanel.this.swapI();
                DisplayParameterPanel.this.invertI.setSelected(DisplayParameterPanel.this.isIInverted());
            }
        });

        //Set up layout, add member components and construct and add local components
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx=1;
        gbc.weighty=1;
        gbc.gridwidth = 3;
        JLabel realLabel = new JLabel("Real axis");
        realLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(realLabel, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        this.add(this.invertR, gbc);
        gbc.gridx = 4;
        gbc.gridwidth = 3;
        JLabel imagLabel = new JLabel("Imaginary axis");
        imagLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(imagLabel, gbc);
        gbc.gridx = 7;
        gbc.gridwidth = 1;
        this.add(this.invertI, gbc);
        gbc.gridx = 8;
        JLabel iterLabel = new JLabel("Iterations");
        iterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(iterLabel, gbc);
        gbc.gridy=1;
        gbc.gridx=0;
        this.add(new JLabel("Left:"), gbc);
        ++gbc.gridx;
        this.add(this.leftRealBox, gbc);
        ++gbc.gridx;
        this.add(new JLabel("Right:"), gbc);
        ++gbc.gridx;
        this.add(this.rightRealBox, gbc);
        ++gbc.gridx;
        this.add(new JLabel("Lower:"), gbc);
        ++gbc.gridx;
        this.add(this.downImagBox, gbc);
        ++gbc.gridx;
        this.add(new JLabel("Upper:"), gbc);
        ++gbc.gridx;
        this.add(this.upImagBox, gbc);
        ++gbc.gridx;
        this.add(this.iterBox, gbc);
        ++gbc.gridx;
        this.add(this.okButton, gbc);
        ++gbc.gridx;
        this.add(this.resetButton, gbc);
    }

    public void setMinMax(double mir, double mar, double mii, double mai)
    {
        this.leftRealBox.setText(String.valueOf(mir));
        this.rightRealBox.setText(String.valueOf(mar));
        this.downImagBox.setText(String.valueOf(mii));
        this.upImagBox.setText(String.valueOf(mai));
        FractalDisplay.getMainWindow().repaint();
    }

    /**
     * You often only want to do one of setting display range or changing iterations,
     * so setting iterations has been separated.
     */
    public void setIterations(int i)
    {
        if(i >= 1)
        {
            this.iterBox.setText(String.valueOf(i));
            FractalDisplay.getMainWindow().repaint();
        }
        else
        {
            this.doErrorBox();
        }
    }

    /**
     * Obtaining iterations does require some extra function calls, but it is more
     * cohesive and reduces resource usage to have these variables stored
     * in only one place. Furthermore, extra function calls would be needed to keep these
     * variables synchronized anyway.
     */
    public int getIterations()
    {
        return Integer.parseInt(this.iterBox.getText());
    }

    /**
     * Obtaining the display range does require some extra function calls, but it is more
     * cohesive and reduces resource usage to have these variables stored
     * in only one place. Furthermore, extra function calls would be needed to keep these
     * variables synchronized anyway.
     * Returned as an array of leftReal, rightReal, downImag, upImag. These are all returned
     * at once because they are almost always needed all together.
     */
    public double[] getDisplayRange()
    {
        return new double[] {
                Double.parseDouble(this.leftRealBox.getText()),
                Double.parseDouble(this.rightRealBox.getText()),
                Double.parseDouble(this.downImagBox.getText()),
                Double.parseDouble(this.upImagBox.getText())
        };
    }

    private void doErrorBox()
    {
        JOptionPane.showMessageDialog(DisplayParameterPanel.this.okButton,
                "Cannot use these details to display a set.\n" +
                        "Please check the following:\n" +
                        "You have used decimal numbers in the min and max boxes;\n" +
                        "The numbers in the max boxes are not equal to those in\n" +
                        "their corresponding min boxes;\n" +
                        "You have entered an integer larger than 0 for the\n" +
                        "number of iterations.");
    }

    public void swapR()
    {
        String temp = this.leftRealBox.getText();
        this.leftRealBox.setText(this.rightRealBox.getText());
        this.rightRealBox.setText(temp);
        FractalDisplay.getMainWindow().repaint();
    }

    public void swapI()
    {
        String temp = this.downImagBox.getText();
        this.downImagBox.setText(this.upImagBox.getText());
        this.upImagBox.setText(temp);
        FractalDisplay.getMainWindow().repaint();
    }

    public boolean isIInverted()
    {
        return Double.parseDouble(this.downImagBox.getText()) > Double.parseDouble(this.upImagBox.getText());
    }

    public boolean isRInverted()
    {
        return Double.parseDouble(this.leftRealBox.getText()) > Double.parseDouble(this.rightRealBox.getText());
    }
}