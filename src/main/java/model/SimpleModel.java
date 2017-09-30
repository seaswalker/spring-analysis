package model;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import java.util.Date;

/**
 * 简单的model.
 *
 * @author skywalker
 */
public class SimpleModel {

    private String name;
    @Max(value = 90, message = "年龄最大不能超过90")
    private Integer age;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "SimpleModel{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", date='" + date + '\'' +
                '}';
    }
}
