package numbers;

/**
 * Models a complex number.
 */
public class Complex
{
    private double realPart;
    private double imagPart;

    public Complex(double real, double imag)
    {
        this.realPart = real;
        this.imagPart = imag;
    }

    public Complex square()
    {
        return this.multiplyBy(this);
    }

    /**
     * Multiplies this complex number by multiplicand. Returns itself to enable method chaining.
     */
    public Complex multiplyBy(Complex multiplicand)
    {
        double newRealPart = (this.realPart * multiplicand.realPart) - (this.imagPart * multiplicand.imagPart);
        double newImagPart = (this.realPart * multiplicand.imagPart) + (this.imagPart * multiplicand.realPart);
        this.realPart = newRealPart;
        this.imagPart = newImagPart;
        return this;
    }

    /**
     * Gives the squared modulus of this complex number.
     */
    public double modulusSquared()
    {
        return this.realPart * this.realPart + this.imagPart * this.imagPart;
    }

    /**
     * Adds another complex number to this one. Returns itself to enable method chaining.
     */
    public Complex add(Complex d)
    {
        this.realPart += d.realPart;
        this.imagPart += d.imagPart;
        return this;
    }

    /**
     * Displays this number in the conventional format.
     */
    public String toString()
    {
        String displayImaginaryPart=this.imagPart + "i";
        if(this.imagPart >= 0)
        {
            displayImaginaryPart = "+" + displayImaginaryPart;
        }
        return this.realPart + displayImaginaryPart;
    }

    public Complex clone()
    {
        return new Complex(this.realPart, this.imagPart);
    }

    //BASIC GETTERS AND SETTERS BELOW HERE

    public double getRealPart()
    {
        return this.realPart;
    }

    public double getImagPart()
    {
        return this.imagPart;
    }
}
