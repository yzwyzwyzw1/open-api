package io.openfuture.api.config.handler

import io.openfuture.api.entity.auth.User
import io.openfuture.api.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Kadach Alexey
 */
class AuthenticationSuccessHandler(
        private val service: UserService
) : SavedRequestAwareAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse,
                                         authentication: Authentication) {
        val principal = authentication.principal as OidcUser
        val persistUser = service.findByGoogleId(principal.subject)

        if (null == persistUser) {
            service.save(User(principal.subject))
        }

        response.sendRedirect("/scaffolds")
    }

}