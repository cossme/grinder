package net.grinder.console.webui;

import net.grinder.console.ConsoleFoundation;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class ServletConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
  
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(ConsoleFoundation.PROPERTIES.getHttpPort());
    }
}