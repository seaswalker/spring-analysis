# 创建

以CacheLoader的方式为例:

```java
LoadingCache<String, String> cache = CacheBuilder.newBuilder().maximumSize(2)
	.build(new CacheLoader<String, String>() {
		@Override
         public String load(String s) throws Exception {
         	return "Hello: " + s;
		}
	});
```

创建的关键便在于build方法,build方法的核心逻辑位于LocalCache构造器，构造器完成了两件事:

- 将设置的属性从CacheBuilder复制到LocalCache。
- 构造缓存存储的数据结构，此数据结构可以理解为一个自己实现的ConcurrentHashMap(分段锁)。

数据结构的示意图:

![guava-cache](images/guava-cache.jpg)

## Segment

Segment代表了其中的一段。其类图(部分):

![Segment类图](images/Segment.jpg)

此类继承ReentrantLock的目的在于方便的进行加锁操作。

那么Segment的个数是如何确定的呢?

**取最小的大于等于目的并行度的2的整次幂，如果设置了按权重大小的淘汰策略，那么还应注意总的权重值不超过给定的上限，每个Segment的权重按20计**。

相关源码:

```java
LocalCache(
      CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
	concurrencyLevel = Math.min(builder.getConcurrencyLevel(), MAX_SEGMENTS);
    int segmentCount = 1;
    while (segmentCount < concurrencyLevel && (!evictsBySize() || segmentCount * 20 <= maxWeight)) {
      ++segmentShift;
      segmentCount <<= 1;
    }
}
```

并行度即并发修改缓存值的线程数，可以通过CacheBuilder的concurrencyLevel方法进行设置，默认4.

## ReferenceEntry

ReferenceEntry是guava-cache中实际进行存储的数据结构，其类图:

![ReferenceEntry类图](images/ReferenceEntry.jpg)

那么在初始状态下，每个Segment中有多少个ReferenceEntry呢?

**取最小的大于等于(initialCapacity / segmentCount)的2的整次幂的值**。关键代码:

```java
LocalCache(
      CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
  	int segmentCapacity = initialCapacity / segmentCount;
    if (segmentCapacity * segmentCount < initialCapacity) {
      ++segmentCapacity;
    }
	int segmentSize = 1;
	while (segmentSize < segmentCapacity) {
		segmentSize <<= 1;
	}
}
```

initialCapacity由CacheBuilder的同名方法进行设置，默认16.

## Segment初始化

关键代码:

```java
LocalCache(
      CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
	if (evictsBySize()) {
		// Ensure sum of segment max weights = overall max weights
		long maxSegmentWeight = maxWeight / segmentCount + 1;
		long remainder = maxWeight % segmentCount;
		for (int i = 0; i < this.segments.length; ++i) {
			if (i == remainder) {
				maxSegmentWeight--;
			}
        	this.segments[i] =
            	createSegment(segmentSize, maxSegmentWeight, builder.getStatsCounterSupplier().get());
      	}
    } else {
		for (int i = 0; i < this.segments.length; ++i) {
         this.segments[i] =
            createSegment(segmentSize, UNSET_INT, builder.getStatsCounterSupplier().get());
		}
	}
}
```

可以看出，初始化根据是否启用了权重大小限制分为了两种情况，两种情况的区别在于maxSegmentWeight参数，用以指定此Segment的权重上限。

- [ ] LocalCache 引用类型

