package com.example.uniride.backend.common.security;

import com.example.uniride.backend.common.error.ApiException;
import com.example.uniride.backend.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Id of the user that made the request (the {@code sub} of the JWT). Controllers read it and
 * pass it to the service, so the services do not depend on Spring Security and are easy to
 * test.
 *
 * <pre>{@code
 * @PostMapping("/{reservationId}/accept")
 * ReservationResponse accept(@PathVariable UUID tripId, @PathVariable UUID reservationId) {
 *     return reservationService.accept(currentUser.id(), tripId, reservationId);
 * }
 * }</pre>
 */
@Component
public class CurrentUser {

    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
}
