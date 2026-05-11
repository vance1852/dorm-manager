package com.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pending_student")
public class PendingStudent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String name;
    private Integer gender;
    private String major;
    private String phone;
    private String sleepPreference;
    private Integer smoking;
    private Integer status;
    private String batchNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String matchReason;
    @TableField(exist = false)
    private Long buildingId;
    @TableField(exist = false)
    private String buildingName;
    @TableField(exist = false)
    private Long roomId;
    @TableField(exist = false)
    private String roomNumber;
    @TableField(exist = false)
    private Long bedId;
    @TableField(exist = false)
    private String bedNumber;
}
