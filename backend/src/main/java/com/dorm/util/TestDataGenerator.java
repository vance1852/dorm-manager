package com.dorm.util;

import com.alibaba.excel.EasyExcel;
import com.dorm.dto.ImportStudentDTO;
import java.util.ArrayList;
import java.util.List;

public class TestDataGenerator {

    public static List<ImportStudentDTO> generateTestStudents() {
        List<ImportStudentDTO> list = new ArrayList<>();

        addStudent(list, "2024001", "张伟", "男", "计算机科学与技术", "13800100001", "早睡型", "否");
        addStudent(list, "2024002", "李强", "男", "计算机科学与技术", "13800100002", "晚睡型", "否");
        addStudent(list, "2024003", "王磊", "男", "计算机科学与技术", "13800100003", "早睡型", "是");
        addStudent(list, "2024004", "刘洋", "男", "软件工程", "13800100004", "晚睡型", "否");
        addStudent(list, "2024005", "陈超", "男", "软件工程", "13800100005", "早睡型", "否");
        addStudent(list, "2024006", "杨帆", "男", "电子信息工程", "13800100006", "晚睡型", "是");
        addStudent(list, "2024007", "赵鹏", "男", "电子信息工程", "13800100007", "早睡型", "否");
        addStudent(list, "2024008", "黄涛", "男", "通信工程", "13800100008", "晚睡型", "否");
        addStudent(list, "2024009", "周杰", "男", "通信工程", "13800100009", "早睡型", "否");
        addStudent(list, "2024010", "吴昊", "男", "自动化", "13800100010", "晚睡型", "是");
        addStudent(list, "2024011", "林娜", "女", "英语", "13800100011", "早睡型", "否");
        addStudent(list, "2024012", "郑琳", "女", "英语", "13800100012", "晚睡型", "否");
        addStudent(list, "2024013", "谢芳", "女", "会计学", "13800100013", "早睡型", "否");
        addStudent(list, "2024014", "韩雪", "女", "会计学", "13800100014", "晚睡型", "否");
        addStudent(list, "2024015", "唐敏", "女", "金融学", "13800100015", "早睡型", "是");
        addStudent(list, "2024016", "冯静", "女", "金融学", "13800100016", "晚睡型", "否");
        addStudent(list, "2024017", "曹颖", "女", "法学", "13800100017", "早睡型", "否");
        addStudent(list, "2024018", "邓丽", "女", "法学", "13800100018", "晚睡型", "否");
        addStudent(list, "2024019", "许婷", "女", "工商管理", "13800100019", "早睡型", "否");
        addStudent(list, "2024020", "叶萍", "女", "工商管理", "13800100020", "晚睡型", "是");

        return list;
    }

    private static void addStudent(List<ImportStudentDTO> list, String studentNo, String name,
                                    String gender, String major, String phone,
                                    String schedulePreference, String smoking) {
        ImportStudentDTO dto = new ImportStudentDTO();
        dto.setStudentNo(studentNo);
        dto.setName(name);
        dto.setGenderStr(gender);
        dto.setMajor(major);
        dto.setPhone(phone);
        dto.setSchedulePreferenceStr(schedulePreference);
        dto.setSmokingStr(smoking);
        list.add(dto);
    }

    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : "test_students.xlsx";
        List<ImportStudentDTO> data = generateTestStudents();
        EasyExcel.write(filePath, ImportStudentDTO.class)
                .sheet("新生信息")
                .doWrite(data);
        System.out.println("测试数据Excel已生成: " + filePath);
    }
}
