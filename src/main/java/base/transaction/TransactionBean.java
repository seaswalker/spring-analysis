package base.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试事务.
 *
 * @author skywalker
 */
@Component
public class TransactionBean {

    public NestedBean getNestedBean() {
        return nestedBean;
    }

    public void setNestedBean(NestedBean nestedBean) {
        this.nestedBean = nestedBean;
    }

    private NestedBean nestedBean;

    @Transactional(propagation = Propagation.REQUIRED)
    public void process() {
        System.out.println("事务执行");
        nestedBean.nest();
    }

}
