package com.tiantian.yuaiagent.dao;

import com.tiantian.yuaiagent.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问
 */
@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 根据用户名查询 */
    public User findByName(String name) {
        var list = jdbcTemplate.query(
                "SELECT id, name, password, role, remark FROM user WHERE name = ?",
                (rs, row) -> {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setPassword(rs.getString("password"));
                    u.setRole(rs.getInt("role"));
                    u.setRemark(rs.getString("remark"));
                    return u;
                }, name);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 插入用户 */
    public int insert(User user) {
        return jdbcTemplate.update(
                "INSERT INTO user(name, password, role, remark) VALUES(?, ?, ?, ?)",
                user.getName(), user.getPassword(), user.getRole(), user.getRemark());
    }
}
