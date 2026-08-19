-- Safe first-start schema for Railway MySQL.
-- Uses IF NOT EXISTS and INSERT IGNORE so restarts do not drop data.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS t_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  avatar TEXT NULL,
  role ENUM('USER','ADMIN') NOT NULL,
  status TINYINT(1) NOT NULL DEFAULT 1,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS t_tags (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(200) NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS t_posts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(500) NOT NULL,
  content LONGTEXT NOT NULL,
  summary TEXT NULL,
  is_ai_generated TINYINT(1) NOT NULL DEFAULT 0,
  author_id BIGINT NOT NULL,
  like_count INT NOT NULL DEFAULT 0,
  view_count INT NOT NULL DEFAULT 0,
  status ENUM('DRAFT','PUBLISHED','ARCHIVED') NOT NULL,
  moderation_status ENUM('PENDING','APPROVED','REJECTED') NULL,
  moderation_result TEXT NULL,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_posts_author (author_id),
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES t_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS t_comments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  content TEXT NOT NULL,
  post_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  parent_id BIGINT NULL,
  like_count INT NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_comments_post (post_id),
  KEY idx_comments_author (author_id),
  KEY idx_comments_parent (parent_id),
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES t_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES t_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES t_comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS t_likes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  post_id BIGINT NULL,
  comment_id BIGINT NULL,
  created_at BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_likes_user_post (user_id, post_id),
  UNIQUE KEY uk_likes_user_comment (user_id, comment_id),
  KEY idx_likes_post (post_id),
  KEY idx_likes_comment (comment_id),
  CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES t_users(id) ON DELETE CASCADE,
  CONSTRAINT fk_likes_post FOREIGN KEY (post_id) REFERENCES t_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_likes_comment FOREIGN KEY (comment_id) REFERENCES t_comments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS t_post_tags (
  post_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (post_id, tag_id),
  KEY idx_post_tags_tag (tag_id),
  CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES t_posts(id) ON DELETE CASCADE,
  CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES t_tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO t_users
  (id, username, password, email, nickname, role, status, created_at, updated_at)
VALUES
  (6, 'zhangsan', '$2a$10$lJWd.rf3ytYuIqM9.GfujOZgMTB92i5EnEiBTk1PD7u.X.YBaiPfO', 'zhangsan@qq.com', '张三', 'USER', 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);
