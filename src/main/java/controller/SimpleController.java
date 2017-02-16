package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

}
