package test.proxy;

/**
 * {@link UserService}实现.
 *
 * @author skywalker
 */
public class UserServiceImpl implements UserService {

    @Override
    public void printName() {
        System.out.println("Name is XXX");
        printAge();
    }

    @Override
    public void printAge() {
        System.out.println("Age: " + 18);
    }

}
