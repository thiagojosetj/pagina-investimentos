package io.github.thiagojosetj.portfolio.identity.security;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class WalletSecurityConfiguration {
  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      WalletMode mode,
      Clock clock,
      SecurityContextRepository contextRepository,
      ObjectProvider<GoogleLoginConfigurer> google)
      throws Exception {
    http.formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .securityContext(context -> context.securityContextRepository(contextRepository))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers("/api/v1/portfolios", "/api/v1/portfolios/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            SecurityProblemResponse.write(response, 401))
                    .accessDeniedHandler(
                        (request, response, exception) -> {
                          var authentication =
                              SecurityContextHolder.getContext().getAuthentication();
                          boolean anonymousPortfolio =
                              request.getRequestURI().startsWith("/api/v1/portfolios")
                                  && (authentication == null
                                      || !(authentication.getPrincipal()
                                          instanceof SessionPrincipal));
                          SecurityProblemResponse.write(response, anonymousPortfolio ? 401 : 403);
                        }));
    if (mode.available()) {
      http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/allocation-simulations/**"))
          .addFilterAfter(
              new AbsoluteSessionExpiryFilter(clock, mode), SecurityContextHolderFilter.class)
          .logout(
              logout ->
                  logout
                      .logoutUrl("/api/v1/logout")
                      .logoutSuccessHandler(
                          (request, response, authentication) -> response.setStatus(204)));
      if (mode.loginMode().equals("google")) {
        google.getObject().configure(http);
      }
    } else {
      http.csrf(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable);
    }
    return http.build();
  }

  @Bean
  @Profile("wallet | local-wallet")
  CookieSerializer cookieSerializer(WalletMode mode) {
    var cookie = new DefaultCookieSerializer();
    cookie.setCookieName(
        mode.loginMode().equals("google") ? "__Host-PORTFOLIO_SESSION" : "PORTFOLIO_SESSION");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setUseSecureCookie(mode.loginMode().equals("google"));
    cookie.setSameSite("Lax");
    cookie.setCookiePath("/");
    return cookie;
  }
}
