package io.github.thiagojosetj.portfolio.shared.config;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("demo")
public class DemoStaticResourcesConfiguration implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Vite inclui o hash do conteúdo nos arquivos deste diretório em cada build.
    registry
        .addResourceHandler("/assets/**")
        .addResourceLocations("classpath:/static/assets/")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
  }
}
