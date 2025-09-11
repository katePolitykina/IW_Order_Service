package org.example.iw_order_service.client;

import lombok.RequiredArgsConstructor;
import org.example.iw_order_service.dto.UserResponse;
import org.example.iw_order_service.exception.UserServiceClientException;
import org.example.iw_order_service.exception.UserServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final KeycloakTokenClient tokenClient;

    @Value("${service.userservice.url}")
    private String userServiceUrl;

//    public UserResponse getUserByEmail(String email) {
//
//        String url = UriComponentsBuilder
//                .fromUriString(userServiceUrl + "/api/v1.0/users/by-email")
//                .queryParam("email", email)
//                .toUriString();
//
//        return performGetUser(url);
//    }

    public UserResponse getUserById(Long id) {

        String url = UriComponentsBuilder
                .fromUriString(userServiceUrl + "/api/v1.0/users/{id}")
                .buildAndExpand(id)
                .toUriString();
        return performGetUser(url);
    }

    private UserResponse performGetUser(String url) {
        String token = tokenClient.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);


        try {
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    UserResponse.class
            );

            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new UserServiceClientException(
                    "User service returned error: " + ex.getStatusCode()+ " " + ex.getResponseBodyAsString()
            );

        } catch (ResourceAccessException ex) {
            throw new UserServiceUnavailableException("Cannot reach user service: "+ ex.getMessage());
        } catch (RestClientException ex) {
            throw new UserServiceUnavailableException("Unexpected error while calling user service: " + ex.getMessage());
        }
    }

}
