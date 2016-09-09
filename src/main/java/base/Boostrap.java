package base;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:config.xml");
		SimpleBean bean = context.getBean(SimpleBean.class);
		bean.send();
		context.close();
	}
	
}
