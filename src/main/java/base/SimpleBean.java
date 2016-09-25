package base;

/**
 * SimpleBean
 * 
 * @author skywalker
 *
 */
public class SimpleBean {
	
	private Student student;
	
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
