package com.dorm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportDTO {
    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("作息偏好")
    private String sleepPreference;

    @ExcelProperty("是否吸烟")
    private String smoking;
}
