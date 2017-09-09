package controller;

import model.SimpleModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 简单的Spring {@link org.springframework.stereotype.Controller}.
 *
 * @author skywalker
 */
@Controller
public class SimpleController {

    @RequestMapping("/echo")
    public String echo(String name, Model model) {
        model.addAttribute("echo", "hello " + name);
        return "echo";
    }

    @RequestMapping("/echoAgain")
    public String echo(@ModelAttribute SimpleModel simpleModel, Model model) {
        model.addAttribute("echo", "hello " + simpleModel.getName() + ", your age is " + simpleModel.getAge() + ".");
        System.out.println(model.asMap().get("simpleModel"));
        return "echo";
    }

}
