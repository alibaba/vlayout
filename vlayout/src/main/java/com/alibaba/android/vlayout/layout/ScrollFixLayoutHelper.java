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


    public ScrollFixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public ScrollFixLayoutHelper(int alignType, int x, int y) {
        super(alignType, x, y);
    }


    @Override
    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        return endPosition >= getRange().getLower() - 1;
    }

}
