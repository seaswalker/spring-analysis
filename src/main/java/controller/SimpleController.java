package controller;

import model.SimpleModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.DataBinder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import validator.SimpleModelValidator;

/**
 * 简单的Spring {@link org.springframework.stereotype.Controller}.
 *
 * @author skywalker
 */
@Controller
public class SimpleController {

    @InitBinder
    public void initBinder(DataBinder dataBinder) {
        //dataBinder.setValidator(new SimpleModelValidator());
        dataBinder.addValidators(new SimpleModelValidator());
    }

    @RequestMapping("/echo")
    public String echo(String name, Model model) {
        model.addAttribute("echo", "hello " + name);
        return "echo";
    }

    @RequestMapping(value = "/echoAgain", method = RequestMethod.POST)
    public String echo(@Validated @RequestBody SimpleModel simpleModel, Model model) {
        model.addAttribute("echo", "hello " + simpleModel.getName() + ", your age is " + simpleModel.getAge() + ".");
        System.out.println(model.asMap().get("simpleModel"));
        return "echo";
    }

}
