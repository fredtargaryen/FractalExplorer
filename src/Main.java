import gui.FractalDisplay;

/**
 * WELCOME TO THE FRACTAL EXPLORER!
 * --Acknowledgements--
 * Thank you to my lecturers for providing the initial information for me to get started, and for choosing such an
 * interesting topic.
 * Thanks to Bowie, D. and Dury, I. et al. for getting me through the development.
 *
 * --Extra Features--
 * I have attempted to add:
 * -Options for inverting the real and imaginary axes
 * -The option to select other fractals for display and interaction (Burning Ship and Buffalo)
 * -A parser for formulae entered by the user
 * -Three different shapes of orbit trap
 *
 * --Operational Notes--
 * Thanks to the parser, the iteration procedure has slowed right down. I've tried to optimise it but it still takes a
 * worrying amount of time (especially on larger fractals like the Buffalo).
 * The window title changes to include "Drawing - please wait" while drawing - please keep waiting; it should get there
 * eventually! It can render Z(i+1) = Z(i)^100+c, at 50 iterations with no traps, in just under 3 minutes.
 */

/**
 * TODO Not painting over old selection rectangle before painting new one.
 * TODO Double painting at beginning?
 *
 * TODO Slow fractal generation
 * TODO - as unary operator as well
 */
public class Main
{
    public static void main(String[] args)
    {
        FractalDisplay fd = new FractalDisplay();
    }
}
