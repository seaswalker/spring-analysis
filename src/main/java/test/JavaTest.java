package test;

import java.lang.reflect.Method;
import java.util.ArrayList;

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

}
