package model;

import javax.validation.constraints.NotBlank;

/**
 * 简单的model.
 *
 * @author skywalker
 */
public class SimpleModel {

    @NotBlank
    private String name;
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "SimpleModel{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

}
