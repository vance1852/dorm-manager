package com.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "楼栋不能为空")
    private Long buildingId;
    @NotBlank(message = "房间号不能为空")
    private String roomNumber;
    private Integer floor;
    @NotNull(message = "容量不能为空")
    private Integer capacity;
    private Integer currentCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    
    @TableField(exist = false)
    private String buildingName;
    @TableField(exist = false)
    private List<Bed> beds;
}
