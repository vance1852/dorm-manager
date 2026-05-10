package com.dorm.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ImportStudentDTO {
    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("性别")
    private String genderStr;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("作息偏好")
    private String schedulePreferenceStr;

    @ExcelProperty("是否吸烟")
    private String smokingStr;

    @ExcelIgnore
    private Integer rowNum;
    @ExcelIgnore
    private String errorMsg;
    @ExcelIgnore
    private Integer gender;
    @ExcelIgnore
    private Integer schedulePreference;
    @ExcelIgnore
    private Integer smoking;
    @ExcelIgnore
    private boolean valid = true;
}
