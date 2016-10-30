package base.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author skywalker
 */
public class AopDemoAdvice implements MethodInterceptor {

    public void beforeSend() {
        System.out.println("before send");
    }

    public void afterSend() {
        System.out.println("after send");
    }

    public void beforeReceive() {
        System.out.println("before receive");
    }

    public void afterReceive() {
        System.out.println("after receive");
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return null;
    }
}
