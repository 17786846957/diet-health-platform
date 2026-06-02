package com.diet.mapper;

import com.diet.entity.WaterRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface WaterRecordMapper extends BaseMapperX<WaterRecord> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM water_record WHERE user_id = #{userId} AND (member_id = #{memberId} OR (member_id IS NULL AND #{memberId} IS NULL)) AND record_date = #{date}")
    double sumAmountByDate(@Param("userId") Long userId, @Param("memberId") Long memberId, @Param("date") LocalDate date);
}