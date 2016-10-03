package java_config;

import base.SimpleBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

/**
 * @author skywalker
 */
public class Bootrap {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SimpleBeanConfig.class);
        SimpleBean simpleBean = context.getBean(SimpleBean.class);
        System.out.println(simpleBean.getStudent().getName());
        System.out.println(Arrays.toString(context.getBeanDefinitionNames()));
    }

}
