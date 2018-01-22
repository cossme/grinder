package net.grinder.console.webui;


import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {

    private int m_httpPort = 6374;

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {

        return new EmbeddedServletContainerCustomizer() {
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
                configurableEmbeddedServletContainer.setPort(m_httpPort);
            }
        };
    }
}