package io.github.mkmax.opticview.ui.graph;

import io.github.mkmax.opticview.ui.layout.OrthoComponent;
import io.github.mkmax.opticview.ui.graph.GraphData.*;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Consumer;

public final class GraphView extends GraphStackDevice {

    /* +--------------------------------+ */
    /* | INITIALIZATION & OTHER MEMBERS | */
    /* +--------------------------------+ */
    private final Stack<Canvas> reusestack = new Stack<> ();
    private final Map<Entry, Canvas> entrymap = new HashMap<> ();

    public GraphView (GraphData ref) {
        setGraphData (ref);
        /* install ortho listeners */
        registerHorizontalRemapListener (onRemap);
        registerVerticalRemapListener (onRemap);
        registerWindowRemapListener (onRemap);
    }

    /* +-----------+ */
    /* | LISTENERS | */
    /* +-----------+ */

    /* REMAP LISTENER */
    private final OrthoComponent.RemapListener onRemap = (__comp) ->
        entrymap.forEach ((entry, canvas) -> {
            clearCanvas (canvas);
            fitCanvas (canvas);
            render (entry, canvas);
        });

    /* ENTRY TRANSACTION LISTENERS */
    private final EntryAdditionListener entryaddlistener = (__gd, entry) ->
        recognizeEntry (entry);
    private final EntryRemovalListener entryremovelistener = (__gd, entry) ->
        dismissEntry (entry);

    /* ENTRY PROPERTY LISTENERS */
    private final SpecificEntryPropChangeListener<Function> entryfunclistener = (__gd, entry, __old, __now) ->
        render (entry, true, false);
    private final SpecificEntryPropChangeListener<Color> entrycolorlistener = (__gd, entry, __old, __now) ->
        render (entry, true, false);

    /* +-----------------------+ */
    /* | OVERRIDES & INTERNALS | */
    /* +-----------------------+ */

    /* ENTRY TRANSACTION IMPLEMENTATION */
    private final Consumer<Entry> recognizeEntryDelegate = this::recognizeEntry;

    private void recognizeEntry (Entry entry) {
        final Canvas canvas = allocateCanvas ();
        getChildren ().add (canvas);
        entrymap.put (entry, canvas);
        render (entry, canvas);
    }

    private void dismissEntry (Entry entry) {
        final Canvas canvas = entrymap.get (entry);
        getChildren ().remove (canvas);
        entrymap.remove (entry);
        storeCanvas (canvas);
    }

    private void purgeEntries () {
        for (Canvas c : entrymap.values ()) {
            getChildren ().remove (c);
            storeCanvas (c);
        }
        entrymap.clear ();
    }

    /* CANVAS HANDLING */
    private Canvas allocateCanvas () {
        final Canvas result;
        if (reusestack.size () > 0) {
            result = reusestack.pop ();
            clearCanvas (result);
            fitCanvas (result);
        }
        else
            result = new Canvas (getWidth (), getHeight ());
        return result;
    }

    private void storeCanvas (Canvas target) {
        reusestack.push (target);
    }

    private void fitCanvas (Canvas target) {
        target.setLayoutX (0d);
        target.setLayoutY (0d);
        target.setWidth (getWidth ());
        target.setHeight (getHeight ());
    }

    private void clearCanvas (Canvas target) {
        target.getGraphicsContext2D ().clearRect (
            0d, 0d, target.getWidth (), target.getHeight ());
    }

    /* RENDERING */
    private void render (Entry entry, boolean clear, boolean fit) {
        final Canvas canvas = entrymap.get (entry);
        if (canvas == null) /* should never happen */
            throw new RuntimeException ("Should not be able to render an unregistered entry");
        if (clear) clearCanvas (canvas);
        if (fit) fitCanvas (canvas);
        render (entry, canvas);
    }

    private void render (Entry entry, Canvas target) {
        /* prepare graphics */
        final Function func = entry.getFunction ();
        final Color color = entry.getPrefColorElse (Color.BLACK);
        final GraphicsContext gc = target.getGraphicsContext2D ();
        gc.setStroke (color);

        /* render graph onto canvas (canvas is assumed to be cleared) */
        final double
            width = getWidth (),
            left  = getLeft (),
            right = getRight ();
        final double
            begin = Math.min (left, right),
            end   = Math.max (left, right);
        final double step = (end - begin) / width;
        double
            prev_screen_x = Double.NaN,
            prev_screen_y = Double.NaN;
        for (int i = 0; i < width; ++i) {
            final double virtual_x = begin + step * i;
            final double virtual_y = func.map (virtual_x);
            final double screen_x = mapx (virtual_x);
            final double screen_y = mapy (virtual_y);
            if (i > 0) /* wait until we have two samples to draw the line segments */
                gc.strokeLine (
                    prev_screen_x,
                    prev_screen_y,
                    screen_x,
                    screen_y
                );
            prev_screen_x = screen_x;
            prev_screen_y = screen_y;
        }
    }

    @Override
    public void onGraphDataChanged (GraphData old, GraphData now) {
        if (old != null) {
            old.removeEntryAdditionListener (entryaddlistener);
            old.removeEntryRemovalListener (entryremovelistener);
            old.removeEntryFunctionPropertyChangeListener (entryfunclistener);
            old.removeEntryPrefColorPropertyChangeListener (entrycolorlistener);
            purgeEntries ();
        }
        now.registerEntryAdditionListener (entryaddlistener);
        now.registerEntryRemovalListener (entryremovelistener);
        now.registerEntryFunctionPropertyChangeListener (entryfunclistener);
        now.registerEntryPrefColorPropertyChangeListener (entrycolorlistener);
        now.forEachEntry (recognizeEntryDelegate);
    }

    @Override
    public void dispose () {
        super.dispose ();
        graphData.removeEntryAdditionListener (entryaddlistener);
        graphData.removeEntryRemovalListener (entryremovelistener);
        graphData.removeEntryFunctionPropertyChangeListener (entryfunclistener);
        graphData.removeEntryPrefColorPropertyChangeListener (entrycolorlistener);
        purgeEntries ();
        removeHorizontalRemapListener (onRemap);
        removeVerticalRemapListener (onRemap);
        removeWindowRemapListener (onRemap);
    }
}