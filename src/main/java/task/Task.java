package task;

import org.springframework.scheduling.annotation.Async;

/**
 * 测试Spring Task.
 *
 * @author skywalker
 */
public class Task {

    @Async("executor")
    public void print() {
        System.out.println("print执行");
    }

}
