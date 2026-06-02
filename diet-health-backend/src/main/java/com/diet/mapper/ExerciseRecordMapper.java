package com.diet.mapper;

import com.diet.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

public interface ExerciseRecordMapper extends BaseMapperX<ExerciseRecord> {

    @Select("SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_record WHERE user_id = #{userId} AND (member_id = #{memberId} OR (member_id IS NULL AND #{memberId} IS NULL)) AND record_date = #{date}")
    double sumCaloriesByDate(@Param("userId") Long userId, @Param("memberId") Long memberId, @Param("date") LocalDate date);
}