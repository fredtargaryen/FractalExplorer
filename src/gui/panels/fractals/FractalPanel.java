package gui.panels.fractals;

import exceptions.InvalidInstructionsException;
import gui.FractalDisplay;
import gui.panels.info.DisplayParameterPanel;
import numbers.Complex;
import org.omg.CORBA.DynAnyPackage.Invalid;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Stack;

public class FractalPanel extends JPanel
{
    /**
     * The number of iterations to perform when drawing a point.
     */
    protected int iterations;

    /**
     * Stack of complex numbers for parsing expressions.
     */
    private Stack<Complex> complexStack;

    /**
     * Stack of decimal numbers for parsing expressions.
     */
    private Stack<Double> doubleStack;

    /**
     * The instruction string as entered by the user.
     */
    protected String rawInstructions;

    /**
     * The definition of the iteration sequence's first term, validated and edited by the program for faster processing.
     */
    protected String[] processedFirstTerm;

    /**
     * The definition of the iteration sequence's next term, validated and edited by the program for faster processing.
     */
    protected String[] processedNextTerm;

    /**
     * Stores up to 10 unique strings representing user-defined numerical values.
     * In processed instruction strings, "Dx" refers to number x in this array.
     */
    protected Double[] userDoubles;

    /**
     * Stores up to 10 unique strings representing user-defined complexes.
     * In processed instruction strings, "Cx" refers to complex number x in this array.
     * If there is a value at position x, the complex is one that has to be recalculated for every iteration, so
     * parseComplex(userComplexStrings[x], ...) must be called.
     * If there is no value, the complex value is constant, so it can be acquired directly from userComplexes[x].
     */
    protected String[][] userComplexStrings;

    /**
     * Stores up to 10 unique user-defined complex numbers, that do not need to be recalculated.
     * At the start of each iteration there is a pattern between userComplexStrings and userComplexes:
     * Where userComplexStrings is null, userComplexes is not null, and vice versa. A null value in userComplexStrings
     * and a value in userComplexes shows that the complex value is persistent throughout all iterations. The opposite
     * shows that the complex value has to be recalculated from the string every iteration. The first time the complex
     * value is calculated, it can be stored in userComplexes and reused. At the end of the iteration these values
     * will have to be removed; the values that need removing can be identified because their corresponding strings are
     * not null.
     */
    protected Complex[] userComplexes;

    //Binary operators and the comma need to protect the operand before them, or it could be operated on by subsequent
    //operators.
    protected int noOfProtectedDoubles;
    protected int noOfProtectedComplexes;

    /**
     * 10 because the numbers 0-9 can be easily used to reference a user value with just one character.
     */
    private static final int NO_OF_ALLOWED_USER_VALUES = 10;

    /**
     * The possible orbit traps.
     */
    public enum Orbit
    {
        NONE,
        CIRCLE,
        CROSS_ENGLISH,
        CROSS_SCOTTISH
    }

    private Orbit orbit;

    public FractalPanel()
    {
        super();
        this.setPreferredSize(new Dimension(600, 600));
        this.complexStack = new Stack<Complex>();
        this.doubleStack = new Stack<Double>();
        this.rawInstructions = "";
        this.processedFirstTerm = new String[0];
        this.processedNextTerm = new String[0];
        this.userDoubles = new Double[NO_OF_ALLOWED_USER_VALUES];
        this.userComplexStrings = new String[0][NO_OF_ALLOWED_USER_VALUES];
        this.userComplexes = new Complex[NO_OF_ALLOWED_USER_VALUES];
        this.orbit = Orbit.NONE;
        this.noOfProtectedDoubles = 0;
        this.noOfProtectedComplexes = 0;
    }

    /**
     * Translates an x-y point on the panel to its corresponding complex number.
     */
    public Complex getPanelCoordsAsComplex(int panelx, int panely)
    {
        int actualWidth = this.getWidth();
        int actualHeight = this.getHeight();
        DisplayParameterPanel paramPanel = FractalDisplay.getMainWindow().getParamPanel();
        //Returns { leftReal, rightReal, downImag, upImag }
        double[] displayRange = paramPanel.getDisplayRange();
        double fractalWidth = Math.abs(displayRange[1] - displayRange[0]);
        double fractalHeight = Math.abs(displayRange[3] - displayRange[2]);

        double realPart = ((double)panelx / actualWidth) * fractalWidth;
        double imagPart = ((double)panely / actualHeight) * fractalHeight;
        //Makes the complex relative to the display range
        if(paramPanel.isRInverted())
        {
            realPart = displayRange[0] - realPart;
        }
        else
        {
            realPart = displayRange[0] + realPart;
        }
        if(paramPanel.isIInverted())
        {
            imagPart = displayRange[3] + imagPart;
        }
        else
        {
            imagPart = displayRange[3] - imagPart;
        }
        return new Complex(realPart, imagPart);
    }

