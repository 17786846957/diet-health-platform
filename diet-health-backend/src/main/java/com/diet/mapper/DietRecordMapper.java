package com.diet.mapper;

import com.diet.entity.DietRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface DietRecordMapper extends BaseMapperX<DietRecord> {

    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} ORDER BY record_date DESC, create_time DESC")
    List<DietRecord> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
