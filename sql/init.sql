-- 创建数据库
CREATE DATABASE IF NOT EXISTS `agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `agent`;

-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`     VARCHAR(100) NOT NULL                COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL                COMMENT '密码',
    `role`     TINYINT      NOT NULL DEFAULT 1       COMMENT '角色 0=管理员 1=用户',
    `remark`   VARCHAR(500) DEFAULT NULL             COMMENT '备注信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员
INSERT INTO `user` (`name`, `password`, `role`, `remark`)
VALUES ('admin', 'admin123', 0, '默认管理员账号');
