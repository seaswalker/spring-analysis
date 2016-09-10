package base;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Boostrap {

	public static void main(String[] args) {
		System.setProperty("spring", "classpath");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("${spring}:config.xml");
		SimpleBean bean = context.getBean(SimpleBean.class);
		bean.send();
		context.close();
	}
	
}
