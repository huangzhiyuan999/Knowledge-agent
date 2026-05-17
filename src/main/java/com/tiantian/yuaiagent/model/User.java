package com.tiantian.yuaiagent.model;

/**
 * 用户实体
 */
public class User {
    private Long id;
    private String name;
    private String password;
    private Integer role;      // 0=管理员 1=用户
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
