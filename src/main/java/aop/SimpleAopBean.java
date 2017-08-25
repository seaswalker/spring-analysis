package aop;

import org.springframework.aop.framework.AopContext;

/**
 * @author skywalker
 */
public class SimpleAopBean {

    public void boo() {
        System.out.println("testA执行");
        testB();
    }

    public void testB() {
        System.out.println("testB执行");
        ((SimpleAopBean) AopContext.currentProxy()).testC();
    }

    public void testC() {
        System.out.println("testC执行");
    }

}
