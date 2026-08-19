package com.lanou.springaidemo.security;

import com.lanou.springaidemo.service.UserService;
import com.lanou.springaidemo.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类
 *
 * 关键点（解决循环依赖）：
 * 1. 不用 @RequiredArgsConstructor 注入 UserService（避免 SecurityConfig 持有 UserService 字段）
 * 2. UserService 通过 @Bean 方法参数注入
 * 3. JwtAuthenticationFilter 不再是 @Component，由本配置类手动创建
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * 显式构造器注入 JwtUtil
     * 不注入 UserService 和 JwtAuthenticationFilter，避免循环依赖
     */
    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 认证提供者
     * UserService 通过方法参数注入，不在 SecurityConfig 字段中持有
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserService usersService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 设置用户详情服务
        authProvider.setUserDetailsService(usersService);
        // 设置密码编码器
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 手动创建 JWT 认证过滤器
     * UserService 通过方法参数注入（Spring 自动解析）
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserService userService) {
        return new JwtAuthenticationFilter(jwtUtil, userService);
    }

    /**
     * 安全过滤器链配置
     * UserService 通过方法参数注入（@Lazy 可选，进一步降低耦合）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   AuthenticationProvider authenticationProvider) throws Exception {
        http
            // 禁用 CSRF（RESTful API 不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 配置无状态会话（JWT 认证不需要 Session）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                     // 公开接口
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/posts/public/**").permitAll()
                    .requestMatchers("/posts/search/**").permitAll()
                    .requestMatchers("/posts/latest").permitAll()
                    .requestMatchers("/comments/post/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/agent/**").permitAll()
                    .requestMatchers("/analytics/**").permitAll()

                    // 管理员接口
                    .requestMatchers("/admin/**").hasRole("ADMIN")

                    // 其他请求需要认证
                    .anyRequest().authenticated()
            )

            // 关键：注册认证提供者（供 Spring Security 做密码校验）
            .authenticationProvider(authenticationProvider)

            // 添加 JWT 过滤器（在用户名密码认证过滤器之前）
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 密码编码器（BCrypt）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}