package base;

import annotation.Init;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * SimpleBean
 * 
 * @author skywalker
 *
 */
@Component("simpleBean")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SimpleBean {

    @Autowired
	private Student student;

    public SimpleBean() {}
	
	public SimpleBean(Student student) {
		this.student = student;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public void send() {
		System.out.println("I am send method from SimpleBean!");
	}

    @Init
    public void init() {
        System.out.println("Init!");
    }
	
}
