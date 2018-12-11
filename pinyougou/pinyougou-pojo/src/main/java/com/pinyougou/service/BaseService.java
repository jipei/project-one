package com.pinyougou.service;

import com.pinyougou.vo.PageResult;

import java.io.Serializable;
import java.util.List;

public interface BaseService<T> {

    /**
     * 根据主键查询
     */
    public T findOne(Serializable id);

    /**
     * 查询全部
     */
    public List<T> findAll();

    /**
     * 根据条件查询列表
     */
    public List<T> findByWhere(T t);

    /**
     * 分页查询列表
     */
    public PageResult findPage(Integer page, Integer rows);

    /**
     * 根据条件分页查询列表
     */
    public PageResult findPage(Integer page,Integer rows,T t);

    /**
     * 新增
     */
    public void add(T t);

    /**
     * 根据主键更新
     */
    public void update(T t);

    /**
     * 批量删除
     */
    public void deleteByIds(Serializable[] ids);
}
