package base.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试嵌套事务.
 *
 * @author skywalker
 */
@Component
public class NestedBean {

    @Transactional(propagation = Propagation.NESTED)
    public void nest() {
        System.out.println("嵌套事务");
    }

}
