# VLayout FAQ

## 组件复用的问题
比如碰到卡顿、类型转换异常等等，都有可能是复用的问题引起的。

在使用 `DelegateAdapter` 的时候，每一个 `LayoutHelper` 都对应于一个 `DelegateAdapter.Adapter`，一般情况下使用方只需要提供自定义的 `DelegateAdapter.Adapter`，然后按照正常的使用方式使用。

但这里有个问题，不同的 `DelegateAdapter.Adapter` 之间，他们的 itemType 是不是一样的？这里有一个选择：在 `DelegateAdapter` 的构造函数里有个 `hasConsistItemType` 参数（默认是 false ）：

当 `hasConsistItemType=false` 的时候，即使不同 `DelegateAdapter.Adapter` 里返回的相同的 itemType，对于 `DelegateAdapter` 也会将它转换成不同的值，对于 `RecyclerView` 来说它们是不同的类型。

当 `hasConsistItemType=true` 的时候，不同的 `DelegateAdapter.Adapter` 之间返回相同的 itemType 的时候，他们之间是可以复用的。

因此如果没有处理好这一点，会导致 `ViewHolder` 的类型转换异常等 bug。有一篇更加详细的资料可参考：[PairFunction](http://pingguohe.net/2017/05/03/the-beauty-of-math-in-vlayout.html)

补充：后来发现一个 bug，当 `hasConsistItemType=true`，在同一位置数据变化，前后构造了不一样的 Adapter，它们返回的 itemType 一样，也会导致类型转换出错，详见：[#182](https://github.com/alibaba/vlayout/issues/182)，目前采用人工保证返回不同的 itemType 来规避。

## 设置每种类型回收复用池的大小
在 README 里写了这么一段 demo：`viewPool.setMaxRecycledViews(0, 10);`，很多人误以为只要这么设置就可以了，实际上有多少种类型的 itemType，就得为它们分别设置复用池大小。比如：

```
viewPool = new RecyclerView.RecycledViewPool();
recyclerView.setRecycledViewPool(viewPool);
viewPool.setMaxRecycledViews(0, 5);
viewPool.setMaxRecycledViews(1, 5);
viewPool.setMaxRecycledViews(2, 5);
viewPool.setMaxRecycledViews(3, 10);
viewPool.setMaxRecycledViews(4, 10);
viewPool.setMaxRecycledViews(5, 10);
...
```
## 下拉刷新和加载更多

VLayout 只负责布局，下拉刷新和加载更多需要业务方自己处理，当然可能存在和一些下拉刷新控件不兼容的 bug。

下拉刷新，有很多框架是通过判断 `RecyclerView` 的第一个 view 的 top 是否为 0 来触发下拉动作。VLayout 里在处理背景、悬浮态的时候加入了一些对 `LayoutManager` 不可见的 View，但又真实存在与 `RecyclerView` 的视图树里，建议使用 `layoutManager.getChildAt(0)` 来获取第一个 view。

加载更多，可以通过 recyclerView 的滚动状态来触发 load-more 事件，需要使用方注册一个 `OnScrollListener`：

```
RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {

            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //hasMore: status of current page, means if there's more data, you have to maintain this status
                if(hasMore) {
                    VirtualLayoutManager lm = (VirtualLayoutManager)recyclerView.getLayoutManager();
                    int first=0, last=0, total=0;
                    first = ((LinearLayoutManager)lm).findFirstVisibleItemPosition();
                    last = ((LinearLayoutManager)lm).findLastVisibleItemPosition();
                    total = recyclerView.getAdapter().getItemCount();
                    if(last > 0
                        && last >= total  - earlyCountForAutoLoad) {
                        //earlyCountForAutoLoad: help to trigger load more listener earlier
                        //TODO trigger loadmore listener
                    }
                }
            }
        }
```

## 横向滑动
没有实现横向滚动的 `LayoutHelper` ，因为 `LayoutHelper` 目前只能做静态的布局，对于跟数据绑定的动态横向滚动布局，比如 `ViewPager` 或者 `RecyclerView` ，建议使用组件的形式提供。也就是一个 `LinearLayoutHelper` 包一个 Item，这个 Item 是 `ViewPager` 或者横向滚动的 `RecyclerView`，且它们是可以和整个页面的 `RecyclerView` 共用一个回收复用池的，参考 Demo 里的第一个组件的使用方法。

## 设置背景图后触发循环布局
给 `LayoutHelper` 设置背景图的时候，由于这个过程是在布局 view 的阶段，设置了图片会触发一次新的 layout，从而又导致触发一次背景图设置，最终进入死循环，因此需要使用方在设置背景图的时候判断当前图片是否已经加载过一次并且成功，如果绑定过一次就不需要再设置图片了，阻断死循环的路径。
具体做法是：
在 `BaseLayoutHelper.LayoutViewBindListener` 的 `onBind()` 方法里判断是否成功绑定过该背景图。
在 `BaseLayoutHelper.LayoutViewUnBindListener` 的 `onUnbind()` 方法里清楚绑定成功与否的状态。
在使用方的图片加载成功回调函数里设置一下图片加载成功的状态，可以自行维护一个 map 或者给 View 设置一个 tag 标记。

我们提供了一个简单的 `DefaultLayoutViewHelper` 封装了这个逻辑，可以参考使用。

## 在可滚动区域里嵌套使用 vlayout 的 `RecyclerView`

不太建议嵌套滚动，除非手势不冲突；如果要完全展开 vlayout 里的内容，牺牲滚动复用，可以调用 `VirtualLayoutManager` 的 `setNoScrolling(true);` 方法设置一下。

## 为 `GridLayoutHelper` 的设置自定义 `SpanSizeLookup`

在 `SpanSizeLookup` 中，`public int getSpanSize(int position)` 方法参数的 position 是整个页面的 position 信息，需要获取当前 layoutHelper 内的相对位置，需要减去一个偏移量，即 `position - getStartPosition()`。

## 获取 `DelegateAdapter` 里数据的相对位置

在 `DelegateAdapter` 里有 `findOffsetPosition(int absolutePosition)` 方法，传入整个页面的绝对位置，获取相对位置。
或者用
```
  public static abstract class Adapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
      public abstract LayoutHelper onCreateLayoutHelper();

      protected void onBindViewHolderWithOffset(VH holder, int position, int offsetTotal) {

      }
  }
```
中的 `onBindViewHolderWithOffset()` 方法代替传统的 `onBindViewHolder()` 方法，其中的 `position` 参数也是相对位置。