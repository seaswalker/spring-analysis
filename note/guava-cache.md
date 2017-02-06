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

## 数据结构

###  segments

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

### ReferenceEntry

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

## 初始化

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

createSegment其实就是对Segment构造器的调用，此构造器主要做了两件事:

- 初始化ReferenceEntry数组数据结构。
- 初始化引用队列。

下面分开对其进行说明。

###  ReferenceEntry数组

关键代码:

```java
Segment(LocalCache<K, V> map, int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
	 initTable(newEntryArray(initialCapacity));
}
```

newEntryArray方法只是创建了一个initialCapacity大小的数组，关键在于initTable:

```java
void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
  this.threshold = newTable.length() * 3 / 4; // 0.75
  if (!map.customWeigher() && this.threshold == maxSegmentWeight) {
	// prevent spurious expansion before eviction
	this.threshold++;
  }
  this.table = newTable;
}
```

这里完成的是对临界值的设置，超过此值数据将进行扩张。

### 引用队列

关键代码:

```java
Segment(LocalCache<K, V> map, int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
  	//当不是强引用的时候成立
	keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue<K>() : null;
	valueReferenceQueue = map.usesValueReferences() ? new ReferenceQueue<V>() : null;
	recencyQueue =
  		map.usesAccessQueue()
	  	? new ConcurrentLinkedQueue<ReferenceEntry<K, V>>()
	  	: LocalCache.<ReferenceEntry<K, V>>discardingQueue();
	writeQueue =
  		map.usesWriteQueue()
	  	? new WriteQueue<K, V>()
	  	: LocalCache.<ReferenceEntry<K, V>>discardingQueue();
	accessQueue =
  		map.usesAccessQueue()
	  	? new AccessQueue<K, V>()
	  	: LocalCache.<ReferenceEntry<K, V>>discardingQueue();
}
```

keyReferenceQueue和valueReferenceQueue用于结合软引用、弱引用以及虚引用使用，关于java中四种引用的区别以及ReferenceQueue的用途，参考:

[Java对象的强、软、弱和虚引用原理+结合ReferenceQueue对象构造Java对象的高速缓存器](http://blog.csdn.net/lyfi01/article/details/6415726)

usesKeyReferences源码:

```java
boolean usesKeyReferences() {
	return keyStrength != Strength.STRONG;
}
```

keyStrength通过CacheBuilder.getKeyStrength获取:

```java
Strength getKeyStrength() {
	return MoreObjects.firstNonNull(keyStrength, Strength.STRONG);
}
```

可以看出，**默认采用强引用的方式**。我们可以通过CacheBuilder的softValues、weakKeys，weakValues方法对其进行设置。

recencyQueue等队列将在后面结合get方法进行说明。

# get(key)

即LocalLoadingCache.get:

```java
@Override
public V get(K key) throws ExecutionException {
	return localCache.getOrLoad(key);
}
```

LocalCache.getOrLoad:

```java
V getOrLoad(K key) throws ExecutionException {
	return get(key, defaultLoader);
}
```

defaultLoader便是在构造时指定的CacheLoader对象。

LocalCache.get:

```java
V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
	int hash = hash(checkNotNull(key));
    return segmentFor(hash).get(key, hash, loader);
}
```

## Hash算法

LocalCache.hash:

```java
int hash(@Nullable Object key) {
    int h = keyEquivalence.hash(key);
    return rehash(h);
}
```

keyEquivalence是策略模式的体现，针对不同的引用方式(LocalCache.Strength)提供不同的hash算法实现。

Equivalence接口类图:

![Equivalence类图](images/Equivalence.jpg)

keyEquivalence属性由CacheBuilder的getKeyEquivalence方法获得:

```java
Equivalence<Object> getKeyEquivalence() {
	return MoreObjects.firstNonNull(keyEquivalence, getKeyStrength().defaultEquivalence());
}
```

可以看出，**使用的hash算法与Strength相关联**。Strength部分源码(仅展示defaultEquivalence方法):

```java
enum Strength {
	STRONG {
		@Override
		Equivalence<Object> defaultEquivalence() {
			return Equivalence.equals();
		}
	},
	SOFT {
		@Override
		Equivalence<Object> defaultEquivalence() {
			return Equivalence.identity();
		}
	},
	WEAK {
		@Override
		Equivalence<Object> defaultEquivalence() {
			return Equivalence.identity();
		}
	}
};
```

以强引用为例。Equivalence.equals()返回的其实是一个单例的Equals对象，由上面类图可以看出，Equals是Equivalence的子类，源码:

```java
static final class Equals extends Equivalence<Object> implements Serializable {

	static final Equals INSTANCE = new Equals();

	@Override
	protected boolean doEquivalent(Object a, Object b) {
		return a.equals(b);
	}

	@Override
	protected int doHash(Object o) {
		return o.hashCode();
	}

	private Object readResolve() {
		return INSTANCE;
	}
}
```

可以看出，对于强引用来说，其哈希算法就是JDK Object的hashCode方法。

而对于weak和soft引用来说，对应的是Identity实例，源码:

```java
static final class Identity extends Equivalence<Object> implements Serializable {
	static final Identity INSTANCE = new Identity();
	@Override
	protected boolean doEquivalent(Object a, Object b) {
		return false;
	}
	@Override
	protected int doHash(Object o) {
		return System.identityHashCode(o);
	}
	private Object readResolve() {
		return INSTANCE;
	}
}
```

identityHashCode返回的是**默认hashCode方法的计算结果，即根据内存地址计算而来的结果**。

至于为什么要分开处理，暂时未知。

## ReHash

guava cache采用了和ConcurrentHashMap同样的算法。

## Segment选取

LocalCache.segmentFor:

```java
Segment<K, V> segmentFor(int hash) {
	return segments[(hash >>> segmentShift) & segmentMask];
}
```

segmentShift和segmentMask的取值，LocalCache构造器源码:

```java
int segmentShift = 0;
int segmentCount = 1;
while (segmentCount < concurrencyLevel && (!evictsBySize() || segmentCount * 20 <= maxWeight)) {
	++segmentShift;
	segmentCount <<= 1;
}
this.segmentShift = 32 - segmentShift;
segmentMask = segmentCount - 1;
```

可以看出，寻找Segment的过程其实是对**hashCode先取高n位，再取余的过程**。

## get(key,hash,loader)

Segment.get简略版源码:

```java
V get(K key, int hash, CacheLoader<? super K, V> loader) throws ExecutionException {
  try {
	if (count != 0) { // read-volatile
	  // don't call getLiveEntry, which would ignore loading values
	  ReferenceEntry<K, V> e = getEntry(key, hash);
	  if (e != null) {
		long now = map.ticker.read();
		V value = getLiveValue(e, now);
		if (value != null) {
		  recordRead(e, now);
		  statsCounter.recordHits(1);
		  return scheduleRefresh(e, key, hash, value, now, loader);
		}
		ValueReference<K, V> valueReference = e.getValueReference();
		if (valueReference.isLoading()) {
		  return waitForLoadingValue(e, key, valueReference);
		}
	  }
	}
	// at this point e is either null or expired;
	return lockedGetOrLoad(key, hash, loader);
  } catch (ExecutionException ee) {
	throw ee;
  } finally {
	postReadCleanup();
  }
}
```



