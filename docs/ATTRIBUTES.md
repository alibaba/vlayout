Vlayout provides rich layout features, which are presented in the form of a serial of layout attributes technically. Here we introduce these attributes.

[中文文档](ATTRIBUTES-ch.md)

# Margin, Padding

Margin, Padding have the similar concepts as normal Android system's margin and padding. There are several difference you should know:

+ Here we say Margin and Padding, specificly mean LayoutHelper's Margin and Padding, not the RecyclerView's.
+ A RecyclerView could contains more than one LayoutHelpers, which means each LayoutHelper is able to host its own margin and padding value.
+ LayoutHelper's margin and padding can coexist with RecylerView's margin and padding.

![TODO]()

### API

For LayoutHelper，call

```public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding)```
```public void setMargin(int leftMargin, int topMargin, int rightMargin, int bottomMargin)```

# bgColor, bgImg

Actually, background color or background image has nothing to do with layout logic. But in our framework, all child views has the same parent which is RecyclerView. If you want to fill background to a certain area with a color or image, it's a hard job. Vlayout has done some effort to make it possible for non-fix or non-float LayoutHelper, so it's introduced here.

### API

Fill background with color, call

```public void setBgColor(int bgColor)```

Fill background with image,

First, provide a simple factory to produce ImageView to LayoutManager,

```
this.mLayoutManager.setLayoutViewFactory(new LayoutViewFactory() {
            @Override
            public View generateLayoutView(@NonNull Context context) {
                return new XXImageView(context);
            }
        });
```

Second, set image load listeners to LayoutHelper,

```
baseHelper.setLayoutViewBindListener(new BindListener(imgUrl));
baseHelper.setLayoutViewUnBindListener(new UnbindListener(imgUrl));


private static class BindListener implements BaseLayoutHelper.LayoutViewBindListener {
        private String imgUrl;

        public BindListener(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        @Override
        public void onBind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
            //loading image
        }
    }

    private static class UnbindListener implements BaseLayoutHelper.LayoutViewUnBindListener {
        private String imgUrl;

        public UnbindListener(String imgUrl) {
            this. imgUrl = imgUrl;
        }

        @Override
        public void onUnbind(View layoutView, BaseLayoutHelper baseLayoutHelper) {
        		//cancel loading image
        }
    }
```

# AspectRatio

In order to uniform child view's height during layout, vlayout introduces AspectRatio. It generally means width/height ratio, but has slight difference in different situation. Both LayoutHelper and LayoutParams of the view inside VirtualLayout have this attribute, while the latter has a higher priority than the former.

+ LayoutHelper定义的AspectRatio，指的是一行View整体的宽度与高度之比，当然整体的宽度是减去了RecyclerView和对应的LayoutHelper的Margin, Padding。
+ View的LayoutParams定义的AspectRatio，指的是在LayoutHelper计算出View的宽度之后，用来确定View的高度时使用的，它会覆盖通过LayoutHelper的AspectRatio计算出来的View的高度，因此具备更高优先级。

![TODO]()

### 接口

对于LayoutHelper，调用

```public void setAspectRatio(float aspectRatio)```

对于View，调用

```((VirutalLayoutManager.LayoutParams) layoutParams).mAspectRatio```

# dividerHeight

LinearLayoutHelper的属性，LinearLayoutHelper是像ListView一样的线性布局，dividerHeight就是每个item之间的间距。

![TODO]()

### 接口

对于LinearLayoutHelper，调用

```public void setDividerHeight(int dividerHeight)```

# weights

ColumnLayoutHelper, GridLayoutHelper的属性，它们都是提供网格状的布局能力，**建议使用GridLayoutHelper**，它的能力更加强大，参考下文介绍。默认情况下，每个网格中每一列的宽度是一样的，通过weights属性，可以指定让每一列的宽度成比例分配，就行LinearLayout的weight属性一样。
weights属性是一个float数组，每一项代表某一列占父容器宽度的百分比，总和建议是100，否则布局会超出容器宽度；如果布局中有4列，那么weights的长度也应该是4；长度大于4，多出的部分不参与宽度计算；如果小于4，不足的部分默认平分剩余的空间。

![TODO]()

### 接口

对于ColumnLayoutHelper, GridLayoutHelper，调用

```public void setWeights(float[] weights)```

# vGap, hGap

GridLayoutHelper与StaggeredGridLayoutHelper都有这两个属性，分别控制View之间的垂直间距和水平间距。

![TODO]()

### 接口

对于GridLayoutHelper, StaggeredGridLayoutHelper ，调用

```public void setHGap(int hGap)```
```public void setVGap(int vGap)```

# spanCount, spanSizeLookup

参考于系统的GridLayoutManager，spanCount表示网格的列数，默认情况下每一个View都占用一个网格区域，但通过提供自定义的spanSizeLookUp，可以指定某个位置的View占用多个网格区域。

![TODO]()

### 接口

使用spanCount调用

```public void setSpanCount(int spanCount)```

使用spanSizeLookup

```public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup)```


# lane

StaggeredGridLayoutHelper中有这个属性，与GridLayoutHelper里的spanCount类似，控制瀑布流的列数。

### 接口

调用

```public void setLane(int lane)```

# fixAreaAdjuster

fix类型的LayoutHelper，在可能需要设置一个相对父容器四个边的偏移量，比如整个页面里有一个固定的标题栏添加在vlayout容器上，vlayout内部的fix类型View不希望与外部的标题有所重叠，那么就可以设置一个fixAreaAdjuster来做偏移。

![TODO]()

### 接口

调用

```public void setAdjuster(FixAreaAdjuster adjuster)```

# alignType, x, y

FixLayoutHelper, ScrollFixLayoutHelper, FloatLayoutHelper的属性，表示吸边时的基准位置，有四个取值，分别是TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT。x和y是相对这四个位置的偏移量，最终的偏移量还要受上述的fixAreaAdjuster影响。

+ TOP_LEFT：基准位置是左上角，x是View左边相对父容器的左边距偏移量，y是View顶边相对父容器的上边距偏移量；
+ TOP_RIGHT：基准位置是右上角，x是View右边相对父容器的右边距偏移量，y是View顶边相对父容器的上边距偏移量；
+ BOTTOM_LEFT：基准位置是左下角，x是View左边相对父容器的左边距偏移量，y是View底边相对父容器的上边距偏移量；
+ BOTTOM_RIGHT：基准位置是右下角，x是View右边相对父容器的右边距偏移量，y是View底边相对父容器的下边距偏移量；

![TODO]()

### 接口

设置基准调用

```public void setAlignType(int alignType)```

设置偏移量调用
```public void setX(int x)```
```public void setY(int y)```

# showType

ScrollFixLayoutHelper的属性，取值有SHOW_ALWAYS, SHOW_ON_ENTER, SHOW_ON_LEAVE。

+ SHOW_ALWAYS：与FixLayoutHelper的行为一致，固定在某个位置；
+ SHOW_ON_ENTER：默认不显示View，当页面滚动到这个View的位置的时候，才显示；
+ SHOW_ON_LEAVE：默认不显示View，当页面滚出这个View的位置的时候显示；

![TODO]()

调用

```public void setShowType(int showType)```

# stickyStart, offset

StickyLayoutHelper的属性，当View的位置在屏幕范围内时，View会随页面滚动而滚动；当View的位置滑出屏幕时，StickyLayoutHelper会将View固定在顶部（stickyStart=true）或者底部（stickyStart=false），固定的位置支持设置偏移量offset。

![TODO]()

调用

```public void setStickyStart(boolean stickyStart)```
```public void setOffset(int offset)```