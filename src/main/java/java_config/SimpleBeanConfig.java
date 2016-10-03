package java_config;

import base.SimpleBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link base.SimpleBean}配置
 *
 * @author skywalker
 */
@Configuration
@Import(StudentConfig.class)
public class SimpleBeanConfig {

    @Autowired
    private StudentConfig studentConfig;

    @Bean
    public SimpleBean simpleBean() {
        SimpleBean simpleBean = new SimpleBean(studentConfig.student());
        return simpleBean;
    }

}
