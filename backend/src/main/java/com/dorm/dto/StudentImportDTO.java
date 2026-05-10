package com.dorm.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
public class StudentImportDTO {

    @ExcelProperty(index = 0, value = "学号")
    @ColumnWidth(15)
    private String studentNo;

    @ExcelProperty(index = 1, value = "姓名")
    @ColumnWidth(12)
    private String name;

    @ExcelProperty(index = 2, value = "性别")
    @ColumnWidth(10)
    private String gender;

    @ExcelProperty(index = 3, value = "专业")
    @ColumnWidth(20)
    private String major;

    @ExcelProperty(index = 4, value = "手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty(index = 5, value = "作息偏好")
    @ColumnWidth(12)
    private String sleepPreference;

    @ExcelProperty(index = 6, value = "是否吸烟")
    @ColumnWidth(10)
    private String isSmoker;
}
