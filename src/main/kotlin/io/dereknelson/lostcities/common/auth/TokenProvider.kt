package io.dereknelson.lostcities.common.auth

import io.dereknelson.lostcities.common.auth.entity.UserRef
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.security.SignatureException
import java.util.*
import java.util.stream.Collectors

@Component
class TokenProvider() {
    private val log = LoggerFactory.getLogger(TokenProvider::class.java)
    private var secretKey: String? = null
    private var tokenValidityInMilliseconds: Long = 60
    private var tokenValidityInMillisecondsForRememberMe: Long = 60 * 60

    private var secret: String = "ZmNhZmUyNzNkNTE1ZTdiZDA2MmJjNWY4MWE2NzFlMTRkMmViNGE3M2E0YTRiYjg1ZGMxMDY1NGZkNjhhMTdmMjI4OTA5NTUzMzkyZjI1NDUyNjFlY2M3MjBkY2Y2OTAwMGU3NDQwYWMxNmZiNTJjZmZjMzkxMmU1OGZmYzQxOGU="

    // @Value("application.authentication.jwt.token-validity-in-seconds")
    private var tokenValidityInSeconds: String = (60 * 60 * 24).toString()

    // @Value("application.security.authentication.jwt.token-validity-in-seconds-for-remember-me")
    private var tokenValidityInSecondsForRememberMe: String = (60 * 60 * 24 * 7).toString()

    init {
        secretKey = secret
        tokenValidityInMilliseconds = 1000 * tokenValidityInSeconds.toLong()
        tokenValidityInMillisecondsForRememberMe =
            1000 * tokenValidityInSecondsForRememberMe.toLong()
    }

    @Suppress("DEPRECATION")
    fun createToken(authentication: Authentication, userRef: UserRef, rememberMe: Boolean): String {
        val authorities = authentication.authorities.stream()
            .map { obj: GrantedAuthority -> obj.authority }
            .collect(Collectors.joining(","))
        val now = Date().time
        val validity: Date
        validity = if (rememberMe) {
            Date(now + tokenValidityInMillisecondsForRememberMe)
        } else {
            Date(now + tokenValidityInMilliseconds)
        }
        return Jwts.builder()
            .setSubject(authentication.name)
            .claim(AUTHORITIES_KEY, authorities)
            .claim(USER_ID_KEY, userRef.id)
            .claim(LOGIN_KEY, userRef.login)
            .claim(EMAIL_KEY, userRef.email)
            .signWith(SignatureAlgorithm.HS512, secretKey!!)
            .setExpiration(validity)
            .compact()
    }

    fun getAuthentication(token: String?): LostCitiesAuthenticationToken {
        Jwts.parserBuilder().setSigningKey(secretKey)
        val claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .body
        val authorities: MutableCollection<GrantedAuthority> = Arrays.stream(
            claims[AUTHORITIES_KEY].toString().split(",".toRegex()).toTypedArray(),
        )
            .map { role: String? -> SimpleGrantedAuthority(role) }
            .collect(Collectors.toList())
        val principal = UserRef(claims[USER_ID_KEY].toString().toLong(), claims[LOGIN_KEY].toString(), claims[EMAIL_KEY].toString())

        val details = principal.asUserDetails(token!!, authorities)

        details.isAuthenticated = true

        return LostCitiesAuthenticationToken(principal, details, token, authorities)
    }

    @Suppress("DEPRECATION")
    fun validateToken(authToken: String?): Boolean {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(authToken)
            return true
        } catch (e: SignatureException) {
            log.info("Invalid JWT signature.")
            log.trace("Invalid JWT signature trace: {}", e)
        } catch (e: MalformedJwtException) {
            log.info("Invalid JWT token.")
            log.trace("Invalid JWT token trace: {}", e)
        } catch (e: ExpiredJwtException) {
            log.info("Expired JWT token.")
            log.trace("Expired JWT token trace: {}", e)
        } catch (e: UnsupportedJwtException) {
            log.info("Unsupported JWT token.")
            log.trace("Unsupported JWT token trace: {}", e)
        } catch (e: IllegalArgumentException) {
            log.info("JWT token compact of handler are invalid.")
            log.trace("JWT token compact of handler are invalid trace: {}", e)
        }
        return false
    }

    private fun UserRef.asUserDetails(token: String, authorities: Collection<GrantedAuthority>): LostCitiesUserDetails {
        return LostCitiesUserDetails(
            id!!,
            login!!,
            email!!,
            userRef = this,
            token = token,
            authority = authorities.toSet(),
            accountNonLocked = true,
            accountNonExpired = true,
            credentialsNonExpired = true,
            enabled = true,
        )
    }

    companion object {
        private const val AUTHORITIES_KEY = "auth"
        private const val USER_ID_KEY = "user_id"
        private const val LOGIN_KEY = "login"
        private const val EMAIL_KEY = "email"
    }
}
