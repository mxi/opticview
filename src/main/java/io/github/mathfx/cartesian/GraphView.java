package io.github.mathfx.cartesian;

import io.github.mathfx.util.Disposable;
import io.github.mathfx.util.ObservableGroup;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.StackPane;

public final class GraphView extends StackPane implements Disposable {

    private final GuideContainer guideContainer = new GuideContainer ();

    private final DoubleProperty left   = new SimpleDoubleProperty (-1d);
    private final DoubleProperty right  = new SimpleDoubleProperty ( 1d);
    private final DoubleProperty bottom = new SimpleDoubleProperty (-1d);
    private final DoubleProperty top    = new SimpleDoubleProperty ( 1d);

    private final ObservableGroup<Number> orthoGroup      = new ObservableGroup<> (left, right, bottom, top);
    private final ObservableGroup<Number> dimensionsGroup = new ObservableGroup<> (widthProperty (), heightProperty ());
    private final ObservableGroup<Number> projectionGroup = new ObservableGroup<> (orthoGroup, dimensionsGroup);

    private double Mx = 1d;
    private double Kx = 0d;

    private double My = 1d;
    private double Ky = 0d;

    {
        getChildren ().addAll (guideContainer);

        projectionGroup.add (this::reproject);
    }

    /* +-----------------+ */
    /* | GETTERS/SETTERS | */
    /* +-----------------+ */

    /* LEFT */
    public double getLeft () {
        return left.get ();
    }

    public void setLeft (double nLeft) {
        left.set (nLeft);
    }

    /* RIGHT */
    public double getRight () {
        return right.get ();
    }

    public void setRight (double nRight) {
        right.set (nRight);
    }

    /* BOTTOM */
    public double getBottom () {
        return bottom.get ();
    }

    public void setBottom (double nBottom) {
        bottom.set (nBottom);
    }

    /* TOP */
    public double getTop () {
        return top.get ();
    }

    public void setTop (double nTop) {
        top.set (nTop);
    }

    /* +---------+ */
    /* | UTILITY | */
    /* +---------+ */

    /**
     * Maps a window space X coordinate to component or viewport
     * space.
     * <p>
     * Viewport space refers to the set of coordinates for which
     * all X are in [0, getWidth()] and all Y are in [0, getHeight()].
     * Window space refers to the set of coordinates for which
     * all X are in [left, right] and all Y are in [bottom, top].
     *
     * @param p The window space X coordinate.
     * @return The viewport space X coordinate.
     */
    public double projectX (double p) {
        return Mx * p + Kx;
    }

    /**
     * Maps a window space Y coordinate to component or viewport
     * space.
     * <p>
     * Viewport space refers to the set of coordinates for which
     * all X are in [0, getWidth()] and all Y are in [0, getHeight()].
     * Window space refers to the set of coordinates for which
     * all X are in [left, right] and all Y are in [bottom, top].
     *
     * @param p The window space Y coordinate.
     * @return The viewport space Y coordinate.
     */
    public double projectY (double p) {
        return My * p + Ky;
    }

    /**
     * Maps a component or viewport space X coordinate back to
     * window space.
     * <p>
     * Viewport space refers to the set of coordinates for which
     * all X are in [0, getWidth()] and all Y are in [0, getHeight()].
     * Window space refers to the set of coordinates for which
     * all X are in [left, right] and all Y are in [bottom, top].
     *
     * @param q The viewport space X coordinate.
     * @return The window space X coordinate.
     */
    public double unprojectX (double q) {
        return (q - Kx) / Mx;
    }

    /**
     * Maps a component or viewport space Y coordinate back to
     * window space.
     * <p>
     * Viewport space refers to the set of coordinates for which
     * all X are in [0, getWidth()] and all Y are in [0, getHeight()].
     * Window space refers to the set of coordinates for which
     * all X are in [left, right] and all Y are in [bottom, top].
     *
     * @param q The viewport space Y coordinate.
     * @return The window space Y coordinate.
     */
    public double unprojectY (double q) {
        return (q - Ky) / My;
    }

    @Override
    public void dispose () {
        /* I opt to dispose each group individually instead of invoking
         * projectionGroup.dispose(true) to make the code more clear of
         * its purpose.
         */
        orthoGroup.dispose ();
        dimensionsGroup.dispose ();
        projectionGroup.dispose ();
    }

    public void reproject () {
        final double width  = getWidth ();
        final double height = getHeight ();

        final double left   = getLeft ();
        final double right  = getRight ();
        final double bottom = getBottom ();
        final double top    = getTop ();

        Mx = width / (right - left);
        Kx = -Mx * left;

        My = height / (bottom - top);
        Ky = -My * top;
    }
}
