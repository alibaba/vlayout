package android.support.v7.widget;

/**
 * Created by villadora on 15/12/7.
 */
public final class InnerRecycledViewPool {

    private RecyclerView.RecycledViewPool mInnerPool;

    public InnerRecycledViewPool(RecyclerView.RecycledViewPool pool) {
        this.mInnerPool = pool;
    }

    public void putRecycledView(RecyclerView.ViewHolder scrap) {
        mInnerPool.putRecycledView(scrap);
    }

    public RecyclerView.ViewHolder getRecycledView(int viewType) {
        RecyclerView.ViewHolder holder = mInnerPool.getRecycledView(viewType);
        if (holder != null)
            holder.resetInternal();
        return holder;
    }
}
