package base;

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

    @Resource
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
	
}
