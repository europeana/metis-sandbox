package eu.europeana.metis.sandbox.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;
import java.util.Locale;
import org.apache.tika.utils.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring security configuration class.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final List<String> BROWSER_SIGNATURES = List.of(
      "Mozilla", "Chrome", "Safari", "Firefox", "Edge", "Opera"
  );

  /**
   * Configures the security filter chain for the application. It disables CSRF (as the API is stateless and uses JWT for
   * authentication), enables CORS, sets up authorization rules, and configures the OAuth2 resource server with JWT
   * authentication.
   * <p>
   * This is a temporary implementation to allow api users without authentication. Non-provided user agent header or a user agent
   * header that does not match a browser will use this security filter chain.
   *
   * @param httpSecurity the HttpSecurity to be configured with the security settings
   * @return the configured SecurityFilterChain
   * @throws Exception if an error occurs during the security configuration
   */
  @SuppressWarnings("squid:S4502")
  @Order(1)
  @Bean
  public SecurityFilterChain configureApiSecurity(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityMatcher(request -> {
                  String userAgent = request.getHeader("User-Agent");
                  if (StringUtils.isBlank(userAgent)) {
                    return true;
                  }
                  String lowercaseUserAgent = userAgent.toLowerCase(Locale.US);
                  return BROWSER_SIGNATURES.stream().map(String::toLowerCase).noneMatch(lowercaseUserAgent::contains);
                })
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestByFile").permitAll()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestByUrl").permitAll()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestOaiPmh").permitAll()
                    .anyRequest().permitAll()
                );

    return httpSecurity.build();
  }

  /**
   * Configures the security filter chain for the application. It disables CSRF (as the API is stateless and uses JWT for
   * authentication), enables CORS, sets up authorization rules, and configures the OAuth2 resource server with JWT
   * authentication.
   *
   * @param httpSecurity the HttpSecurity to be configured with the security settings
   * @return the configured SecurityFilterChain
   * @throws Exception if an error occurs during the security configuration
   */
  @SuppressWarnings("squid:S4502")
  @Order(2)
  @Bean
  public SecurityFilterChain configureUiSecurity(HttpSecurity httpSecurity) throws Exception {
    httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestByFile").authenticated()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestByUrl").authenticated()
                    .requestMatchers(HttpMethod.POST, "/dataset/*/harvestOaiPmh").authenticated()
                    .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2Configurer ->
                    oauth2Configurer.jwt(withDefaults())
                ).securityMatcher("/**");

    return httpSecurity.build();
  }
}
