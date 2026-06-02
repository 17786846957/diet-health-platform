package com.diet.mapper;

import com.diet.entity.DietRecordDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DietRecordDetailMapper extends BaseMapperX<DietRecordDetail> {

    @Insert("<script>" +
            "INSERT INTO diet_record_detail (record_id, food_id, amount, calories) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.recordId}, #{item.foodId}, #{item.amount}, #{item.calories})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<DietRecordDetail> details);
}
