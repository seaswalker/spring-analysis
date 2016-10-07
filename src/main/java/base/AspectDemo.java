package base;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author skywalker
 */
@Component
@Aspect
public class AspectDemo {

    @Pointcut("execution(public void base.SimpleBean.send())")
    public void send() {

    }

    @Pointcut("this(org.springframework.beans.factory.annotation.Configurable)")
    public void annotation() {}

    @Before("send() || annotation()")
    public void doSend() {
        System.out.println("before send!");
    }

}
