package gui.panels.fractals;

import exceptions.InvalidInstructionsException;
import gui.FractalDisplay;
import gui.panels.info.DisplayParameterPanel;
import numbers.Complex;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class FractalPanel extends JPanel
{
    protected int iterations;

    private Stack<Complex> complexStack;
    private Stack<Double> doubleStack;
    private Stack<Character> operatorStack;

    /**
     * The instruction string as entered by the user.
     */
    private String rawInstructions;

    /**
     * The instruction string, edited by the program for faster processing.
     */
    private String processedInstructions;

    /**
     * Any numerical values entered in rawInstructions.
     */
    private double[] userDoubles;

    /**
     * The index of the first user double in the rule for the next term. Will be constant for all instruction strings,
     * so only set once.
     */
    private int firstNextTermUserDoubleIndex;

    /**
     * Pointer to the current double being used.
     */
    private int userDoublesIndex;

    /**
     * If the first term contains no special complexes, it is composed of constant values and therefore only needs to
     * be calculated once. If this is null, the first term must contain at least one special complex, and therefore
     * needs to be recalculated.
     */
    private Complex constantFirstTerm;

    public FractalPanel()
    {
        super();
        this.setPreferredSize(new Dimension(600, 600));
        this.complexStack = new Stack<Complex>();
        this.doubleStack = new Stack<Double>();
        this.operatorStack = new Stack<Character>();
        this.rawInstructions = "";
        this.firstNextTermUserDoubleIndex = -1;
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
        String instructions = this.getProcessedInstructions();
        if(!instructions.equals(""))
        {
            this.iterations = FractalDisplay.getMainWindow().getParamPanel().getIterations();
            Complex userSelectedPoint = FractalDisplay.getMainWindow().getTopDisplay().getLastPoint();

            //The Complex whose corresponding pixel is currently being coloured in the FractalPanel
            Complex c;

            //The first term in the iteration sequence for this fractal
            Complex firstTerm = this.constantFirstTerm;

            //The previous term in the iteration sequence for this fractal
            Complex prevSeqValue;

            String[] parts = instructions.split(";");
            String firstTermDefinition = parts[0];
            String nextTermRule = parts[1];

            int iterationsManaged;

            //Stops the while loop if the squared modulus gets above 4
            boolean stop;
            for (int y = recty; y < height; ++y) {
                for (int x = rectx; x < width; ++x) {
                    c = this.getPanelCoordsAsComplex(rectx + x, recty + y);

                    this.userDoublesIndex = 0;
                    if(this.constantFirstTerm == null)
                    {
                        //The first term needs to be recalculated.
                        //First and previous terms can be null because the validation method has already been called, so
                        //it can be assumed that the first term will make no reference to them.
                        firstTerm = this.parseInstruction(firstTermDefinition, null, null, c, userSelectedPoint);
                    }
                    else
                    {
                        //If not, firstTerm remains equal to this.constantFirstTerm, and the index can move straight to
                        //the next term rule.
                        this.userDoublesIndex = this.firstNextTermUserDoubleIndex;
                    }
                    stop = false;
                    iterationsManaged = 0;
                    prevSeqValue = firstTerm.clone();
                    if (prevSeqValue.modulusSquared() < 4) {
                        while (iterationsManaged < this.iterations + 1 && !stop) {
                            ++iterationsManaged;
                            //Uses next term rule to determine how to reach the next term
                            prevSeqValue = this.parseInstruction(nextTermRule, prevSeqValue, firstTerm, c, userSelectedPoint);
                            this.userDoublesIndex = firstNextTermUserDoubleIndex;
                            if (prevSeqValue.modulusSquared() >= 4) {
                                stop = true;
                            }
                        }
                    }
                    g.setColor(this.chooseColour(iterationsManaged));
                    g.fillRect(rectx + x, recty + y, 1, 1);
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        FractalDisplay mainWindow = FractalDisplay.getMainWindow();
        mainWindow.setTitle("Fractal Explorer (Drawing - please wait)");
        this.paintRect(g, 0, 0, this.getWidth(), this.getHeight());
        mainWindow.setTitle("Fractal Explorer");
    }

    /**
     * Decides the colour based on the number of iterations performed before divergence
     */
    private Color chooseColour(int iterationsManaged)
    {
        if(iterationsManaged > iterations)
        {
            return Color.BLACK;
        }
        else if(iterationsManaged == 0)
        {
            return Color.WHITE;
        }
        else if(iterationsManaged < (float)iterations / 3)
        {
            return Color.LIGHT_GRAY;
        }
        else if(iterationsManaged < (float)iterations * 2 / 3)
        {
            return Color.GRAY;
        }
        else
        {
            return Color.DARK_GRAY;
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
    private Complex parseInstruction(String s, Complex prev, Complex first, Complex currentPoint, Complex userPoint)
    {
        this.userDoublesIndex = 0;
        int x = 0;
        char currentChar;
        //Used by binary operators to check if they are operating on doubles or Complexes.
        //True if a double was last pushed; false if a Complex.
        boolean doubleLastPushed = true;
        int stringLength = s.length();
        //Binary operators need to reserve the operand before them, or it will be operated on by subsequent operators.
        //This includes the comma.
        int noOfReservedDoubles = 0;
        int noOfReservedComplexes = 0;
        //When an item is pushed that cannot lead directly to some double or Complex processing, this is set to false
        //to minimize the number of times the program has to enter the stack loop.
        boolean enterStackLoop;
        while(x < stringLength)
        {
            enterStackLoop = true;
            currentChar = s.charAt(x);
            if (currentChar == '$') {
                this.doubleStack.push(this.userDoubles[this.userDoublesIndex]);
                doubleLastPushed = true;
                ++this.userDoublesIndex;
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
                ++noOfReservedDoubles;
                this.operatorStack.push(currentChar);
            }
            else if(this.isBinaryOperator(currentChar))
            {
                if(doubleLastPushed)
                {
                    ++noOfReservedDoubles;
                }
                else
                {
                    ++noOfReservedComplexes;
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

            //THE STACK LOOP
            if(enterStackLoop) {
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
                            if (doubStackSize > noOfReservedDoubles) {
                                this.doubleStack.push(this.doubleStack.pop() + this.doubleStack.pop());
                                this.operatorStack.pop();
                                --noOfReservedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            int compStackSize = this.complexStack.size();
                            if (compStackSize > noOfReservedComplexes) {
                                this.complexStack.push(this.complexStack.pop().add(this.complexStack.pop()));
                                this.operatorStack.pop();
                                --noOfReservedComplexes;
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
                            if (doubStackSize > noOfReservedDoubles)
                            {
                                Double toSubtract = this.doubleStack.pop();
                                this.doubleStack.push(this.doubleStack.pop() - toSubtract);
                                this.operatorStack.pop();
                                --noOfReservedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            if (this.complexStack.size() > noOfReservedComplexes) {
                                Complex complexToSubtract = this.complexStack.pop();
                                complexToSubtract = new Complex(-complexToSubtract.getRealPart(), -complexToSubtract.getImagPart());
                                this.complexStack.push(this.complexStack.pop().add(complexToSubtract));
                                this.operatorStack.pop();
                                --noOfReservedComplexes;
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
                            if (doubStackSize > noOfReservedDoubles) {
                                this.doubleStack.push(this.doubleStack.pop() * this.doubleStack.pop());
                                this.operatorStack.pop();
                                --noOfReservedDoubles;
                                --opStackSize;
                            }
                            else
                            {
                                exitStackLoop = true;
                            }
                        }
                        else {
                            int compStackSize = this.complexStack.size();
                            if (compStackSize > noOfReservedComplexes) {
                                this.complexStack.push(this.complexStack.pop().multiplyBy(this.complexStack.pop()));
                                this.operatorStack.pop();
                                --noOfReservedComplexes;
                                --opStackSize;
                            } else {
                                exitStackLoop = true;
                            }
                        }
                    }
                    else if (topOperator == 'a') {
                        if (this.doubleStack.size() >= noOfReservedDoubles + 1) {
                            this.doubleStack.push(Math.abs(this.doubleStack.pop()));
                            this.operatorStack.pop();
                            --opStackSize;
                        } else {
                            exitStackLoop = true;
                        }
                    } else if (topOperator == 'r') {
                        if (this.complexStack.size() >= noOfReservedComplexes + 1) {
                            this.doubleStack.push(this.complexStack.pop().getRealPart());
                            this.operatorStack.pop();
                            --opStackSize;
                        } else {
                            exitStackLoop = true;
                        }
                    } else if (topOperator == 'i') {
                        if (this.complexStack.size() >= noOfReservedComplexes + 1) {
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
                        --noOfReservedDoubles;
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
     * instruction is set. Any doubles from the raw instruction are placed in userDoubles and replaced with a $
     * character.
     * Throws the exception per error because a string that is invalid in one way can lead to valid parts of
     * the string being wrongly listed as invalid.
     */
    public void validateInstructions() throws InvalidInstructionsException {
        String rawInstructions = this.rawInstructions;

        //Checks that there is only one semicolon
        String[] instructionParts = rawInstructions.split(";");
        int noOfParts = instructionParts.length;
        if (noOfParts < 2) {
            throw new InvalidInstructionsException("There was no semicolon to separate the first term from the rule " +
                    "for the next term.");
        } else if (noOfParts > 2) {
            throw new InvalidInstructionsException("Only one semicolon is permitted.");
        }

        //The only difference in syntax between the first term and next term rules is that the first term cannot contain
        //f or p (which can refer to the first term) because the first term is a base case so cannot be defined in terms
        //of itself. The first term therefore has to be checked for f or p and then normal validation rules will apply
        //to both.
        //Also, if the first term contains any other special complex, it is flagged as needing to be recalculated for
        //each point.
        boolean canKeepConstant = true;
        String firstTerm = instructionParts[0];
        char currentChar;
        for (int x = 0; x < firstTerm.length(); ++x) {
            currentChar = firstTerm.charAt(x);
            if (currentChar == 'f') {
                throw new InvalidInstructionsException("Cannot use the character f when defining the first term.");
            } else if (currentChar == 'p') {
                throw new InvalidInstructionsException("Cannot use the character p when defining the first term.");
            } else if (isSpecialComplex(currentChar)) {
                canKeepConstant = false;
            }
        }

        //Checks for user-defined doubles. If any are found, they are added to userDoubles and replaced with a $
        //character for faster processing later on.
        ArrayList<String> possibleDoubles = new ArrayList<String>();
        int possDoubStartIndex = -1;
        String newFirstTerm = "";
        String newInstructions = "";
        int x = 0;
        while (x < rawInstructions.length()) {
            currentChar = rawInstructions.charAt(x);
            if (this.isSpecialComplex(currentChar) || this.isUnaryOperator(currentChar) ||
                    this.isBinaryOperator(currentChar) || currentChar == '[' || currentChar == ',' || currentChar == ']'
                    || currentChar == ';')
            {
                //It can't possibly be part of a double
                if (possDoubStartIndex > -1)
                {
                    possibleDoubles.add(rawInstructions.substring(possDoubStartIndex, x));
                    newInstructions += rawInstructions.substring(0, possDoubStartIndex) + "$";
                    rawInstructions = rawInstructions.substring(x);
                    x = 0;
                    possDoubStartIndex = -1;
                }
                if(currentChar == ';')
                {
                    //At this point, the size of possibleDoubles is equal to the number of user doubles in the first
                    //term.
                    this.firstNextTermUserDoubleIndex = possibleDoubles.size();
                    //Also, the first term is completely processed, although the last character still needs to be added.
                    newFirstTerm = newInstructions + rawInstructions.substring(0, 1);
                }
            }
            else
            {
                if (possDoubStartIndex == -1) {
                    possDoubStartIndex = x;
                }
            }
            ++x;
        }
        if (possDoubStartIndex > -1) {
            //There is one more double left over
            possibleDoubles.add(rawInstructions.substring(possDoubStartIndex));
            newInstructions += "$";
        } else {
            //There are some operators and/or complexes left over
            newInstructions += rawInstructions;
        }
        int noOfPossDoubs = possibleDoubles.size();
        double[] userDoubles = new double[noOfPossDoubs];
        for (x = 0; x < noOfPossDoubs; ++x) {
            String nextString = possibleDoubles.get(x);
            try {
                userDoubles[x] = Double.parseDouble(possibleDoubles.get(x));
            } catch (NumberFormatException nfe) {
                throw new InvalidInstructionsException("'" + nextString + "' is not a valid number.");
            }
        }
        this.userDoubles = userDoubles;
        //Both terms must evaluate to complex numbers.
        instructionParts = newInstructions.split(";");
        if (!isComplex(instructionParts[0])) {
            throw new InvalidInstructionsException("The result of the first term is not a complex number.");
        }
        if (!isComplex(instructionParts[1])) {
            throw new InvalidInstructionsException("The result of the rule for the next term is not a complex number.");
        }
        //The string is definitely valid after this point.
        this.userDoublesIndex = 0;
        if (canKeepConstant)
        {
            //All complex parameters can be null because there are no special complexes in the first term.
            this.constantFirstTerm = this.parseInstruction(newFirstTerm, null, null, null, null);
        }
        this.processedInstructions = newInstructions;
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
        if(stringLength == 1)
        {
            //s is one of the user-defined doubles
            return firstChar == '$';
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
        //Two complexes of any length must have a binary operator between them, so if the string is length 2 then
        //that is invalid
        else if(stringLength > 2)
        {
            //Checks if the first complex is special
            if(isSpecialComplex(s.charAt(0)) && isBinaryOperator(s.charAt(1)) && isComplex(s.substring(2)))
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

    //BASIC GETTERS AND SETTERS

    public void setRawInstructions(String instructions)
    {
        this.rawInstructions = instructions;
    }

    public String getRawInstructions()
    {
        return this.rawInstructions;
    }

    public String getProcessedInstructions()
    {
        return this.processedInstructions;
    }
}