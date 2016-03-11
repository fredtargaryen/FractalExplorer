package gui.panels.fractals;

import exceptions.InvalidInstructionsException;
import gui.FractalDisplay;
import gui.panels.info.DisplayParameterPanel;
import numbers.Complex;

import javax.swing.*;
import java.awt.*;
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
     * Stack of symbols in the expression being parsed, which determine the calculations to be done.
     */
    private Stack<Character> operatorStack;

    /**
     * The instruction string as entered by the user.
     */
    protected String rawInstructions;

    /**
     * The definition of the iteration sequence's first term, validated and edited by the program for faster processing.
     */
    protected String processedFirstTerm;

    /**
     * The definition of the iteration sequence's next term, validated and edited by the program for faster processing.
     */
    protected String processedNextTerm;

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
    protected String[] userComplexStrings;

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
        this.operatorStack = new Stack<Character>();
        this.rawInstructions = "";
        this.processedFirstTerm = "";
        this.processedNextTerm = "";
        this.userDoubles = new Double[NO_OF_ALLOWED_USER_VALUES];
        this.userComplexStrings = new String[NO_OF_ALLOWED_USER_VALUES];
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
        if(userSelectedPoint != null || (!this.processedFirstTerm.contains("u") && !this.processedNextTerm.contains("u")))
        {
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
                if(distance <= 0.5)
                {
                    float colourDist = 0.5F - (float) distance;
                    return new Color(2 * colourDist, 0, colourDist);
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
     * covered in this method, such as the stacks being empty when a binary operator is processed, are therefore not
     * going to occur.
     * @param s a valid string to be parsed.
     * @param prev the previous Complex in the sequence
     * @param first the first Complex in the sequence
     * @param currentPoint the current point being drawn on the panel
     * @param userPoint the last point selected by the user
     * @return the Complex that is described by these instructions
     */
    private Complex parseComplex(String s, Complex prev, Complex first, Complex currentPoint, Complex userPoint)
    {
        int x = 0;
        char currentChar;
        //Used by binary operators to check if they are operating on doubles or Complexes.
        //True if a double was last pushed; false if a Complex.
        boolean doubleLastPushed = true;
        int stringLength = s.length();

        //Points to the next complex number in userComplexes to be checked for the first time.
        int nextComplexToCheck = 0;
        //When an item is pushed that cannot lead directly to some double or Complex processing, this is set to false
        //to minimize the number of times the program has to enter the stack loop.
        boolean enterStackLoop;
        while(x < stringLength)
        {
            enterStackLoop = true;
            currentChar = s.charAt(x);

            if(currentChar == 'D')
            {
                //There is a user-defined double here
                ++x;
                //The number after D is the position in userDoubles of the desired double
                this.doubleStack.push(this.userDoubles[Integer.parseInt(s.substring(x, x + 1))]);
                doubleLastPushed = true;
            }
            else if(currentChar == 'C')
            {
                //There is a user-defined complex here
                ++x;
                //The number after C is the position in userComplexStrings or userComplexes of the desired complex
                int complexIndex = Integer.parseInt(s.substring(x, x + 1));
                //If the index is pointing to a complex that has not been calculated yet, and the string is null
                //so calculation is required:
                if(complexIndex >= nextComplexToCheck && this.userComplexStrings[complexIndex] != null)
                {
                    //Protect all current stack items while parsing
                    int prevCompProtect = this.noOfProtectedComplexes;
                    this.noOfProtectedComplexes = this.complexStack.size();
                    int prevDoubProtect = this.noOfProtectedDoubles;
                    this.noOfProtectedDoubles = this.doubleStack.size();

                    //Parse the corresponding string and store the result at complexIndex
                    this.userComplexes[complexIndex] = this.parseComplex(this.userComplexStrings[complexIndex], prev, first, currentPoint, userPoint);
                    ++nextComplexToCheck;
                    //Stop reserving items
                    this.noOfProtectedComplexes = prevCompProtect;
                    this.noOfProtectedDoubles = prevDoubProtect;
                }
                //At this point the complex value at complexIndex will already be calculated and available in
                //userComplexes.
                this.complexStack.push(this.userComplexes[complexIndex].clone());
                doubleLastPushed = false;
            }
            else if (currentChar == 'p') {
                this.complexStack.push(prev.clone());
                doubleLastPushed = false;
            }
            else if(currentChar == 'u') {
                this.complexStack.push(userPoint.clone());
                doubleLastPushed = false;
            }
            else if(currentChar == 'c') {
                this.complexStack.push(currentPoint.clone());
                doubleLastPushed = false;
            }
            else if(currentChar == 'f')
            {
                this.complexStack.push(first.clone());
                doubleLastPushed = false;
            }
            else if(currentChar == ',')
            {
                ++this.noOfProtectedDoubles;
                this.operatorStack.push(currentChar);
            }
            else if(this.isBinaryOperator(currentChar))
            {
                if(doubleLastPushed)
                {
                    ++this.noOfProtectedDoubles;
                }
                else
                {
                    ++this.noOfProtectedComplexes;
                }
                this.operatorStack.push(currentChar);
            }
            else if(currentChar == ']')
            {
                this.operatorStack.push(currentChar);
            }
            else
            {
                //The character is a unary operator or [, so it can be pushed straight on and there is no need to
                //enter the stack loop.
                this.operatorStack.push(currentChar);
                enterStackLoop = false;
            }

            //The stack loop pops and processes as many operators as it can before exitStackLoop gets set to true.
            if(enterStackLoop)
            {
                char topOperator;
                //Used for characters that are waiting for more operands and so are not ready to take from the stack.
                boolean exitStackLoop = false;
                int opStackSize = this.operatorStack.size();
                while (opStackSize > 0 && !exitStackLoop) {
                    topOperator = this.operatorStack.peek();
                    if (topOperator == '+')
                    {
                        if(doubleLastPushed)
                        {
                            int doubStackSize = this.doubleStack.size();
                            if (doubStackSize > this.noOfProtectedDoubles) {
                                this.doubleStack.push(this.doubleStack.pop() + this.doubleStack.pop());
                                this.operatorStack.pop();
                                --this.noOfProtectedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            int compStackSize = this.complexStack.size();
                            if (compStackSize > this.noOfProtectedComplexes) {
                                this.complexStack.push(this.complexStack.pop().add(this.complexStack.pop()));
                                this.operatorStack.pop();
                                --this.noOfProtectedComplexes;
                                --opStackSize;
                            } else {
                                exitStackLoop = true;
                            }
                        }
                    }
                    else if(topOperator == '-')
                    {
                        if(doubleLastPushed)
                        {
                            int doubStackSize = this.doubleStack.size();
                            if (doubStackSize > this.noOfProtectedDoubles)
                            {
                                Double toSubtract = this.doubleStack.pop();
                                this.doubleStack.push(this.doubleStack.pop() - toSubtract);
                                this.operatorStack.pop();
                                --this.noOfProtectedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            if (this.complexStack.size() > this.noOfProtectedComplexes) {
                                Complex complexToSubtract = this.complexStack.pop();
                                complexToSubtract = new Complex(-complexToSubtract.getRealPart(), -complexToSubtract.getImagPart());
                                this.complexStack.push(this.complexStack.pop().add(complexToSubtract));
                                this.operatorStack.pop();
                                --this.noOfProtectedComplexes;
                                --opStackSize;
                            } else {
                                exitStackLoop = true;
                            }
                        }
                    }
                    else if (topOperator == '*')
                    {
                        if(doubleLastPushed)
                        {
                            int doubStackSize = this.doubleStack.size();
                            if (doubStackSize > this.noOfProtectedDoubles) {
                                this.doubleStack.push(this.doubleStack.pop() * this.doubleStack.pop());
                                this.operatorStack.pop();
                                --this.noOfProtectedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            int compStackSize = this.complexStack.size();
                            if (compStackSize > this.noOfProtectedComplexes) {
                                this.complexStack.push(this.complexStack.pop().multiplyBy(this.complexStack.pop()));
                                this.operatorStack.pop();
                                --this.noOfProtectedComplexes;
                                --opStackSize;
                            } else {
                                exitStackLoop = true;
                            }
                        }
                    }
                    else if (topOperator == 'a') {
                        if (this.doubleStack.size() >= this.noOfProtectedDoubles + 1) {
                            this.doubleStack.push(Math.abs(this.doubleStack.pop()));
                            this.operatorStack.pop();
                            --opStackSize;
                        } else {
                            exitStackLoop = true;
                        }
                    } else if (topOperator == 'r') {
                        if (this.complexStack.size() >= this.noOfProtectedComplexes + 1) {
                            this.doubleStack.push(this.complexStack.pop().getRealPart());
                            this.operatorStack.pop();
                            --opStackSize;
                        } else {
                            exitStackLoop = true;
                        }
                    } else if (topOperator == 'i') {
                        if (this.complexStack.size() >= this.noOfProtectedComplexes + 1) {
                            this.doubleStack.push(this.complexStack.pop().getImagPart());
                            this.operatorStack.pop();
                            --opStackSize;
                        } else {
                            exitStackLoop = true;
                        }
                    } else if (topOperator == '[' || topOperator == ',') {
                        exitStackLoop = true;
                    }
                    //The top symbol must be ]
                    else {
                        double imagPart = this.doubleStack.pop();
                        this.complexStack.push(new Complex(this.doubleStack.pop(), imagPart));
                        this.operatorStack.pop();
                        this.operatorStack.pop();
                        --this.noOfProtectedDoubles;
                        this.operatorStack.pop();
                        opStackSize -= 3;
                    }
                }
            }
            ++x;
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

        //Loops through the string to check for user-defined doubles. If any are found, they are added to userDoubles
        //and replaced with a reference to an item of userDoubles, for faster processing later on.
        int possValueStartIndex = -1;
        String doublesRemovedInstructions = "";
        String nextValueString = "";
        int x = 0;
        while (x < rawInstructions.length())
        {
            currentChar = rawInstructions.charAt(x);
            if (this.isSpecialComplex(currentChar) || this.isUnaryOperator(currentChar) ||
                    this.isBinaryOperator(currentChar) || currentChar == '[' || currentChar == ',' || currentChar == ']'
                    || currentChar == ';')
            {
                //currentChar can't possibly be part of a double
                if (possValueStartIndex > -1)
                {
                    //The method has detected the start of a new double before
                    try
                    {
                        //Replaces the user double with D followed by the index returned by addNewUserDouble,
                        //effectively pointing to the double in userDoubles.
                        doublesRemovedInstructions += rawInstructions.substring(0, possValueStartIndex) + "D"
                                + this.addNewUserDouble(rawInstructions.substring(possValueStartIndex, x));
                    }
                    catch(NumberFormatException nfe)
                    {
                        //The double string wasn't a valid double
                        throw new InvalidInstructionsException("'"+nextValueString+"' is not a valid number.");
                    }
                    catch(ArrayIndexOutOfBoundsException aioobe)
                    {
                        //The array was full so numberToTry went out of range
                        throw new InvalidInstructionsException("Too many numbers have been defined.");
                    }

                    //Restarts the search for doubles using the rest of rawInstructions
                    rawInstructions = rawInstructions.substring(x);
                    x = 0;
                    possValueStartIndex = -1;
                }
            }
            else
            {
                //This character could be the start of a double.
                if (possValueStartIndex == -1) {
                    possValueStartIndex = x;
                }
            }
            ++x;
        }
        //The whole string has been looped through.
        if (possValueStartIndex > -1)
        {
            //There is one more possible double left over
            doublesRemovedInstructions += rawInstructions.substring(0, possValueStartIndex) + "D"
                    + this.addNewUserDouble(rawInstructions.substring(possValueStartIndex));
        } else {
            //There are some operators and/or complexes left over
            doublesRemovedInstructions += rawInstructions;
        }

        //Loops through the string again, to check for user-defined complex numbers. If any are found, they are added to
        //one of two arrays and replaced with a reference to an item of userComplexStrings, for faster processing later
        //on.
        possValueStartIndex = -1;
        String complexesRemovedInstructions = "";
        nextValueString = "";
        x = 0;
        while (x < doublesRemovedInstructions.length())
        {
            currentChar = doublesRemovedInstructions.charAt(x);
            if(currentChar == '[') {
                //This character could be the start of a complex.
                if (possValueStartIndex == -1) {
                    possValueStartIndex = x;
                } else {
                    //The user is starting a new complex inside a new complex - not allowed
                    throw new InvalidInstructionsException("Cannot create a new complex number inside one that is still being created.");
                }
            }
            else if(currentChar == ']')
            {
                //This character could be the end of a valid complex.
                if(possValueStartIndex == -1)
                {
                    //Closing bracket before an opening bracket
                    throw new InvalidInstructionsException("Cannot use ']' without a '[' before it.");
                }
                else
                {
                    nextValueString = doublesRemovedInstructions.substring(possValueStartIndex, x + 1);
                    //Replaces the user complex with C followed by the index returned by addNewUserComplex,
                    //effectively pointing to the complex in userComplexes.
                    try
                    {
                        complexesRemovedInstructions += doublesRemovedInstructions.substring(0, possValueStartIndex) + "C"
                                + this.addNewUserComplex(nextValueString);
                    }
                    catch(NumberFormatException nfe)
                    {
                        //The complex string wasn't valid
                        throw new InvalidInstructionsException("'"+nextValueString+"' is not a valid complex number.");
                    }
                    catch(ArrayIndexOutOfBoundsException aioobe)
                    {
                        //The array was full so numberToTry went out of range
                        throw new InvalidInstructionsException("Too many complex numbers have been defined.");
                    }

                    //Restarts the search for complexes using the rest of doublesRemovedInstructions
                    doublesRemovedInstructions = doublesRemovedInstructions.substring(x + 1);
                    x = 0;
                    possValueStartIndex = -1;
                }
            }
            ++x;
        }
        //The whole string has been looped through.
        if (possValueStartIndex > -1)
        {
            //There is one more possible Complex left over
            complexesRemovedInstructions += doublesRemovedInstructions.substring(0, possValueStartIndex) + "C"
                    + this.addNewUserComplex(doublesRemovedInstructions.substring(possValueStartIndex));
        }
        else
        {
            //There are some operators and/or complexes left over
            complexesRemovedInstructions += doublesRemovedInstructions;
        }

        //Checks that both sections of the instruction string evaluate to complex numbers.
        instructionParts = complexesRemovedInstructions.split(";");
        if (!isComplex(instructionParts[0])) {
            throw new InvalidInstructionsException("The result of the first term is not a complex number.");
        }
        if (!isComplex(instructionParts[1])) {
            throw new InvalidInstructionsException("The result of the rule for the next term is not a complex number.");
        }

        //At this point, the instruction string is considered valid.
        this.processedFirstTerm = instructionParts[0];
        this.processedNextTerm = instructionParts[1];
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

    private boolean isDouble(String s)
    {
        char firstChar = s.charAt(0);
        int stringLength = s.length();
        if(stringLength == 2)
        {
            //s is one of the user-defined doubles
            return firstChar == 'D' || (this.isSpecialComplex(s.charAt(1)) && (firstChar == 'i' || firstChar == 'r'));
        }
        else
        {
            //s may instead be a string that evaluates to a double.

            //The result of a double, followed by a binary operator, followed by a double must be a double.
            //Iterates through the string until a binary operator is found. If one is found, all characters before
            //and after it are placed in a new array.
            String[] possibleParts;
            int x = 0;
            boolean foundBinOp = false;
            while (x < stringLength && !foundBinOp)
            {
                if (isBinaryOperator(s.charAt(x)))
                {
                    foundBinOp = true;
                }
                ++x;
            }
            if (foundBinOp) {
                if (x == 1 || x == stringLength)
                {
                    //A binary operator was found at the beginning or end of the string, which is not permitted.
                    return false;
                }
                else
                {
                    //The string has at least one binary operator, so the length of possibleParts is
                    //2. The result of a double, followed by a binary operator, followed by a double is always a double.
                    possibleParts = new String[]{s.substring(0, x - 1), s.substring(x)};
                    return this.isDouble(possibleParts[0]) && this.isDouble(possibleParts[1]);
                }
            }
            else
            {
                //The string has no binary operators.

                //The result of 'a' followed by a double must be a double
                return (firstChar == 'a' && this.isDouble(s.substring(1)) ||
                        //This is also true for 'r' or 'i' followed by a Complex
                        (this.isComplex(s.substring(1)) && (firstChar == 'r' || firstChar == 'i')));
            }
        }
    }

    /**
     * Non-special complexes are defined in instruction strings as [a,b], where and and b are valid doubles.
     * a is the real part and b is the coefficient of i in the imaginary part. The minimum length of such
     * complexes in instruction strings is 5, as in [0,0].
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
        //Two complexes of any length must have a binary operator between them, so if the string is length 2 then
        //that is invalid
        else if(stringLength > 2)
        {
            //Checks if the first complex is special
            if(isSpecialComplex(s.charAt(0)) && isBinaryOperator(s.charAt(1)) && isComplex(s.substring(2)))
            {
                return true;
            }
            else if(isComplex(s.substring(0, 2)) && isBinaryOperator(s.charAt(2)) && isComplex(s.substring(3)))
            {
                return true;
            }
            //Checks if the first complex is not special
            //Using the example string "[$,rp]*[ip,$]*c"
            else if(stringLength > 4)
            {
                if(s.charAt(0) == '[')
                {
                    //Searches for a ] in the expression
                    int x = 0;
                    boolean foundBracket = false;
                    String[] fullComplexParts;
                    while(x < stringLength && !foundBracket)
                    {
                        if (s.charAt(x) == ']')
                        {
                            foundBracket = true;
                        }
                        ++x;
                    }
                    if(foundBracket)
                    {
                        //There is a [ at the start and a ] at some point after it in this string.
                        //Item 0 of the array is whatever is inside the square brackets.
                        //Item 1 is the rest of the expression.
                        if (x == stringLength)
                        {
                            //The candidate complex takes up the whole string.
                            fullComplexParts = new String[]{s.substring(1, x - 1)};
                        }
                        else {
                            //There are some characters left over to process
                            //{"$,rp","*[ip,$]*c"}
                            fullComplexParts = new String[]{s.substring(1, x - 1), s.substring(x)};
                        }
                    }
                    else
                    {
                        //Candidate complex is invalid
                        return false;
                    }
                    //{"$","rp"}
                    String[] firstComplexParts = fullComplexParts[0].split(",");
                    if(fullComplexParts.length == 1)
                    {
                                            //"$"                             //"rp"
                        return isDouble(firstComplexParts[0]) && isDouble(firstComplexParts[1]);
                    }
                    else if(fullComplexParts.length == 2)
                    {                                //"*"
                        return isBinaryOperator(fullComplexParts[1].charAt(0))
                                && isDouble(firstComplexParts[0])
                                && isDouble(firstComplexParts[1])
                                                                //"[ip,$]*c"
                                &&isComplex(fullComplexParts[1].substring(1));
                    }
                }
            }
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