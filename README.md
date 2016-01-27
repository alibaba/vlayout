# VirtualLayoutManager

Project `vlayout` is a powerfull LayoutManager extension for RecyclerView, it provides a group of layouts for RecyclerView. Make it able to handle a complicate situation when grid, list and other layouts in the same recyclerview.

## Usage


### Import Library

For gradle:

```
// gradle
compile 'com.alibaba.android:vlayout:1.2.0@aar'
```

Or in maven:

```
// pom.xml 
<dependency>
  <groupId>com.alibaba.android</groupId>
  <artifactId>vlayout</artifactId>
  <version>1.2.0</version>
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

* YOu can use `DelegateAdapter` for as a root adapter to usage
