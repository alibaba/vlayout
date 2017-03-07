# vlayout

[English Document](README.md)

VirtualLayout是一个针对RecyclerView的LayoutManager扩展, 主要提供一整套布局方案和布局间的组件复用的问题。

## 设计思路

通过定制化的LayoutManager，接管整个RecyclerView的布局逻辑；LayoutManager管理了一系列LayoutHelper，LayoutHelper负责具体布局逻辑实现的地方；每一个LayoutHelper负责页面某一个范围内的组件布局；不同的LayoutHelper可以做不同的布局逻辑，因此可以在一个RecyclerView页面里提供异构的布局结构，这就能比系统自带的LinearLayoutManager、GridLayoutManager等提供更加丰富的能力。同时支持扩展LayoutHelper来提供更多的布局能力。

## 主要功能

 * 默认通用布局实现，解耦所有的View和布局之间的关系: Linear, Grid, 吸顶, 浮动, 固定位置等。
	* LinearLayoutHelper: 线性布局
	* GridLayoutHelper:  Grid布局， 支持横向的colspan
	* FixLayoutHelper: 固定布局，始终在屏幕固定位置显示
	* ScrollFixLayoutHelper: 固定布局，但之后当页面滑动到该图片区域才显示, 可以用来做返回顶部或其他书签等
	* FloatLayoutHelper: 浮动布局，可以固定显示在屏幕上，但用户可以拖拽其位置
	* ColumnLayoutHelper: 栏格布局，都布局在一排，可以配置不同列之间的宽度比值
	* SingleLayoutHelper: 通栏布局，只会显示一个组件View
	* OnePlusNLayoutHelper: 一拖N布局，可以配置1-5个子元素
	* StickyLayoutHelper: stikcy布局， 可以配置吸顶或者吸底
	* StaggeredGridLayoutHelper: 瀑布流布局，可配置间隔高度/宽度
 * 上述默认实现里可以大致分为两类：一是非fix类型布局，像线性、Grid、栏格等，它们的特点是布局在整个页面流里，随页面滚动而滚动；另一类就是fix类型的布局，它们的子节点往往不随页面滚动而滚动。
 * 所有除布局外的组件复用，VirtualLayout将用来管理大的模块布局组合，扩展了RecyclerView，使得同一RecyclerView内的组件可以复用，减少View的创建和销毁过程。


## 使用

版本请参考mvn repository上的最新版本（目前最新版本是1.0.1），最新的 aar 都会发布到 jcenter 和 MavenCentral 上，确保配置了这两个仓库源，然后引入aar依赖：

```
// gradle
compile ('com.alibaba.android:vlayout:1.0.1@aar') {
	transitive = true
}
```

或者maven

```
// pom.xml in maven
<dependency>
  <groupId>com.alibaba.android</groupId>
  <artifactId>vlayout</artifactId>
  <version>1.0.1</version>
  <type>aar</type>
</dependency>
```


初始化```LayoutManager```

```
final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);

recyclerView.setLayoutManager(layoutManager);
```

设置回收复用池大小，（如果一屏内相同类型的 View 个数比较多，需要设置一个合适的大小，防止来回滚动时重新创建 View）：

```
RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
recyclerView.setRecycledViewPool(viewPool);
viewPool.setMaxRecycledViews(0, 10);
```

加载数据时有两种方式:

* 一种是使用 ```DelegateAdapter```, 可以像平常一样写继承自```DelegateAdapter.Adapter```的Adapter, 只比之前的Adapter需要多重载```onCreateLayoutHelper```方法。
其他的和默认Adapter一样。

```
DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager, hasStableItemType);
recycler.setAdapter(delegateAdapter);

// 之后可以通过 setAdapters 或 addAdapter方法添加DelegateAdapter.Adapter

delegateAdapter.setAdapters(adapters);

// or
CustomAdapter adapter = new CustomAdapter(data, new GridLayoutHelper());
delegateAdapter.addAdapter(adapter);

```

* 另一种是当业务有自定义的复杂需求的时候, 可以继承自```VirtualLayoutAdapter```, 实现自己的Adapter

```
public class MyAdapter extends VirtualLayoutAdapter {
   ......
}

MyAdapter myAdapter = new MyAdapter(layoutManager);

//构造 layoutHelper 列表
List<LayoutHelper> helpers = new LinkedList<>();
GridLayoutHelper gridLayoutHelper = new GridLayoutHelper(4);
gridLayoutHelper.setItemCount(25);
helpers.add(gridLayoutHelper);

GridLayoutHelper gridLayoutHelper2 = new GridLayoutHelper(2);
gridLayoutHelper2.setItemCount(25);
helpers.add(gridLayoutHelper2);

//将 layoutHelper 列表传递给 adapter
myAdapter.setLayoutHelpers(helpers);

//将 adapter 设置给 recyclerView
recycler.setAdapter(myAdapter);

```

在这种情况下，需要使用者注意在当```LayoutHelpers```的结构或者数据数量等会影响到布局的元素变化时，需要主动调用```setLayoutHepers```去更新布局模式。



# Demo

![](http://img3.tbcdn.cn/L1/461/1/1b9bfb42009047f75cee08ae741505de2c74ac0a)

[Demo工程](https://github.com/alibaba/vlayout/tree/master/examples)

# 布局属性

每一种layoutHelper都有自己的布局属性来控制布局样式，详情请参考[文档](docs/ATTRIBUTES-ch.md)。

# 开源许可证

vlayout遵循MIT开源许可证协议。