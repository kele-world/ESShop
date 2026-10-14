package com.w.esshop.service;

import com.w.esshop.entity.Category;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /** 获取所有分类 */
    List<Category> listAll();

    /** 新增分类(管理员) */
    void add(Category category);

    /** 更新分类(管理员) */
    void update(Category category);

    /** 删除分类(管理员) */
    void delete(Long id);
}