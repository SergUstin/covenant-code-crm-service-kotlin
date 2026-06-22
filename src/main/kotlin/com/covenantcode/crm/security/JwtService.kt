package com.covenantcode.crm.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun extractUsername(token: String): String =
        extractClaim(token, Claims::getSubject)

    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T =
        claimsResolver(extractAllClaims(token))

    fun generateToken(userDetails: UserDetails): String =
        buildToken(emptyMap(), userDetails, expiration)

    fun generateToken(extraClaims: Map<String, Any>, userDetails: UserDetails): String =
        buildToken(extraClaims, userDetails, expiration)

    fun generateRefreshToken(userDetails: UserDetails): String =
        buildToken(emptyMap(), userDetails, refreshExpiration)

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean =
        extractUsername(token) == userDetails.username && !isTokenExpired(token)

    private fun buildToken(extraClaims: Map<String, Any>, userDetails: UserDetails, ttlMillis: Long): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.username)
            .issuedAt(Date(now))
            .expiration(Date(now + ttlMillis))
            .signWith(getSigningKey())
            .compact()
    }

    private fun isTokenExpired(token: String): Boolean =
        extractClaim(token, Claims::getExpiration).before(Date())

    private fun extractAllClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload

    private fun getSigningKey() =
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
}
