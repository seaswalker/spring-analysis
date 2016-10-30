package base.aop.annotation;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author skywalker
 */
@Aspect
public class AspectDemo {

    @Pointcut("execution(void base.aop.AopDemo.send(..))")
    public void beforeSend() {}

    @Before("beforeSend()")
    public void before() {
        System.out.println("send之前");
    }

}
