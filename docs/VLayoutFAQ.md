# VLayout FAQ

## 组件复用的问题
比如碰到卡顿、类型转换异常等等，都有可能是复用的问题引起的。

在使用DelegateAdapter的时候，每一个LayoutHelper都对应于一个DelegateAdapter.Adapter，一般情况下使用方只需要提供自定义的DelegateAdapter.Adapter，然后按照正常的使用方式使用。

但这里有个问题，不同的DelegateAdapter.Adapter之间，他们的itemType是不是一样的？这里有一个选择：在DelegateAdapter的构造函数里有个hasConsistItemType参数（默认是false）：

当hasConsistItemType=false的时候，即使不同DelegateAdapter.Adapter里返回的相同的itemType，对于DelegateAdapter也会将它转换成不同的值，对于RecyclerView来说它们是不同的类型。

当hasConsistItemType=true的时候，不同的DelegateAdapter.Adapter之间返回相同的itemType的时候，他们之间是可以复用的。

因此如果没有处理好这一点，会导致ViewHolder的类型转换异常等bug。有一篇更加详细的资料可参考：[PairFunction](http://pingguohe.net/2017/05/03/the-beauty-of-math-in-vlayout.html)

## 下拉刷新和加载更多

VLayout只负责布局，下拉刷新和加载更多需要业务方自己处理，当然可能存在和一些下拉刷新控件不兼容的bug。

下拉刷新，有很多框架是通过判断RecyclerView的第一个view的top是否为0来触发下拉动作。VLayout里在处理背景、悬浮态的时候加入了一些对LayoutManager不可见的View，但又真实存在与RecyclerView的视图树里，建议使用`layoutManager.getChildAt(0)`来获取第一个view。

加载更多，可以通过recyclerView的滚动状态来触发load-more事件，需要使用方注册一个OnScrollListener：

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
没有实现横向滚动的LayoutHelper，因为LayoutHelper目前只能做静态的布局，对于跟数据绑定的动态横向滚动布局，比如ViewPager或者RecyclerView，建议使用组件的形式提供。也就是一个LinearLayoutHelper包一个Item，这个Item是ViewPager或者横向滚动的RecyclerView，且它们是可以和整个页面的RecyclerView共用一个回收复用池的，参考Demo里的第一个组件的使用方法。

## 设置背景图后触发循环布局
给LayoutHelper设置背景图的时候，由于这个过程是在布局view的阶段，设置了图片会触发一次新的layout，从而又导致触发一次背景图设置，最终进入死循环，因此需要使用方在设置背景图的时候判断当前图片是否已经加载过一次并且成功，如果绑定过一次就不需要再设置图片了，阻断死循环的路径。
具体做法是：
在`BaseLayoutHelper.LayoutViewBindListener`的`onBind()`方法里判断是否成功绑定过该背景图。
在`BaseLayoutHelper.LayoutViewUnBindListener`的`onUnbind()`方法里清楚绑定成功与否的状态。
在使用方的图片加载成功回调函数里设置一下图片加载成功的状态，可以自行维护一个map或者给View设置一个tag标记。

我们提供了一个简单的`DefaultLayoutViewHelper`封装了这个逻辑，可以参考使用。
