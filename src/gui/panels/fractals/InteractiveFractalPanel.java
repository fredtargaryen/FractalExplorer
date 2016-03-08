package gui.panels.fractals;

import gui.FractalDisplay;
import numbers.Complex;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * InteractiveFractalPanels can be clicked to select points and dragged to select areas
 * to zoom into.
 */
public class InteractiveFractalPanel extends FractalPanel
{
    private int startx;
    private int starty;
    private int endx;
    private int endy;
    private int endx_old;
    private int endy_old;

    public InteractiveFractalPanel()
    {
        super();
        this.startx = -1;
        this.starty = -1;
        this.endx = -1;
        this.endy = -1;
        this.endx_old = -1;
        this.endy_old = -1;
        this.addMouseListener(new MouseAdapter()
        {
            /**
             * Updates the user selected point being displayed
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                super.mouseClicked(e);
                FractalDisplay.getMainWindow().getTopDisplay().setPoint(InteractiveFractalPanel.this.getPanelCoordsAsComplex(e.getX(), e.getY()));
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseReleased(e);
                InteractiveFractalPanel.this.zoomInWithDragRectCoords();
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter()
        {
            /**
             * Updates the rectangle drawn when a user clicks and drags on this panel.
             */
            @Override
            public void mouseDragged(MouseEvent e)
            {
                super.mouseDragged(e);
                InteractiveFractalPanel.this.setDragRectCoords(e.getX(), e.getY());
            }
        });
    }

    private void setDragRectCoords(int mousex, int mousey) {
        int width = this.getWidth();
        int height = this.getHeight();

        if (this.startx == -1 && this.starty == -1) {
            this.startx = mousex;
            this.starty = mousey;
            this.endx_old = this.startx;
            this.endy_old = this.starty;
        }
        else
        {
            if (mousex < 0) {
                mousex = 0;
            } else if (mousex >= width) {
                mousex = width - 1;
            }
            if (mousey < 0) {
                mousey = 0;
            } else if (mousey >= height) {
                mousey = height - 1;
            }
            this.endx_old = this.endx;
            this.endy_old = this.endy;
            this.endx = mousex;
            this.endy = mousey;
        }
        this.repaint();
    }

    private void zoomInWithDragRectCoords()
    {
        if(!(this.startx == this.endx || this.starty == this.endy))
        {
            int leftx;
            int rightx;
            int topy;
            int bottomy;
            //Transferring from the panel's x-y axis to the complex r-i axis.
            if(this.startx < this.endx)
            {
                leftx = this.startx;
                rightx = this.endx;
            }
            else
            {
                leftx = this.endx;
                rightx = this.startx;
            }
            if(this.starty < this.endy)
            {
                topy = this.starty;
                bottomy = this.endy;
            }
            else
            {
                topy = this.endy;
                bottomy = this.starty;
            }
            //The Complex that would appear towards the top left of the panel (smallest r, largest i)
            Complex topLeft = this.getPanelCoordsAsComplex(leftx, topy);
            //The Complex that would appear towards the bottom right of the panel (largest r, smallest i)
            Complex bottomRight = this.getPanelCoordsAsComplex(rightx, bottomy);
            FractalDisplay.getMainWindow().getParamPanel().setMinMax(topLeft.getRealPart(), bottomRight.getRealPart(), bottomRight.getImagPart(), topLeft.getImagPart());
        }
        this.startx = -1;
        this.starty = -1;
        this.endx = -1;
        this.endy = -1;
        this.endx_old = -1;
        this.endy_old = -1;
    }

    @Override
    public void paintComponent(Graphics g) {
        if (this.startx > -1 && this.starty > -1 && this.endx > -1
                && this.endy > -1 && this.endx_old > -1 && this.endy_old > -1)
        {
            int leftx;
            int rightx;
            int topy;
            int bottomy;
            int leftx_old;
            int rightx_old;
            int topy_old;
            int bottomy_old;
            if (this.startx <= this.endx) {
                leftx = this.startx;
                rightx = this.endx;
            } else {
                leftx = this.endx;
                rightx = this.startx;
            }
            if (this.starty <= this.endy) {
                topy = this.starty;
                bottomy = this.endy;
            } else {
                topy = this.endy;
                bottomy = this.starty;
            }
            if (this.startx <= this.endx_old) {
                leftx_old = this.startx;
                rightx_old = this.endx_old;
            } else {
                leftx_old = this.endx_old;
                rightx_old = this.startx;
            }
            if (this.starty <= this.endy_old) {
                topy_old = this.starty;
                bottomy_old = this.endy_old;
            } else {
                topy_old = this.endy_old;
                bottomy_old = this.starty;
            }
            int width_old = rightx_old - leftx_old + 1;
            int height_old = bottomy_old - topy_old + 1;
            //Paint over the old rectangle. Makes sure to only paint over the edges
            //to minimize recalculations.
            this.paintRect(g, leftx_old,    topy_old,       width_old,  1);
            this.paintRect(g, rightx_old,   topy_old,       1,          height_old);
            this.paintRect(g, leftx_old,    bottomy_old,    width_old,  1);
            this.paintRect(g, leftx_old,    topy_old,       1,          height_old);
            //Draw the new rectangle
            g.setColor(Color.CYAN);
            g.drawRect(leftx, topy, rightx - leftx + 1, bottomy - topy + 1);
        }
        else if (this.startx == -1 && this.endx == -1 && this.starty == -1
                && this.endy == -1 && this.endx_old == -1 && this.endy_old == -1)
        {
            super.paintComponent(g);
        }
    }
}
