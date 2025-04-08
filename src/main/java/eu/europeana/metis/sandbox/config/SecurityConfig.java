package eu.europeana.metis.sandbox.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  @Bean
  public SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
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
