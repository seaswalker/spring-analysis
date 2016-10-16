package base.aop;

/**
 * @author skywalker
 */
public class AopDemo implements AopDemoInter {

    public void send() {
        System.out.println("send from aopdemo");
    }

    public void receive() {
        System.out.println("receive from aopdemo");
    }

    @Override
    public void inter() {
        System.out.println("inter");
    }
}
