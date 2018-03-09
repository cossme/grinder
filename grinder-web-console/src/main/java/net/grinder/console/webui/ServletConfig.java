package net.grinder.console.webui;


import net.grinder.common.GrinderProperties;
import net.grinder.console.ConsoleFoundation;
import net.grinder.console.model.ConsoleProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {

        return new EmbeddedServletContainerCustomizer() {
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
                configurableEmbeddedServletContainer.setPort(
                  ConsoleFoundation.PROPERTIES.getHttpPort());
            }
        };
    }
}