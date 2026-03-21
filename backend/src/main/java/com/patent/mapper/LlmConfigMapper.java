package com.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.patent.model.entity.SysLlmConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * LLM 配置 Mapper
 */
@Mapper
public interface LlmConfigMapper extends BaseMapper<SysLlmConfig> {

    /**
     * 查询用户当前启用的配置
     *
     * @param userId 用户ID（0=系统默认）
     */
    @Select("SELECT * FROM sys_llm_config WHERE user_id = #{userId} AND is_active = 1 AND deleted = 0 LIMIT 1")
    SysLlmConfig findActiveByUserId(@Param("userId") Long userId);

    /**
     * 查询用户所有配置（未删除）
     *
     * @param userId 用户ID
     */
    @Select("SELECT * FROM sys_llm_config WHERE user_id = #{userId} AND deleted = 0 ORDER BY is_active DESC, created_at DESC")
    List<SysLlmConfig> findAllByUserId(@Param("userId") Long userId);

    /**
     * 取消用户所有启用状态（同一用户只能有一个启用配置）
     *
     * @param userId 用户ID
     */
    @Update("UPDATE sys_llm_config SET is_active = 0 WHERE user_id = #{userId} AND deleted = 0")
    void deactivateAllByUserId(@Param("userId") Long userId);
}
