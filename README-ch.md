# VirtualLayout

VirtualLayout是一个针对RecyclerView的LayoutManager扩展, 主要通过提供一整套布局方案和布局间的组件复用的问题。

## 主要提供:

 * 默认通用布局实现，解耦所有的View和布局之间的关系: Linear, Grid, 吸顶, 浮动, 固定位置等.
 * 所有除布局外的组件复用，VirtualLayout将用来管理大的模块布局组合, 扩展了RecyclerView，使得同一RecyclerView内的组件可以复用，减少View的创建和销毁过程.


## 使用

引入aar依赖:

```
// gradle
compile 'com.tmall.android:vlayout:1.2.0@aar'
```

或者maven

```
// pom.xml in maven
<dependency>
  <groupId>com.tmall.android</groupId>
  <artifactId>vlayout</artifactId>
  <version>1.2.0</version>
  <type>aar</type>
</dependency>
```


初始化```LayoutManager```

```java
final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);

recyclerView.setLayoutManager(layoutManager);
```


加载数据时有两种方式:

* 一种是使用 ```DelegateAdapter```, 可以想平常一样写继承自```DelegateAdapter.Adapter```的Adapter, 只比之前的Adapter需要多重载```onCreateLayoutHelper```方法。
其他的和默认Adapter一样。

```java
DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager, hasStableItemType);
recycler.setAdapter(delegateAdapter);

// 之后可以通过 setAdapters 或 addAdapter方法添加Adapter

delegateAdapter.setAdapters(adapters);

// or
CustomAdapter adapter = new CustomAdapter(data, new GridLayoutHelper());
delegateAdapter.addAdapter(adapter);

```

* 另一种是当业务有自定义的复杂需求的时候, 可以继承自```VirtualLayoutAdapter```, 实现自己的Adapter

```java
public class MyAdapter extends VirtualLayoutManager {
   ....
}

```

在这种情况下，需要使用者注意在当```LayoutHelpers```的结构或者数据数量等会影响到布局的元素变化时，需要主动调用```setLayoutHepers```去更新布局模式。


推荐使用第一种方式，简单方便，开发者也很熟悉。


### 目前支持的布局类型

* LinearLayoutHelper: 线性布局
* GridLayoutHelper:  Grid布局， 支持横向的colspan
* FixLayoutHelper: 固定布局，始终在屏幕固定位置显示
* ScrollFixLayoutHelper: 固定布局，但之后当页面滑动到该图片区域才显示, 可以用来做返回顶部或其他书签等
* FloatLayoutHelper: 浮动布局，可以固定显示在屏幕上，但用户可以拖拽其位置
* ColumnLayoutHelper: 栏格布局，都布局在一排，可以配置不同列之间的宽度比值
* SingleLayoutHelper: 通栏布局，只会显示一个组件View
* OnePlusNLayoutHelper: 一拖N布局，可以配置1-5个子元素
* StickyLayoutHelper: stikcy布局， 可以配置吸顶或者吸底
* 等待更多的布局支持

#### 目前有的布局还不支持横向滑动，欢迎补充贡献。

### TODOs

* 更多布局支持，瀑布流等
* 横向滑动＋回收
* 嵌套滑动和动画
* Horizontal Layout，目前只支持纵向布局


