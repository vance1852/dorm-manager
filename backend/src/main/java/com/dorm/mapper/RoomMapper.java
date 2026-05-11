package com.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dorm.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {
    
    @Select("<script>" +
            "SELECT r.*, b.name as building_name " +
            "FROM room r " +
            "LEFT JOIN building b ON r.building_id = b.id " +
            "WHERE r.deleted = 0 " +
            "<if test='buildingId != null'> AND r.building_id = #{buildingId}</if>" +
            "<if test='roomNumber != null and roomNumber != \"\"'> AND r.room_number LIKE CONCAT('%', #{roomNumber}, '%')</if>" +
            "ORDER BY r.id DESC" +
            "</script>")
    IPage<Room> selectPageWithInfo(Page<Room> page, @Param("buildingId") Long buildingId, @Param("roomNumber") String roomNumber);

    @Select("SELECT r.* FROM room r " +
            "JOIN building b ON r.building_id = b.id " +
            "WHERE r.deleted = 0 AND r.status = 1 AND b.gender = #{gender} " +
            "ORDER BY b.id, r.room_number")
    List<Room> selectAvailableRoomsByGender(@Param("gender") Integer gender);
}