    /**
     * For calling during a paintComponent method. Gets rid of things (like lines from old drag rectangles)
     * by painting a rectangle over them according to the rules in paintComponent. Better to call this than
     * repaint because repaint will only schedule the panel for repainting, whereas this will do it
     * immediately.
     */
    public void paintRect(Graphics g, int rectx, int recty, int width, int height)
    {
        Complex userSelectedPoint = FractalDisplay.getMainWindow().getTopDisplay().getLastPoint();
        this.iterations = FractalDisplay.getMainWindow().getParamPanel().getIterations();

        //The Complex whose corresponding pixel is currently being coloured in the FractalPanel
        Complex currentPoint;

        //The first term in the iteration sequence for this fractal
        Complex firstTerm;

        //The previous term in the iteration sequence for this fractal
        Complex prevSeqValue;

        int iterationsManaged;

        //Stops the while loop if the escape condition is satisfied.
        for (int y = recty; y < recty + height; ++y) {
            for (int x = rectx; x < rectx + width; ++x) {
                currentPoint = this.getPanelCoordsAsComplex(x, y);

                firstTerm = this.parseComplex(this.processedFirstTerm, null, null, currentPoint, userSelectedPoint);
                iterationsManaged = 0;
                prevSeqValue = firstTerm.clone();
                while (!this.escape(prevSeqValue) && iterationsManaged < this.iterations + 1) {
                    ++iterationsManaged;
                    //Uses next term rule to determine how to reach the next term
                    prevSeqValue = this.parseComplex(this.processedNextTerm, prevSeqValue, firstTerm, currentPoint, userSelectedPoint);
                }
                g.setColor(this.chooseColour(prevSeqValue, iterationsManaged));
                g.drawLine(x, y, x, y);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(!(this.processedFirstTerm.equals("") || this.processedNextTerm.equals("")))
        {
            FractalDisplay mainWindow = FractalDisplay.getMainWindow();
            mainWindow.setTitle("Fractal Explorer (Drawing - please wait)");
            this.paintRect(g, 0, 0, this.getWidth(), this.getHeight());
            mainWindow.setTitle("Fractal Explorer");
        }
    }

    /**
     * Determines whether the point should stop the sequence
     */
    private boolean escape(Complex point)
    {
        switch(this.orbit)
        {
            case CIRCLE:
                //Creates a circle of radius 0.25.
                return Math.sqrt(point.modulusSquared()) <= 0.25;
            case CROSS_ENGLISH:
                //Creates a cross along the axes of thickness 0.1.
                return Math.abs(point.getRealPart()) <= 0.05
                        || Math.abs(point.getImagPart()) <= 0.05;
            case CROSS_SCOTTISH:
                //Creates a diagonal cross of thickness 0.1.
                double realPart = Math.abs(point.getRealPart());
                double imagPart = Math.abs(point.getImagPart());
                return realPart >= imagPart - 0.05 && realPart <= imagPart + 0.05;

            default:
                //The default: sequence terminates when modulus >= 2 (squared modulus >= 4).
                return point.modulusSquared() >= 4.0;
        }
    }

    /**
     * Decides the colour based on the point position and number of iterations managed.
     */
    private Color chooseColour(Complex point, int iterationsManaged)
    {
        switch(this.orbit)
        {
            case CIRCLE:
                double distance = Math.sqrt(point.modulusSquared());
                if(distance <= 0.25)
                {
                    float colourDist = 0.25F - (float) distance;
                    return new Color(4 * colourDist, 0, colourDist);
                }
                else
                {
                    return Color.black;
                }
            case CROSS_ENGLISH:
                double closestDistanceToAxis = Math.min(Math.abs(point.getRealPart()), Math.abs(point.getImagPart()));
                if(closestDistanceToAxis <= 0.05)
                {
                    float colourDist = 0.05F - (float) closestDistanceToAxis;
                    return new Color(20 * colourDist, 0, 0);
                }
                else
                {
                    return Color.white;
                }
            case CROSS_SCOTTISH:
                float realVsImagDifference = (float)Math.abs(Math.abs(point.getRealPart()) - Math.abs(point.getImagPart()));
                if(realVsImagDifference <= 0.05)
                {
                    realVsImagDifference *= 20;
                    return new Color(1.0F - realVsImagDifference, 1.0F - realVsImagDifference, 1.0F);
                }
                else
                {
                    return Color.BLUE;
                }
            case NONE:
                if (iterationsManaged > iterations)
                {
                    return Color.BLACK;
                }
                else if (iterationsManaged == 0)
                {
                    return Color.WHITE;
                }
                else if (iterationsManaged < (float) iterations / 3)
                {
                    return Color.LIGHT_GRAY;
                }
                else if (iterationsManaged < (float) iterations * 2 / 3)
                {
                    return Color.GRAY;
                }
                else
                {
                    return Color.DARK_GRAY;
                }
            default:
                return Color.BLACK;
        }
    }

    /**
     * Any instruction strings specified in the code are tested and valid, and any strings entered by the user are only
     * made available if they pass validateInstructions, so that s can be assumed to be valid. Any conditions not
     * covered in this method, such as the stacks being empty when a binary operator is processed, should therefore not
     * occur.
     * @param comp a valid expression to be parsed.
     * @param prev the previous Complex in the sequence
     * @param first the first Complex in the sequence
     * @param currentPoint the current point being drawn on the panel
     * @param userPoint the last point selected by the user
     * @return the Complex that is described by these instructions
     */
    private Complex parseComplex(String[] comp, Complex prev, Complex first, Complex currentPoint, Complex userPoint)
    {
        //Points to the next complex number in userComplexes to be checked for the first time.
        int nextComplexToCheck = 0;
        String currentString;
        char currentFirstChar;
        for(int x = 0; x < comp.length; ++x)
        {
            currentString = comp[x];
            currentFirstChar = currentString.charAt(0);
            if(currentFirstChar == 'p')
            {
                this.complexStack.push(prev.clone());
            }
            else if(currentFirstChar == 'u')
            {
                this.complexStack.push(userPoint.clone());
            }
            else if(currentFirstChar == 'c')
            {
                this.complexStack.push(currentPoint.clone());
            }
            else if(currentFirstChar == 'f')
            {
                this.complexStack.push(first.clone());
            }
            else if(currentFirstChar == '*')
            {
                char secondChar = currentString.charAt(1);
                if(secondChar == 'C')
                {
                    this.complexStack.push(this.complexStack.pop().multiplyBy(this.complexStack.pop()));
                }
                else if(secondChar == 'D')
                {
                    this.doubleStack.push(this.doubleStack.pop() * this.doubleStack.pop());
                }
            }
            else if(currentFirstChar == '+')
            {
                char secondChar = currentString.charAt(1);
                if(secondChar == 'C')
                {
                    this.complexStack.push(this.complexStack.pop().add(this.complexStack.pop()));
                }
                else if(secondChar == 'D')
                {
                    this.doubleStack.push(this.doubleStack.pop() + this.doubleStack.pop());
                }
            }
            else if(currentFirstChar == '-')
            {
                char secondChar = currentString.charAt(1);
                //Subtraction is not commutative, so order needs to be preserved
                if(secondChar == 'C')
                {
                    Complex newerComp = this.complexStack.pop();
                    this.complexStack.push(this.complexStack.pop()
                            .add(new Complex(-newerComp.getRealPart(), -newerComp.getImagPart())));
                }
                else if(secondChar == 'D')
                {
                    double newerDoub = this.doubleStack.pop();
                    this.doubleStack.push(this.doubleStack.pop() - newerDoub);
                }
            }
            else if(currentFirstChar == 'a')
            {
                this.doubleStack.push(Math.abs(this.doubleStack.pop()));
            }
            else if(currentFirstChar == 'r')
            {
                this.doubleStack.push(this.complexStack.pop().getRealPart());
            }
            else if(currentFirstChar == 'i')
            {
                this.doubleStack.push(this.complexStack.pop().getImagPart());
            }
            else if(currentFirstChar == 'D')
            {
                //There is a user-defined double here
                this.doubleStack.push(this.userDoubles[Integer.parseInt(currentString.substring(1))]);
            }
            else if(currentFirstChar == 'C')
            {
                //There is a user-defined complex here
                //The number after C is the position in userComplexStrings or userComplexes of the desired complex
                int complexIndex = Integer.parseInt(currentString.substring(1));
                if(complexIndex >= nextComplexToCheck && this.userComplexStrings[complexIndex] != null)
                {
                    //The index is pointing to a complex that has not been calculated yet, AND the string is null
                    //so calculation is required.
                    //Parse the corresponding string and store the result at complexIndex
                    this.userComplexes[complexIndex] = this.parseComplex(this.userComplexStrings[complexIndex], prev, first, currentPoint, userPoint);
                    ++nextComplexToCheck;
                }
                //At this point the complex value at complexIndex will have been calculated and will be available in
                //userComplexes.
                this.complexStack.push(this.userComplexes[complexIndex].clone());
            }
        }
        return this.complexStack.pop();
    }

    /**
     * Checks that the raw instruction string is valid. If it is valid (i.e. no exceptions were thrown), the processed
     * instruction is set. What constitutes a valid formula is described fully in the window displayed by the Create New
     * Fractal Button (see FractalSelectPanel#createFractal ActionListener code).
     *
     * This method also adjusts the string to make it as quick to parse as possible. Any numerical values from the raw
     * string that are specified by the user are placed in userDoubles and replaced with a $ character.
     * The first ten unique complex numbers specified by the user are replaced by single-digit values which refer to
     * their place in an array. If any complex numbers are equal they are replaced by the same number. Then, when
     * parsing, the complex number only needs to be evaluated once and stored in another array. The string is also much
     * shorter so the loop takes less time.
     *
     * Throws the exception per error because a string that is invalid in one way can lead to valid parts of
     * the string being wrongly listed as invalid.
     */
    public void validateInstructions() throws InvalidInstructionsException
    {
        String rawInstructions = this.rawInstructions;

        //Checks that there is only one semicolon
        String[] instructionParts = rawInstructions.split(";");
        int noOfParts = instructionParts.length;
        if (noOfParts < 2)
        {
            throw new InvalidInstructionsException("There was no semicolon to separate the first term from the rule " +
                    "for the next term.");
        }
        else if (noOfParts > 2)
        {
            throw new InvalidInstructionsException("Only one semicolon is permitted.");
        }

        //The only difference in syntax between the first term and next term rules is that the first term cannot contain
        //f or p (which can refer to the first term) because the first term is a base case so cannot be defined in terms
        //of itself. The first term therefore has to be checked for f or p and then normal validation rules will apply
        //to both.
        String firstTerm = instructionParts[0];
        char currentChar;
        for (int x = 0; x < firstTerm.length(); ++x)
        {
            currentChar = firstTerm.charAt(x);
            if (currentChar == 'f')
            {
                throw new InvalidInstructionsException("Cannot use the character f when defining the first term.");
            }
            else if (currentChar == 'p')
            {
                throw new InvalidInstructionsException("Cannot use the character p when defining the first term.");
            }
        }
        ArrayList<String> firstTermParts;
        String[] postfixFirstTerm;
        ArrayList<String> nextTermParts;
        String[] postfixNextTerm;
        try
        {
            firstTermParts = this.breakUpExpression(firstTerm);
            postfixFirstTerm = this.setInfixListToPostfix(firstTermParts);
            nextTermParts = this.breakUpExpression(instructionParts[1]);
            postfixNextTerm = this.setInfixListToPostfix(nextTermParts);
        }
        catch(InvalidInstructionsException iie)
        {
            throw iie;
        }

        //Loops through the list to check for user-defined doubles. If any are found, they are added to userDoubles
        //and replaced with a reference to an item of userDoubles, for faster processing later on.
        int x = 0;
        String currentString;
        while (x < postfixFirstTerm.length)
        {
            currentString = postfixFirstTerm[x];
            if(this.isComplex(currentString) || this.isBinaryOperator(currentString.charAt(0))
                    || this.isUnaryOperator(currentString.charAt(0)))
            {
                ++x;
            }
            else
            {
                if(this.isDouble(currentString))
                {
                    int userDoubleIndex = this.addNewUserDouble(currentString);
                    postfixFirstTerm[x] = "D"+userDoubleIndex;
                    ++x;
                }
                else
                {
                    throw new InvalidInstructionsException("'"+currentString+"' is not a valid number.");
                }
            }
        }

        //Loops through the list again, to check for user-defined complex numbers. If any are found, they are added to
        //one of two arrays and replaced with a reference to an item of userComplexStrings, for faster processing later
        //on.
        x = 0;
        while (x < postfixFirstTerm.length) {
            currentString = postfixFirstTerm[x];
            if (this.isDouble(currentString) || this.isBinaryOperator(currentString.charAt(0))
                    || this.isUnaryOperator(currentString.charAt(0))) {
                ++x;
            } else {
                //This character could be the start of a complex.
                if (this.isComplex(currentString)) {
                    int userComplexIndex = this.addNewUserComplex(currentString);
                    postfixNextTerm[x] = "C" + userComplexIndex;
                    ++x;
                } else {
                    throw new InvalidInstructionsException("'" + currentString + "' is not a valid complex number.");
                }
            }
        }

        //Checks that both sections of the instruction string evaluate to complex numbers, by doing a test parse of
        //both. Failure to return a complex number means the string was invalid in a way that was not caught by the
        //previous code.
        Complex zero = new Complex(0, 0);
        try
        {
            this.parseComplex(postfixFirstTerm, zero, zero, zero, zero);
        }
        catch(Exception e)
        {
            throw new InvalidInstructionsException("The result of the first term is not a complex number.");
        }
        try {
            this.parseComplex(postfixNextTerm, zero, zero, zero, zero);
        }
        catch(Exception e)
        {
            throw new InvalidInstructionsException("The result of the rule for the next term is not a complex number.");
        }

        //At this point, the instruction string is considered valid.
        this.processedFirstTerm = postfixFirstTerm;
        this.processedNextTerm = postfixNextTerm;
    }

    /**
     * Rearranges the ArrayList's items to resemble a postfix expression; these are much quicker and easier to parse
     * than infix.
     * Brackets, division and indices are currently unsupported, but the order of evaluation is as follows:
     * 1. Unary operators
     * 2. Multiplication
     * 3. Addition
     * 4. Subtraction
     * Also catches leading binary operators, and invalid doubles and complex numbers because it calls isDouble and
     * isComplex.
     */
    private String[] setInfixListToPostfix(ArrayList<String> infixList) throws InvalidInstructionsException
    {
        String prevString;
        String currentString;
        String nextString;
        char firstChar;
        //Loop backwards through the list to deal with unary operators. Avoids using extra conditional branches with
        //chained unary operators.
        for(int x = infixList.size() - 1;  x >= 0; --x)
        {
            currentString = infixList.get(x);
            if(currentString.equals("r") || currentString.equals("i"))
            {
                if(x == infixList.size() - 1)
                {
                    //Trailing unary operator
                    throw new InvalidInstructionsException("Cannot end an expression with 'r' or 'i'.");
                }
                else
                {
                    prevString = infixList.get(x + 1);
                    if(this.isComplex(prevString))
                    {
                        String newTerm = prevString + "|" + currentString;
                        //Remove these two strings from the list and add the new one
                        infixList.remove(x);
                        infixList.remove(x);
                        infixList.add(x, newTerm);
                    }
                    else if(this.isDouble(prevString))
                    {
                        throw new InvalidInstructionsException("Can only use 'r' or 'i' with a complex number.");
                    }
                    else
                    {
                        throw new InvalidInstructionsException("'"+prevString+"' is not a recognized number.");
                    }
                }
            }
            else if(currentString.equals("a"))
            {
                if(x == infixList.size() - 1)
                {
                    //Trailing unary operator
                    throw new InvalidInstructionsException("Cannot end an expression with 'a'.");
                }
                else
                {
                    prevString = infixList.get(x + 1);
                    if (this.isDouble(prevString)) {
                        String newTerm = prevString + "|" + currentString;
                        //Remove these two strings from the list and add the new one
                        infixList.remove(x);
                        infixList.remove(x);
                        infixList.add(x, newTerm);
                    } else if (this.isComplex(prevString)) {
                        throw new InvalidInstructionsException("Can only use 'a' with a complex number.");
                    } else {
                        throw new InvalidInstructionsException("'" + prevString + "' is not a recognized value.");
                    }
                }
            }
        }

        //In other cases, can loop through the list forwards.
        for(int x = 0; x < infixList.size(); ++x)
        {
            currentString = infixList.get(x);
            firstChar = currentString.charAt(0);
            //Checks if currentString is a multiplication sign
            if(firstChar == '*')
            {
                if(x == 0)
                {
                    //Leading binary operator
                    throw new InvalidInstructionsException("Cannot start an expression with '*'.");
                }
                prevString = infixList.get(x - 1);
                nextString = infixList.get(x + 1);
                if(this.isDouble(prevString))
                {
                    //The previous value is a double so the next value should also be a double
                    if(this.isDouble(nextString))
                    {
                        //Operating on two doubles is valid
                        //Marks the operator as operating on doubles
                        currentString += "D";
                    }
                    else if(this.isComplex(nextString))
                    {
                        throw new InvalidInstructionsException("Unable to multiply complex numbers with real numbers.");
                    }
                    else
                    {
                        throw new InvalidInstructionsException(nextString+" is not recognized as a valid real or complex number.");
                    }
                    //Group these three strings together in one string, so that they can't be separated by future
                    //rearrangements. Adds a comma between them so they can be easily separated later.
                    String newTerm = prevString+"|"+nextString+"|"+currentString;
                    //Replace these three strings with the new concatenated string.
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.add(x - 1, newTerm);
                }
                else if(this.isComplex(prevString))
                {
                    //The previous value is a complex so the next value should also be a complex
                    if(this.isComplex(nextString))
                    {
                        //Operating two complexes is valid
                        //Marks the operator as operating on complexes
                        currentString += "C";
                    }
                    else if(this.isDouble(nextString))
                    {
                        throw new InvalidInstructionsException("Unable to multiply complex numbers with real numbers.");
                    }
                    else
                    {
                        throw new InvalidInstructionsException(nextString+" is not recognized as a valid real or complex number.");
                    }
                    //Group these three strings together in one string, so that they can't be separated by future
                    //rearrangements. Adds a comma between them so they can be easily separated later.
                    String newTerm = prevString+"|"+nextString+"|"+currentString;
                    //Replace these three strings with the new concatenated string.
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.add(x - 1, newTerm);
                }
                else
                {
                    throw new InvalidInstructionsException(prevString+" and "+nextString+" are not recognized as values.");
                }
            }
        }
        for(int x = 0; x < infixList.size(); ++x)
        {
            currentString = infixList.get(x);
            firstChar = currentString.charAt(0);
            //Checks if currentString is a multiplication sign
            if(firstChar == '+' || firstChar == '-')
            {
                if(x == 0)
                {
                    //Leading binary operator
                    throw new InvalidInstructionsException("Cannot start an expression with '+' or '-'.");
                }
                prevString = infixList.get(x - 1);
                nextString = infixList.get(x + 1);
                if(this.isDouble(prevString))
                {
                    //The previous value is a double so the next value should also be a double
                    if(this.isDouble(nextString))
                    {
                        //Operating on two doubles is valid
                        //Marks the operator as operating on doubles
                        currentString += "D";
                    }
                    else if(this.isComplex(nextString))
                    {
                        throw new InvalidInstructionsException("Unable to add or subtract complex numbers with real numbers.");
                    }
                    else
                    {
                        throw new InvalidInstructionsException(nextString+" is not recognized as a valid real or complex number.");
                    }
                    //Group these three strings together in one string, so that they can't be separated by future
                    //rearrangements. Adds a comma between them so they can be easily separated later.
                    String newTerm = prevString+"|"+nextString+"|"+currentString;
                    //Replace these three strings with the new concatenated string.
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.add(x - 1, newTerm);
                }
                else if(this.isComplex(prevString))
                {
                    //The previous value is a complex so the next value should also be a complex
                    if(this.isComplex(nextString))
                    {
                        //Operating two complexes is valid
                        //Marks the operator as operating on complexes
                        currentString += "C";
                    }
                    else if(this.isDouble(nextString))
                    {
                        throw new InvalidInstructionsException("Unable to add or subtract complex numbers with real numbers.");
                    }
                    else
                    {
                        throw new InvalidInstructionsException(nextString+" is not recognized as a valid real or complex number.");
                    }
                    //Group these three strings together in one string, so that they can't be separated by future
                    //rearrangements. Adds a comma between them so they can be easily separated later.
                    String newTerm = prevString+"|"+nextString+"|"+currentString;
                    //Replace these three strings with the new concatenated string.
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.remove(x - 1);
                    infixList.add(x - 1, newTerm);
                }
                else
                {
                    throw new InvalidInstructionsException(prevString+" and "+nextString+" are not recognized as values.");
                }
            }
        }
        return infixList.get(0).split("\\|");
    }

    /**
     * Splits the expression into a list of binary operators and (possible) operands, preserving order, making
     * infix-postfix conversion easier.
     * Catches trailing operators, leading binary operators and unbalanced square brackets, so there is no need to check
     * for these in subsequent code.
     * @param exp the expression to split
     * @return the list
     */
    private ArrayList<String> breakUpExpression(String exp) throws InvalidInstructionsException
    {
        String restOfExp = exp;
        //Set to true when a [ is found. This causes the loop to continue until a ] is found; then the whole complex
        //can be added to the list and parsed separately.
        boolean discoveredComplexStart = false;
        ArrayList<String> parts = new ArrayList<String>();
        //Start looping through the expression
        int index = 0;
        char currentChar;
        while(index < restOfExp.length()) {
            currentChar = restOfExp.charAt(index);
            //Check for square brackets
            if (currentChar == '[') {
                if (discoveredComplexStart) {
                    //User has opened a square bracket without closing a previous one first
                    throw new InvalidInstructionsException("You must close the previous '[' before entering a new one.");
                } else {
                    discoveredComplexStart = true;
                }
            } else if (currentChar == ']') {
                if (discoveredComplexStart) {
                    //In a valid string, this marks the end of a valid user-defined complex.
                    //Add the ] and everything before it to the list
                    //However the [] were only needed to separate out the complex from the rest of the expression, so
                    //they are not added to the list.
                    parts.add(restOfExp.substring(1, index));
                    if (index < restOfExp.length() - 1) {
                        //There is leftover text to evaluate
                        restOfExp = restOfExp.substring(index + 1);
                    }
                    discoveredComplexStart = false;
                    index = -1;
                } else {
                    //The user added a ] before adding a [
                    throw new InvalidInstructionsException("You must provide a '[' before you can use ']'.");
                }
            }

            else if (!discoveredComplexStart)
            {
                //Break up the text as usual.
                if (isUnaryOperator(currentChar)) {
                    parts.add(restOfExp.substring(index, index + 1));
                    if (index == restOfExp.length() - 1)
                    {
                        //There is a trailing unary operator
                        throw new InvalidInstructionsException("Cannot end an expression with a unary operator.");
                    }
                    restOfExp = restOfExp.substring(index + 1);
                    index = -1;
                } else if (isBinaryOperator(currentChar)) {
                    if (index == restOfExp.length() - 1) {
                        //The binary operator is trailing, so it is missing its right operand!
                        throw new InvalidInstructionsException("Cannot end an expression with a binary operator.");
                    }
                    if (index > 0) {
                        parts.add(restOfExp.substring(0, index));
                    }

                    parts.add(restOfExp.substring(index, index + 1));
                    restOfExp = restOfExp.substring(index + 1);
                    index = -1;
                }
            }
            ++index;
            //Otherwise, the character is assumed to be a valid non-special character that is part of a user-defined
            //double or complex. If it is invalid, a later validation sub-process will throw an exception.
        }
        parts.add(restOfExp);
        return parts;
    }

    private boolean isBinaryOperator(char c)
    {
        return c == '*' || c == '+' || c == '-';
    }

    private boolean isUnaryOperator(char c)
    {
        return c == 'a' || c == 'r' || c == 'i';
    }

    /**
     * Special complexes can be referenced with just one letter, and do not need to be explicitly defined in the
     * instruction string. The exceptions to this are f and p in the definition of the first term, because they can
     * refer to the first term.
     */
    private boolean isSpecialComplex(char c)
    {
        return c == 'c' || c == 'f' || c == 'p' || c == 'u';
    }

    /**
     * A correct result is unlikely if this is called after setInfixListToPostfix.
     */
    private boolean isDouble(String s)
    {
        char firstChar = s.charAt(0);
        int stringLength = s.length();
        if(stringLength == 2)
        {
            //s is one of the user-defined doubles
            return firstChar == 'D' || (this.isSpecialComplex(s.charAt(1)) && (firstChar == 'i' || firstChar == 'r'));
        }
        else if(stringLength > 2)
        {
            //Could be 'r|[a complex]' or 'i|[a complex]'.
            return ((firstChar == 'r' || firstChar == 'i') && s.charAt(1) == '|' && this.isComplex(s.substring(2)))
                    //Could be 'a|[a double]
                    || (firstChar == 'a' || s.charAt(1) == '|' && this.isDouble(s.substring(2)));
        }
        else if(stringLength >= 6)
        {
            //The double might be the internal representation of an expression for the result of two doubles and an
            //operator, such as '0.5|0.8|+D', which can be generated in setInfixListToPostfix. In this case its minimum
            //length is 6; if a result is a double it will end in D.
            return s.charAt(stringLength - 1) == 'D';
        }
        else
        {
            return false;
        }
    }

    /**
     * Non-special complexes are defined in instruction strings as [a,b], where and and b are valid doubles.
     * a is the real part and b is the coefficient of i in the imaginary part. The minimum length of such
     * complexes in instruction strings is 5, as in [0,0].
     * A correct result is unlikely if this is called after setInfixListToPostfix.
     */
    private boolean isComplex(String s)
    {
        int stringLength = s.length();
        if(stringLength == 1)
        {
            //Only the special Complex numbers can take up a single character.
            return isSpecialComplex(s.charAt(0));
        }
        else if(stringLength == 2)
        {
            return s.charAt(0) == 'C';
        }
        else if(stringLength >= 3)
        {
            //The string could be a user-defined complex, such as '0,0'. These have a minimum length of 3.
            String[] possibleComplexParts = s.split(",");
            if(possibleComplexParts.length == 2)
            {
                return this.isDouble(possibleComplexParts[0]) && this.isDouble(possibleComplexParts[1]);
            }
            else
            {
                return false;
            }
        }
        else if(stringLength >= 6)
        {
            //The complex might be the internal representation of an expression for the result of two complexes and an
            //operator, such as 'p,p,*C', which can be generated in setInfixListToPostfix. In this case its minimum
            //length is 6; if a result is a complex it will end in C.
            return s.charAt(stringLength - 1) == 'C';
        }
        return false;
    }

    /**
     * Operates very similarly to addNewUserDouble. Strings which will not have a constant value are placed in
     * userComplexStrings. Strings which will are converted straight to complex numbers and stored in userComplexes.
     */
    private int addNewUserComplex(String nextComplexString) throws ArrayIndexOutOfBoundsException, NumberFormatException
    {
        //If the complex takes the form [Dx,Dy], it is made up of user-defined numbers so it will not need
        //to be recalculated. This checks for that pattern.
        if(nextComplexString.matches("\\[\\D\\d,\\D\\d\\]"))
        {
            //This complex can go straight to userComplexes
            Complex newComplex = this.parseComplex(nextComplexString, null, null, null, null);
            int numberToTry = -1;
            while(numberToTry > -2)
            {
                ++numberToTry;
                if(this.userComplexes[numberToTry] == null && this.userComplexStrings[numberToTry] == null)
                {
                    this.userComplexes[numberToTry] = newComplex;
                    return numberToTry;
                }
                else if(this.userComplexes[numberToTry] != null)
                {
                    if(this.userComplexes[numberToTry].getRealPart() == newComplex.getRealPart()
                            &&this.userComplexes[numberToTry].getImagPart() == newComplex.getImagPart())
                    {
                        return numberToTry;
                    }
                }
            }
        }
        else
        {
            //The complex needs to be validated. If it is found to be valid it goes to userComplexStrings because
            //it doesn't just contain user doubles, so it may need to be recalculated every iteration.
            if(this.isComplex(nextComplexString))
            {
                int numberToTry = -1;
                while(numberToTry > -2)
                {
                    ++numberToTry;
                    if(this.userComplexes[numberToTry] == null && this.userComplexStrings[numberToTry] == null)
                    {
                        this.userComplexStrings[numberToTry] = nextComplexString;
                        return numberToTry;
                    }
                    else if(this.userComplexStrings[numberToTry] != null)
                    {
                        if(this.userComplexStrings[numberToTry].equals(nextComplexString))
                        {
                            return numberToTry;
                        }
                    }
                }
            }
            else
            {
                throw new NumberFormatException();
            }
        }
        //Should have thrown an exception before this point
        return 666;
    }

    /**
     * If the candidate double is valid, this iterates through userDoubles to find a place for it.
     * @return The index in the array of this double value.
     */
    private int addNewUserDouble(String nextDoubleString) throws ArrayIndexOutOfBoundsException, NumberFormatException
    {
        Double nextDouble = Double.parseDouble(nextDoubleString);
        //At this point the double is valid.
        int numberToTry = -1;
        //If the item being checked is null, all previous doubles must have been checked and found to be
        //equal.
        //If the item being checked is equal to nextDouble, the loop simply stops.
        //Once the loop stops, the double string is replaced with D followed by the index the loop
        //stopped at, which effectively points to the double in the array.
        while(numberToTry > -2)
        {
            ++numberToTry;
            if(this.userDoubles[numberToTry] == null)
            {
                this.userDoubles[numberToTry] = nextDouble;
                return numberToTry;
            }
            else if(this.userDoubles[numberToTry].equals(nextDouble))
            {
                return numberToTry;
            }
        }
        //Should have thrown an exception before reaching this point
        return 666;
    }

    public void setOrbitTrap(String name)
    {
        if(name.equals("None"))
        {
            this.orbit = Orbit.NONE;
        }
        else if(name.equals("Circle"))
        {
            this.orbit = Orbit.CIRCLE;
        }
        else if(name.equals("Cross (English)"))
        {
            this.orbit = Orbit.CROSS_ENGLISH;
        }
        else if(name.equals("Cross (Scottish)"))
        {
            this.orbit = Orbit.CROSS_SCOTTISH;
        }
    }

    //BASIC GETTERS AND SETTERS

    public void setRawInstructions(String instructions)
    {
        this.rawInstructions = instructions;
    }
}