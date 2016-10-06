package base;

import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
        //SimpleBean simpleBean = SimpleBean.class.cast(context.getBean(SimpleBean.class));
        SimpleBean simpleBean = new SimpleBean();
        System.out.println(simpleBean.getStudent());
        System.out.println(SimpleBean.class.getName());
        context.close();
	}
	
}
