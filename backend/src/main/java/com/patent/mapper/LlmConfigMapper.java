package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.SysLlmConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * LLM 配置 Mapper
 */
@Mapper
public interface LlmConfigMapper extends BaseMapper<SysLlmConfig> {

    /**
     * 查询用户当前选择的配置（通过 user_llm_selection 表）
     * 用户有自己的选择记录则返回对应配置，否则返回系统默认激活配置
     */
    @Select("SELECT c.* FROM sys_llm_config c " +
            "INNER JOIN user_llm_selection s ON c.id = s.config_id " +
            "WHERE s.user_id = #{userId} AND c.deleted = 0 LIMIT 1")
    SysLlmConfig findSelectedByUserId(@Param("userId") Long userId);

    /**
     * 查询系统默认激活配置（is_active=1 的系统配置，管理员设置的全局默认）
     */
    @Select("SELECT * FROM sys_llm_config WHERE user_id = 0 AND is_active = 1 AND deleted = 0 LIMIT 1")
    SysLlmConfig findSystemDefault();

    /**
     * 查询系统默认配置（user_id=0）+ 用户自定义配置（合并展示）
     */
    @Select("SELECT * FROM sys_llm_config WHERE (user_id = 0 OR user_id = #{userId}) AND deleted = 0 " +
            "ORDER BY user_id ASC, created_at DESC")
    List<SysLlmConfig> findAllWithSystem(@Param("userId") Long userId);

    /**
     * 查询用户当前选择的配置ID
     */
    @Select("SELECT config_id FROM user_llm_selection WHERE user_id = #{userId}")
    Long findSelectedConfigId(@Param("userId") Long userId);

    /**
     * 保存或更新用户的配置选择（UPSERT）
     */
    @Insert("INSERT INTO user_llm_selection (user_id, config_id) VALUES (#{userId}, #{configId}) " +
            "ON DUPLICATE KEY UPDATE config_id = #{configId}")
    void upsertUserSelection(@Param("userId") Long userId, @Param("configId") Long configId);

    /**
     * 取消系统配置的全局激活状态（管理员操作）
     */
    @Update("UPDATE sys_llm_config SET is_active = 0 WHERE user_id = 0 AND deleted = 0")
    void deactivateAllSystemConfigs();

    /**
     * 取消用户自定义配置的激活状态（兼容旧逻辑，用户自定义配置 is_active 不再使用）
     */
    @Update("UPDATE sys_llm_config SET is_active = 0 WHERE user_id = #{userId} AND deleted = 0")
    void deactivateAllByUserId(@Param("userId") Long userId);
}
