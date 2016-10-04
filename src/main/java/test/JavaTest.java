package test;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author skywalker
 */
public class JavaTest {

    private class MyList extends ArrayList {

        @Override
        public String get(int index) {
            return "";
        }
    }

    public static void main(String[] args) {
        for (Method method : MyList.class.getDeclaredMethods()) {
            System.out.println("name: " + method.getName() + ", return: " + method.getReturnType());
        }
    }

    @Test
    public void classpath() {
        System.out.println(System.getProperty("java.class.path"));
    }

    @Test
    public void findClass() throws IOException {
        Enumeration<URL> base = JavaTest.class.getClassLoader().getResources("base/*");
        while (base.hasMoreElements()) {
            System.out.println(base.nextElement().toString());
        }
    }

}
