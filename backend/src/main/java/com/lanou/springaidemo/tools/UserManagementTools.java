package com.lanou.springaidemo.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理工具集
 * 给 Spring AI 大模型调用的工具类
 * 功能：用户角色修改、禁用/启用、查询、统计
 */
// 交给 Spring 容器管理
@Component
// 自动注入所有 final 字段
@RequiredArgsConstructor
// 日志注解
@Slf4j
public class UserManagementTools {

    // 用户Mapper，操作用户表
    private final UsersMapper userMapper;

    /**
     * AI工具：更新用户角色（管理员 ↔ 普通用户）
     */
    @Tool(name = "update_user_role", description = "更新用户的角色权限，将普通用户设为管理员或反之。")
    public Map<String, Object> updateUserRole(
            @ToolParam(description = "用户ID", required = true) Long userId,
            @ToolParam(description = "新角色：ADMIN-管理员，USER-普通用户", required = true) String newRole) {

        // 打印AI调用日志
        log.info("Tool Calling: update_user_role - 用户ID: {}, 新角色: {}", userId, newRole);

        // 根据ID查询用户
        Users user = userMapper.selectById(userId);

        // 用户不存在，返回失败
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        try {
            // 将字符串角色转为枚举类型
            Users.Role role = Users.Role.valueOf(newRole.toUpperCase());

            // 设置新角色
            user.setRole(role);

            // 更新数据库
            userMapper.updateById(user);

            // 构造返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "用户角色已更新为: " + newRole);
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("newRole", newRole);
            return result;
        } catch (IllegalArgumentException e) {
            // 角色值无效时捕获异常
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的角色值: " + newRole + "，有效值为 ADMIN 或 USER");
            return result;
        }
    }

    /**
     * AI工具：禁用用户账户（禁用后无法登录）
     */
    @Tool(name = "disable_user", description = "禁用用户账户，被禁用的用户将无法登录系统")
    public Map<String, Object> disableUser(
            @ToolParam(description = "用户ID", required = true) Long userId,
            @ToolParam(description = "禁用原因", required = false) String reason) {

        // 打印日志
        log.info("Tool Calling: disable_user - 用户ID: {}, 原因: {}", userId, reason);

        // 查询用户
        Users user = userMapper.selectById(userId);

        // 用户不存在
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 设置为禁用状态
        user.setStatus(false);

        // 保存到数据库
        userMapper.updateById(user);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户账户已禁用" + (reason != null ? "，原因: " + reason : ""));
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    /**
     * AI工具：启用用户账户
     */
    @Tool(name = "enable_user", description = "重新启用被禁用的用户账户")
    public Map<String, Object> enableUser(
            @ToolParam(description = "用户ID", required = true) Long userId) {

        log.info("Tool Calling: enable_user - 用户ID: {}", userId);

        // 查询用户
        Users user = userMapper.selectById(userId);

        // 用户不存在
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 设置为启用
        user.setStatus(true);

        // 更新数据库
        userMapper.updateById(user);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "用户账户已启用");
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        return result;
    }

    /**
     * AI工具：获取所有管理员
     */
    @Tool(name = "get_admins", description = "获取所有管理员用户列表")
    public Map<String, Object> getAdmins() {
        log.info("Tool Calling: get_admins - 获取管理员列表");

        // 查询角色为 ADMIN 的用户
        List<Users> admins = userMapper.selectList(new LambdaQueryWrapper<Users>()
                .eq(Users::getRole, Users.Role.ADMIN));

        // 封装返回
        Map<String, Object> result = new HashMap<>();
        result.put("total", admins.size());
        result.put("admins", admins);
        return result;
    }

    /**
     * AI工具：获取普通用户列表
     */
    @Tool(name = "get_regular_users", description = "获取所有普通用户（非管理员）列表")
    public Map<String, Object> getRegularUsers(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_regular_users - 限制: {}", limit);

        // 查询角色为 USER 的普通用户
        List<Users> users = userMapper.selectList(new LambdaQueryWrapper<Users>()
                .eq(Users::getRole, Users.Role.USER));

        // 限制返回条数
        int size = limit != null && limit < users.size() ? limit : users.size();

        Map<String, Object> result = new HashMap<>();
        result.put("total", users.size());
        result.put("returned", size);
        result.put("users", users.subList(0, size));
        return result;
    }

    /**
     * AI工具：根据用户名/邮箱搜索用户
     */
    @Tool(name = "search_users", description = "根据用户名或邮箱搜索用户")
    public Map<String, Object> searchUsers(
            @ToolParam(description = "搜索关键词（用户名或邮箱）", required = true) String keyword) {

        log.info("Tool Calling: search_users - 关键词: {}", keyword);

        // 模糊查询：用户名 或 邮箱 包含关键词
        List<Users> users = userMapper.selectList(new LambdaQueryWrapper<Users>()
                .like(Users::getUsername, keyword)
                .or()
                .like(Users::getEmail, keyword));

        Map<String, Object> result = new HashMap<>();
        result.put("total", users.size());
        result.put("users", users);
        return result;
    }

    /**
     * AI工具：获取用户统计数据
     */
    @Tool(name = "get_user_statistics", description = "获取用户统计信息，包括管理员数量、普通用户数量等")
    public Map<String, Object> getUserStatistics() {
        log.info("Tool Calling: get_user_statistics - 获取用户统计");

        // 总用户数
        long totalUsers = userMapper.selectCount(null);

        // 管理员数量
        List<Users> admins = userMapper.selectList(new LambdaQueryWrapper<Users>().eq(Users::getRole, Users.Role.ADMIN));

        // 普通用户数量
        List<Users> users = userMapper.selectList(new LambdaQueryWrapper<Users>().eq(Users::getRole, Users.Role.USER));

        // 封装统计结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);               // 总人数
        result.put("adminCount", admins.size());           // 管理员数
        result.put("userCount", users.size());             // 普通用户数
        result.put("adminPercentage", totalUsers > 0 ? (admins.size() * 100.0 / totalUsers) : 0); // 管理员占比
        return result;
    }
}