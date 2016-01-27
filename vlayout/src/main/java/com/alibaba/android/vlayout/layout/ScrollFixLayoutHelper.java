package com.alibaba.android.vlayout.layout;

/**
 * Absolute layout which only shows after scrolling to its' position,
 * it'll layout View based on leftMargin/topMargin/rightMargin/bottomMargin.
 *
 * @author villadora
 * @since 1.0.0
 */
public class ScrollFixLayoutHelper extends FixLayoutHelper {

    private static final String TAG = "ScrollFixLayoutHelper";


    private boolean mShowOnEnter = true;

    public ScrollFixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public ScrollFixLayoutHelper(int alignType, int x, int y) {
        super(alignType, x, y);
    }

    public void setShowOnEnter(boolean showOnEnter) {
        this.mShowOnEnter = showOnEnter;
    }


    @Override
    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        if (mShowOnEnter)
            return endPosition >= getRange().getLower() - 1;
        else {
            // show on leave from top
            return startPosition >= getRange().getLower() - 1;
        }
    }

}
