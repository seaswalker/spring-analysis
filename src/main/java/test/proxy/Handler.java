package test.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * {@link java.lang.reflect.InvocationHandler}实现.
 *
 * @author skywalker
 */
public class Handler implements InvocationHandler {

    private final Object target;

    public Handler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        System.out.println("Method " + name + " is proxyed.");
        return method.invoke(target, args);
    }


}
