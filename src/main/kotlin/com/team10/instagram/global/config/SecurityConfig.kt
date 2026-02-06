package com.team10.instagram.global.config

import com.team10.instagram.domain.auth.jwt.JwtAuthenticationFilter
import com.team10.instagram.domain.auth.jwt.OAuth2LoginSuccessHandler
import com.team10.instagram.domain.auth.service.CustomOAuth2UserService
import com.team10.instagram.global.error.ErrorCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // 1. CORS
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            // 인증 실패 시 로그인 페이지 이동(302) 대신 401 에러(JSON) 반환
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    val errorCode = ErrorCode.LOGIN_REQUIRED

                    response.contentType = "application/json;charset=UTF-8"
                    response.status = errorCode.status.value() // 401

                    // JSON 생성 시 Enum의 code와 message를 사용
                    response.writer.write(
                        """
                        {
                            "isSuccess": false,
                            "code": "${errorCode.code}",
                            "message": "${errorCode.message}",
                            "data": null
                        }
                        """.trimIndent(),
                    )
                }
            }.authorizeHttpRequests {
                // Actuator health check
                it.requestMatchers("/actuator/health").permitAll()
                // Swagger
                it
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                    ).permitAll()

                // root
                it.requestMatchers("/").permitAll()
                // Auth 경로
                it.requestMatchers("/api/v1/auth/**").permitAll()
                // 에러 발생 시 Spring이 내부적으로 호출하는 경로
                it.requestMatchers("/error").permitAll()
                it.requestMatchers("/oauth2/**", "/login/**").permitAll()
                // 나머지 요청은 인증 필요
                it.anyRequest().authenticated()
            }.oauth2Login {
                it
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }.successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler { _, response, exception ->
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            exception.message ?: "OAuth authentication failed",
                        )
                    }
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    // 2. CORS 설정 내용
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins =
            listOf(
                "https://www.wfinstaclone.shop", // domain
                "https://d1ki8kre4wetjx.cloudfront.net", // Front
                "https://api.wfinstaclone.shop", // Back
                "http://localhost:3000", // 로컬 (React/Next.js)
                "http://localhost:5173", // 로컬 (Vite)
                "http://localhost:8080", // 로컬 (백엔드)
            )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowCredentials = true // 쿠키/인증정보 포함 허용
        configuration.allowedHeaders = listOf("*") // 모든 헤더 허용

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
