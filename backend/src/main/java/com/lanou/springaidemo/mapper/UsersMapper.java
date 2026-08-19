package com.lanou.springaidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanou.springaidemo.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Mapper
public interface UsersMapper extends BaseMapper<Users> {
    /**
     * 根据用户名查询用户（登录认证时使用）
     * @param username 用户名
     * @return 用户对象（Optional包装，避免空指针）
     */
    @Select("SELECT * FROM t_users WHERE username = #{username}")
    Optional<Users> findByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    @Select("SELECT * FROM t_users WHERE email = #{email}")
    Optional<Users> findByEmail(@Param("email") String email);

    /**
     * 检查用户名是否已存在（注册校验）
     * @param username 用户名
     * @return 是否已存在
     */
    @Select("SELECT COUNT(*) FROM t_users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否已注册（注册校验）
     * @param email 邮箱
     * @return 是否已注册
     */
    @Select("SELECT COUNT(*) FROM t_users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    @Select("SELECT * FROM t_users WHERE role = #{role}")
    List<Users> findByRole(@Param("role") Users.Role role);

    @Select("SELECT * FROM t_users WHERE username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')")
    List<Users> findByUsernameContainingOrEmailContaining(@Param("keyword") String keyword);
}
