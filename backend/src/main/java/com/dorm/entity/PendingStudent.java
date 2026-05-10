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
    private Integer isSmoker;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private Integer rowNum;
}
