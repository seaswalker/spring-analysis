package cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

/**
 * Guava demo.
 *
 * @author skywalker
 */
public class CacheDemo {

    @Test
    public void cacheLoader() throws ExecutionException {
        LoadingCache<String, String> cache = CacheBuilder.newBuilder().maximumSize(2)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String s) throws Exception {
                        return "Hello: " + s;
                    }
                });
        System.out.println(cache.get("China"));
        cache.put("US", "US");
        System.out.println(cache.get("US"));
        //放不进去
        cache.put("UK", "UK");
    }

}
