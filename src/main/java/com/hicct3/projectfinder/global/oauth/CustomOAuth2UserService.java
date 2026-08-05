package com.hicct3.projectfinder.global.oauth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, oauth2User.getAttributes());

        Map<String, Object> enrichedAttributes = new HashMap<>(oauth2User.getAttributes());
        enrichedAttributes.put("_provider", attributes.getProvider());
        enrichedAttributes.put("_providerId", attributes.getProviderId());
        enrichedAttributes.put("_email", attributes.getEmail());
        enrichedAttributes.put("_emailVerified", attributes.getEmailVerified());
        enrichedAttributes.put("_name", attributes.getName());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                enrichedAttributes,
                userNameAttributeName
        );
    }
}
