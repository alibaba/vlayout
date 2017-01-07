# vlayout

[中文文档](README-ch.md)

Project `vlayout` is a powerfull LayoutManager extension for RecyclerView, it provides a group of layouts for RecyclerView. Make it able to handle a complicate situation when grid, list and other layouts in the same recyclerview.

## Design

By providing a custom LayoutManager to RecyclerView, VirtualLayout is able to layout child views with different style at single view elegantly. The custom LayoutManager manages a serial of layoutHelpers where each one implements the specific layout logic for a certain position range items. By the way, implementing your custom layoutHelper and provding it to the framework is also supported.

## Main Feature
* Provide default common layout implements, decouple the View and Layout. Default layout implements are:
	* LinearLayoutHelper: provide linear layout as LinearLayoutManager.
	* GridLayoutHelper: privide grid layout as GridLayoutManager, but with more feature.
	* FixLayoutHelper: fix the view at certain position of screen, the view does not scroll with whole page.
	* ScrollFixLayoutHelper: fix the view at certain position of screen, but the view does not show until it scrolls to it position.
	* FloatLayoutHelper: float the view on top of page, user can drag and drop it.
	* ColumnLayoutHelper: perform like GridLayoutHelper but layouts all child views in one line.
	* SingleLayoutHelper: contain only one child view.
	* OnePlusNLayoutHelper: a custom layout with one child view layouted at left and the others at right, you may not need this.
	* StickyLayoutHelper: scroll the view when its position is inside the screen, but fix the view at start or end when its position is outside the screen.
	* StaggeredGridLayoutHelper: provide waterfall like layout as StaggeredGridLayoutManager.
* LayoutHelpers provided by default can be generally divided into two categories. One is non-fix LayoutHelper such as LinearLayoutHelper, GridLayoutHelper, etc which means the children of these LayoutHelper will be layouted in the flow of parent container and will be scrolled with the container scrolling. While the other is fix LayoutHelper which means the child of these is always fix in parent container.


## Usage


### Import Library

Please find the latest version in maven repository.

For gradle:

```
// gradle
compile 'com.alibaba.android:vlayout:1.0.0@aar'
```

Or in maven:

```
// pom.xml 
<dependency>
  <groupId>com.alibaba.android</groupId>
  <artifactId>vlayout</artifactId>
  <version>1.0.0</version>
  <type>aar</type>
</dependency>
```

### Initialize LayoutManager

```java
final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
final VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);

recyclerView.setLayoutManager(layoutManager);
```

### Set Adapters

* You can use `DelegateAdapter` for as a root adapter to make combination of your own adapters. Just make it extend ```DelegateAdapter.Adapter``` and overrides ```onCreateLayoutHelper``` method.


```java
DelegateAdapter delegateAdapter = new DelegateAdapter(layoutManager, hasStableItemType);
recycler.setAdapter(delegateAdapter);

// Then you can set sub- adapters

delegateAdapter.setAdapters(adapters);

// or
CustomAdapter adapter = new CustomAdapter(data, new GridLayoutHelper());
delegateAdapter.addAdapter(adapter);

```

* The other way to set adapter is extending ```VirtualLayoutAdapter``` and implementing it to make deep combination to your business code.

```java
public class MyAdapter extends VirtualLayoutAdapter {
   ....
}

```

In this way, one thing you should note is that you should call ```setLayoutHelpers``` when the data of Adapter changes.

# Demo

[Demo Project]()

# Layout Attributes

Each layoutHelper has a few attributes to control its layout style. See [this](docs/ATTRIBUTES.md) to read more.