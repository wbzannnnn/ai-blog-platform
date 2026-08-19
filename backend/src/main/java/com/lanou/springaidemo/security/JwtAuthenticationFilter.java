package com.lanou.springaidemo.security;

import com.lanou.springaidemo.service.UserService;
import com.lanou.springaidemo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 从请求头提取 JWT，验证后设置 Spring Security 上下文
 *
 * 关键：不要加 @Component！
 * 因为 SecurityConfig 也需要这个过滤器，如果加 @Component 会导致 Spring 自动注入
 * 进而产生循环依赖（JwtFilter -> UserService -> SecurityConfig -> JwtFilter）
 *
 * 改为在 SecurityConfig 中通过 @Bean 方法手动创建
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * 构造器注入（手动创建时使用）
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 从请求头提取 JWT
            String jwt = extractJwtFromRequest(request);

            // 2. 校验 token 有效性
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt) && !jwtUtil.isTokenExpired(jwt)) {
                // 3. 提取用户名
                String username = jwtUtil.extractUsername(jwt);

                // 4. 加载用户详情（含角色权限）
                UserDetails userDetails = userService.loadUserByUsername(username);

                // 5. 创建认证令牌并设置权限
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()  // 关键：传入权限列表（ROLE_ADMIN等）
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 写入 Security 上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT认证成功，用户: {}, 权限: {}", username, userDetails.getAuthorities());
            }
        } catch (Exception ex) {
            // 记录异常但继续过滤链，由后续的授权规则决定是否放行
            log.error("JWT认证处理异常: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头中提取 JWT
     * 格式：Bearer xxxxx
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
