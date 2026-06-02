package com.diet.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Collection;
import java.util.List;

/**
 * 扩展 BaseMapper，提供便捷查询方法
 * 参考 ruoyi-vue-pro 的 BaseMapperX 设计
 */
public interface BaseMapperX<T> extends BaseMapper<T> {

    /**
     * 分页查询，返回 MyBatis-Plus Page 对象
     */
    default Page<T> selectPage(int pageNo, int pageSize, LambdaQueryWrapper<T> wrapper) {
        Page<T> page = new Page<>(pageNo, pageSize);
        return selectPage(page, wrapper);
    }

    /**
     * 按单个字段精确查询一条
     */
    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /**
     * 按单个字段查询列表
     */
    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /**
     * 按单个字段 IN 查询列表
     */
    default List<T> selectList(SFunction<T, ?> field, Collection<?> values) {
        return selectList(new LambdaQueryWrapper<T>().in(field, values));
    }

    /**
     * 按单个字段统计
     */
    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }
}
