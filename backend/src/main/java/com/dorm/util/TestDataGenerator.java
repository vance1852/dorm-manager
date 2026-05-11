package com.dorm.util;

import com.alibaba.excel.EasyExcel;
import com.dorm.dto.StudentImportDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestDataGenerator {

    public static void main(String[] args) {
        List<StudentImportDTO> testData = generateTestData();
        String outputPath = "新生测试数据.xlsx";
        EasyExcel.write(outputPath, StudentImportDTO.class)
                .sheet("新生数据")
                .doWrite(testData);
        System.out.println("测试Excel文件已生成: " + new File(outputPath).getAbsolutePath());
    }

    public static List<StudentImportDTO> generateTestData() {
        List<StudentImportDTO> list = new ArrayList<>();

        String[][] maleStudents = {
                {"2024001", "张伟", "男", "计算机科学与技术", "13800138001", "早睡型", "否"},
                {"2024002", "李强", "男", "计算机科学与技术", "13800138002", "晚睡型", "是"},
                {"2024003", "王磊", "男", "计算机科学与技术", "13800138003", "早睡型", "否"},
                {"2024004", "刘洋", "男", "软件工程", "13800138004", "晚睡型", "否"},
                {"2024005", "陈明", "男", "软件工程", "13800138005", "早睡型", "是"},
                {"2024006", "杨阳", "男", "软件工程", "13800138006", "晚睡型", "否"},
                {"2024007", "赵鹏", "男", "电子信息工程", "13800138007", "早睡型", "否"},
                {"2024008", "周杰", "男", "电子信息工程", "13800138008", "晚睡型", "是"},
                {"2024009", "吴涛", "男", "电子信息工程", "13800138009", "早睡型", "否"},
                {"2024010", "郑浩", "男", "机械工程", "13800138010", "晚睡型", "否"},
                {"2024011", "孙宇", "男", "机械工程", "13800138011", "早睡型", "否"},
                {"2024012", "马超", "男", "机械工程", "13800138012", "晚睡型", "是"}
        };

        String[][] femaleStudents = {
                {"2024013", "王芳", "女", "计算机科学与技术", "13800138013", "早睡型", "否"},
                {"2024014", "李娜", "女", "计算机科学与技术", "13800138014", "晚睡型", "否"},
                {"2024015", "张丽", "女", "软件工程", "13800138015", "早睡型", "否"},
                {"2024016", "刘婷", "女", "软件工程", "13800138016", "晚睡型", "否"},
                {"2024017", "陈静", "女", "电子信息工程", "13800138017", "早睡型", "否"},
                {"2024018", "杨敏", "女", "电子信息工程", "13800138018", "晚睡型", "否"},
                {"2024019", "赵雪", "女", "财务管理", "13800138019", "早睡型", "否"},
                {"2024020", "周琳", "女", "财务管理", "13800138020", "晚睡型", "否"}
        };

        for (String[] data : maleStudents) {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setStudentNo(data[0]);
            dto.setName(data[1]);
            dto.setGender(data[2]);
            dto.setMajor(data[3]);
            dto.setPhone(data[4]);
            dto.setSleepPreference(data[5]);
            dto.setSmoking(data[6]);
            list.add(dto);
        }

        for (String[] data : femaleStudents) {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setStudentNo(data[0]);
            dto.setName(data[1]);
            dto.setGender(data[2]);
            dto.setMajor(data[3]);
            dto.setPhone(data[4]);
            dto.setSleepPreference(data[5]);
            dto.setSmoking(data[6]);
            list.add(dto);
        }

        return list;
    }
}
