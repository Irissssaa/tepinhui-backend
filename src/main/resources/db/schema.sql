-- ============================================
-- 特品汇数据库初始化脚本
-- MySQL 8.0
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS tepinhui
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tepinhui;

-- ============================================
-- 核心业务表
-- ============================================

-- 1. 用户表
-- DROP TABLE IF EXISTS `user`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `role` ENUM('CONSUMER', 'MERCHANT', 'ADMIN') NOT NULL COMMENT '角色',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 商家表
-- DROP TABLE IF EXISTS `merchant`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `merchant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `shop_name` VARCHAR(100) DEFAULT NULL COMMENT '店铺名称',
    `license_no` VARCHAR(50) DEFAULT NULL COMMENT '营业执照号',
    `qualification` VARCHAR(255) DEFAULT NULL COMMENT '资质文件URL',
    `status` ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending' COMMENT '审核状态',
    `audit_remark` VARCHAR(200) DEFAULT NULL COMMENT '审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- 3. 产地表
-- DROP TABLE IF EXISTS `origin`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `origin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产地ID',
    `province_name` VARCHAR(30) DEFAULT NULL COMMENT '省份名称',
    `city_name` VARCHAR(30) DEFAULT NULL COMMENT '城市名称',
    `county_name` VARCHAR(30) DEFAULT NULL COMMENT '区县名称',
    `geo_code` VARCHAR(20) DEFAULT NULL COMMENT '地理编码（行政编码）',
    `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '详细地址',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产地表';

-- 4. 特产表
-- DROP TABLE IF EXISTS `specialty`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `specialty` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '特产ID',
    `origin_id` BIGINT NOT NULL COMMENT '产地ID',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '特产名称',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `cultural_info` TEXT COMMENT '文化信息',
    `season_tag` VARCHAR(50) DEFAULT NULL COMMENT '季节标签',
    `cover_img` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    `is_landing` TINYINT DEFAULT 0 COMMENT '是否上架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (`id`),
    INDEX `idx_origin_id` (`origin_id`),
    FOREIGN KEY (`origin_id`) REFERENCES `origin`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特产表';

-- 5. 商品表
-- DROP TABLE IF EXISTS `product`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `merchant_id` BIGINT NOT NULL COMMENT '商家ID',
    `specialty_id` BIGINT NOT NULL COMMENT '特产ID',
    `name` VARCHAR(200) DEFAULT NULL COMMENT '商品名称',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `images` JSON COMMENT '商品图片列表',
    `status` ENUM('on', 'off', 'review') NOT NULL DEFAULT 'review' COMMENT '状态（上架/下架/审核中）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (`id`),
    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_specialty_id` (`specialty_id`),
    FOREIGN KEY (`merchant_id`) REFERENCES `merchant`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`specialty_id`) REFERENCES `specialty`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 6. 订单表
-- DROP TABLE IF EXISTS `orders`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '总金额',
    `status` ENUM('pending', 'paid', 'shipped', 'done', 'cancelled') NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    `address` JSON COMMENT '收货地址快照',
    `logistics_no` VARCHAR(50) DEFAULT NULL COMMENT '物流单号',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    INDEX `idx_user_id` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 7. 订单明细表
-- DROP TABLE IF EXISTS `order_item`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
    `subtotal` DECIMAL(10,2) DEFAULT NULL COMMENT '小计',
    `snapshot` JSON COMMENT '商品快照',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_product_id` (`product_id`),
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 8. 溯源记录表
-- DROP TABLE IF EXISTS `trace_record`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `trace_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '溯源记录ID',
    `trace_code` VARCHAR(64) NOT NULL COMMENT '溯源码',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
    `qr_url` VARCHAR(255) DEFAULT NULL COMMENT '二维码URL',
    `origin_address` VARCHAR(200) DEFAULT NULL COMMENT '产地详细地址',
    `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    `produce_date` DATE DEFAULT NULL COMMENT '生产日期',
    `producer_name` VARCHAR(100) DEFAULT NULL COMMENT '生产商名称',
    `raw_material` TEXT COMMENT '原材料信息',
    `process_factory` VARCHAR(100) DEFAULT NULL COMMENT '加工厂名称',
    `process_date` DATE DEFAULT NULL COMMENT '加工日期',
    `process_desc` TEXT COMMENT '加工过程描述',
    `inspect_org` VARCHAR(100) DEFAULT NULL COMMENT '检测机构',
    `inspect_date` DATE DEFAULT NULL COMMENT '检测日期',
    `inspect_result` VARCHAR(50) DEFAULT NULL COMMENT '检测结果',
    `inspect_report_url` VARCHAR(255) DEFAULT NULL COMMENT '检测报告URL',
    `warehouse_in_time` DATETIME DEFAULT NULL COMMENT '入库时间',
    `warehouse_out_time` DATETIME DEFAULT NULL COMMENT '出库时间',
    `logistics_info` TEXT COMMENT '物流信息',
    `audit_status` ENUM('pending', 'pass', 'reject') NOT NULL DEFAULT 'pending' COMMENT '审核状态',
    `audit_remark` VARCHAR(200) DEFAULT NULL COMMENT '审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_code` (`trace_code`),
    INDEX `idx_product_id` (`product_id`),
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='溯源记录表';

-- ============================================
-- 新增表（电商核心流程）
-- ============================================

-- 9. 商品分类表 [新增]
-- DROP TABLE IF EXISTS `category`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 10. 收货地址表 [新增]
-- DROP TABLE IF EXISTS `address`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `address` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `consignee` VARCHAR(50) NOT NULL COMMENT '收货人',
    `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
    `province` VARCHAR(30) NOT NULL COMMENT '省份',
    `city` VARCHAR(30) NOT NULL COMMENT '城市',
    `county` VARCHAR(30) DEFAULT NULL COMMENT '区县',
    `detail` VARCHAR(200) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 11. 购物车表 [新增]
-- DROP TABLE IF EXISTS `cart_item`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `quantity` INT DEFAULT 1 COMMENT '数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 12. 商品评价表 [新增]
-- DROP TABLE IF EXISTS `review`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `rating` TINYINT NOT NULL COMMENT '评分（1-5）',
    `content` TEXT COMMENT '评价内容',
    `images` JSON COMMENT '评价图片',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_order_id` (`order_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- 13. 特产文化内容表 [新增]
-- DROP TABLE IF EXISTS `culture_content`;  -- 注释掉以保护现有数据
CREATE TABLE IF NOT EXISTS `culture_content` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容ID',
    `specialty_id` BIGINT NOT NULL COMMENT '特产ID',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `type` ENUM('video', 'article', 'story') DEFAULT 'article' COMMENT '类型',
    `cover_img` VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_specialty_id` (`specialty_id`),
    FOREIGN KEY (`specialty_id`) REFERENCES `specialty`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特产文化内容表';
