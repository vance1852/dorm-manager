package com.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dorm.entity.Bed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BedMapper extends BaseMapper<Bed> {
    
    @Select("SELECT b.*, s.name as student_name, s.student_no " +
            "FROM bed b " +
            "LEFT JOIN student s ON b.student_id = s.id " +
            "WHERE b.room_id = #{roomId} " +
            "ORDER BY b.bed_number")
    List<Bed> selectByRoomId(@Param("roomId") Long roomId);

    @Select("SELECT b.* FROM bed b " +
            "JOIN room r ON b.room_id = r.id " +
            "JOIN building bd ON r.building_id = bd.id " +
            "WHERE b.status = 0 AND bd.gender = #{gender} " +
            "ORDER BY bd.id, r.id, b.id")
    List<Bed> selectAvailableBedsByGender(@Param("gender") Integer gender);
}
