package com.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotBlank(message = "姓名不能为空")
    private String name;
    private String college;
    private String major;
    private String className;
    @NotNull(message = "性别不能为空")
    private Integer gender;
    private String phone;
    private Integer schedulePreference;
    private Integer smoking;
    private LocalDate enrollDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    
    @TableField(exist = false)
    private String buildingName;
    @TableField(exist = false)
    private String roomNumber;
    @TableField(exist = false)
    private String bedNumber;
    @TableField(exist = false)
    private Long bedId;
}
