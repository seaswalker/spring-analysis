package base;

import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
		SimpleBean bean = SimpleBean.class.cast(context.getBean(SimpleBean.class));
        System.out.println(bean.getStudent().getName());
        context.close();
	}
	
}
