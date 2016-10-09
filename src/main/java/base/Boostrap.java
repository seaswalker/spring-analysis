package base;

import base.aop.AopDemo;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Arrays;

public class Boostrap {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
        AopDemo aop = AopDemo.class.cast(context.getBean("aopDemo"));
        aop.send();
        aop.receive();
	}
	
}
