package base;

import base.aop.AopDemo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
        AopDemo aop = (AopDemo) context.getBean("aopDemo");
        aop.send();
    }
	
}
