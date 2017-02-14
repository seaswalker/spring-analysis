package base.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试事务.
 *
 * @author skywalker
 */
@Component
public class TransactionBean {

    @Transactional
    public void process() {
        System.out.println("事务执行");
    }

}
