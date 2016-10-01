package base;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
		SimpleBean bean = SimpleBean.class.cast(context.getBean("simpleBean"));
		System.out.println(bean.getStudent().getName());
		context.close();
	}
	
}
