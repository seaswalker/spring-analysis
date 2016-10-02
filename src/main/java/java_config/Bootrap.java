package java_config;

import base.SimpleBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author skywalker
 */
public class Bootrap {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SimpleBeanConfig.class);
        SimpleBean simpleBean = context.getBean(SimpleBean.class);
        System.out.println(simpleBean.getStudent().getName());
    }

}
