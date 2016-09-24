package local;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Test;

/**
 * 测试java本地化相关
 * 
 * @author skywalker
 *
 */
public class Local {

	@Test
	public void resourceBoundle() {
		//此文件必须放在classpath下面
		ResourceBundle bundle = ResourceBundle.getBundle("resource/resource", Locale.US);
		System.out.println("US: " + bundle.getString("greeting.common"));
		bundle = ResourceBundle.getBundle("resource/resource", Locale.CHINA);
		System.out.println("CN: " + bundle.getString("greeting.common"));
	}
	
}
