CREATE DATABASE IF NOT EXISTS copywriting_ai_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE copywriting_ai_agent;

SET NAMES utf8mb4;

-- =========================
-- 1. 用户表
-- =========================
DROP TABLE IF EXISTS user;
CREATE TABLE user
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 id',
    userName     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户名',
    userAccount  VARCHAR(64)          DEFAULT NULL COMMENT '用户账号',
    userPassword VARCHAR(255)         DEFAULT NULL COMMENT '用户密码（加密存储）',
    userPhone    VARCHAR(20)          DEFAULT NULL COMMENT '用户手机号',
    avatar       VARCHAR(512)         DEFAULT NULL COMMENT '用户头像',
    userProfile  VARCHAR(512)         DEFAULT NULL COMMENT '用户简介',
    userRole     VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/manager/admin',
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '账号状态：0-正常 1-封禁',
    createTime   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';


-- =========================
-- 2. 内容创作项目表
-- =========================
DROP TABLE IF EXISTS content_project;
CREATE TABLE content_project
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 id',

    userId          BIGINT       NOT NULL COMMENT '用户 id',
    projectName     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '项目名称',
    platform        VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '目标平台：xiaohongshu/wechat/douyin',
    topic           VARCHAR(255) NOT NULL DEFAULT '' COMMENT '主题',
    audience        VARCHAR(255)          DEFAULT NULL COMMENT '目标人群',
    tone            VARCHAR(255)          DEFAULT NULL COMMENT '用户原始风格描述',
    normalizedStyle VARCHAR(64)           DEFAULT NULL COMMENT '归一化风格标签',
    productInfo     TEXT                  DEFAULT NULL COMMENT '产品信息',
    requirement     TEXT                  DEFAULT NULL COMMENT '用户完整原始需求文本',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '项目状态：0:INIT/1:RUNNING/2:SUCCESS/3:FAILED',

    createTime      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='内容创作项目表';


-- =========================
-- 3. 内容生成结果表
-- =========================
DROP TABLE IF EXISTS content_result;
CREATE TABLE content_result
(
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键 id',
    projectId        BIGINT   NOT NULL COMMENT '项目 id',

    title            VARCHAR(512)      DEFAULT NULL COMMENT '标题',
    openingHook      TEXT              DEFAULT NULL COMMENT '开头钩子',
    body             LONGTEXT          DEFAULT NULL COMMENT '正文',
    cta              TEXT              DEFAULT NULL COMMENT 'CTA',
    imagePrompt      LONGTEXT          DEFAULT NULL COMMENT '最终图片提示词',
    reviewPass       TINYINT  NOT NULL DEFAULT 0 COMMENT '是否通过评审：0-否 1-是',
    titleScore       DECIMAL(4, 2)     DEFAULT NULL COMMENT '标题评分',
    bodyScore        DECIMAL(4, 2)     DEFAULT NULL COMMENT '正文评分',
    imagePromptScore DECIMAL(4, 2)     DEFAULT NULL COMMENT '图片提示词评分',
    overallScore     DECIMAL(4, 2)     DEFAULT NULL COMMENT '总体评分',
    reviewFeedback   TEXT              DEFAULT NULL COMMENT '评审反馈',
    createTime       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删 1-已删',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='内容生成结果表';


-- =========================
-- 4. 图片结果表
-- =========================
DROP TABLE IF EXISTS project_image;
CREATE TABLE project_image
(
    id        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键 id',
    projectId BIGINT        NOT NULL COMMENT '项目 id',
    resultId  BIGINT        NOT NULL COMMENT '结果 id',
    imageUrl  VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '图片地址',
    PRIMARY KEY (id),
    KEY idx_projectId (projectId),
    KEY idx_resultId (resultId)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='项目图片表';
