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


    public static final int SHOW_ALWAYS = 0;
    public static final int SHOW_ON_ENTER = 1;
    public static final int SHOW_ON_LEAVE = 2;


    private int mShowType = SHOW_ALWAYS;

    public ScrollFixLayoutHelper(int x, int y) {
        this(TOP_LEFT, x, y);
    }

    public ScrollFixLayoutHelper(int alignType, int x, int y) {
        super(alignType, x, y);
    }

    public void setShowType(int showType) {
        this.mShowType = showType;
    }


    @Override
    protected boolean shouldBeDraw(int startPosition, int endPosition, int scrolled) {
        switch (mShowType) {
            case SHOW_ON_ENTER:
                // when previous item is entering
                return endPosition >= getRange().getLower() - 1;
            case SHOW_ON_LEAVE:
                // show on leave from top
                // when next item is the first one in screen
                return startPosition >= getRange().getLower() + 1;
            case SHOW_ALWAYS:
            default:
                // default is always
                return true;
        }

    }

}
