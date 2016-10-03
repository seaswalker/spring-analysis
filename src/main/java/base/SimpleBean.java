package base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * SimpleBean
 * 
 * @author skywalker
 *
 */
@Component("simpleBean")
public class SimpleBean {

    @Autowired(required = false)
	private Student student;

    public SimpleBean() {}
	
	public SimpleBean(Student student) {
		this.student = student;
	}

	public Student getStudent() {
		return student;
	}

    @Required
	public void setStudent(Student student) {
		this.student = student;
	}

	public void send() {
		System.out.println("I am send method from SimpleBean!");
	}
	
}
