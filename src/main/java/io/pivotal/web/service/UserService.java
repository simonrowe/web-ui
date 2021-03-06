package io.pivotal.web.service;

import com.newrelic.api.agent.Trace;
import io.pivotal.web.domain.RegistrationRequest;
import io.pivotal.web.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Service
@RefreshScope
@Slf4j
public class UserService {

    @Autowired(required = false)
    private WebClient webClient;

    @Value("${pivotal.userService.name}")
    private String userService;


    @Trace(async = true)
    public void registerUser(RegistrationRequest registrationRequest) {
        log.debug("Creating user with userId: " + registrationRequest.getEmail());
        User user = webClient
                .post()
                .uri("//" + userService + "/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(registrationRequest)
                .retrieve()
                .bodyToMono(User.class)
                .block();
        log.info("Status from registering account for " + registrationRequest.getEmail() + " is " + user.getId());
    }

    @Trace(async = true)
    public User getUser(String user, OAuth2AuthorizedClient oAuth2AuthorizedClient, OAuth2User oAuth2User) {
        log.debug("Looking for user with user name: " + user);
        User account = this.webClient
                .get()
                .uri("//" + userService + "/users/"+ user)
                .attributes(oauth2AuthorizedClient(oAuth2AuthorizedClient))
                .retrieve()
                .bodyToMono(User.class)
                .block();
        if (oAuth2User instanceof DefaultOidcUser) {
            DefaultOidcUser oidcUser = (DefaultOidcUser)oAuth2User;
            account.setJwt((String)oidcUser.getUserInfo().getClaims().get("accessToken"));
        }

        return account;
    }
}