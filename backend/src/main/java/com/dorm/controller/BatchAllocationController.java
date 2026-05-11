package com.dorm.controller;

import com.alibaba.excel.EasyExcel;
import com.dorm.annotation.OperationLog;
import com.dorm.annotation.RequireRole;
import com.dorm.common.Result;
import com.dorm.common.RoleConstants;
import com.dorm.dto.AllocationResultDTO;
import com.dorm.dto.ImportResultDTO;
import com.dorm.dto.StudentImportDTO;
import com.dorm.service.BatchAllocationService;
import com.dorm.util.TestDataGenerator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/batch-allocation")
@RequiredArgsConstructor
@RequireRole({RoleConstants.ADMIN, RoleConstants.DORM_MANAGER})
public class BatchAllocationController {

    private final BatchAllocationService batchAllocationService;

    @PostMapping("/import")
    @OperationLog(module = "批量分配", operation = "导入新生数据")
    public Result<ImportResultDTO> importStudents(@RequestParam("file") MultipartFile file) {
        return Result.success(batchAllocationService.importStudents(file));
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("新生导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<StudentImportDTO> templateData = List.of(
                createTemplateRow("2024001", "张三", "男", "计算机科学与技术", "13800138001", "早睡型", "否"),
                createTemplateRow("2024002", "李四", "女", "软件工程", "13800138002", "晚睡型", "是")
        );

        EasyExcel.write(response.getOutputStream(), StudentImportDTO.class)
                .sheet("新生数据")
                .doWrite(templateData);
    }

    @GetMapping("/test-data")
    public void downloadTestData(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("新生测试数据(20条)", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<StudentImportDTO> testData = TestDataGenerator.generateTestData();

        EasyExcel.write(response.getOutputStream(), StudentImportDTO.class)
                .sheet("新生数据")
                .doWrite(testData);
    }

    private StudentImportDTO createTemplateRow(String studentNo, String name, String gender, 
                                               String major, String phone, String sleep, String smoking) {
        StudentImportDTO dto = new StudentImportDTO();
        dto.setStudentNo(studentNo);
        dto.setName(name);
        dto.setGender(gender);
        dto.setMajor(major);
        dto.setPhone(phone);
        dto.setSleepPreference(sleep);
        dto.setSmoking(smoking);
        return dto;
    }

    @PostMapping("/allocate")
    @OperationLog(module = "批量分配", operation = "执行智能分配")
    public Result<AllocationResultDTO> startAllocation(@RequestParam String batchNo) {
        return Result.success(batchAllocationService.startAllocation(batchNo));
    }

    @PostMapping("/confirm")
    @OperationLog(module = "批量分配", operation = "确认入住")
    public Result<Void> confirmAllocation(@RequestParam String batchNo, @RequestBody List<Long> pendingStudentIds) {
        batchAllocationService.confirmAllocation(batchNo, pendingStudentIds);
        return Result.success();
    }

    @PostMapping("/clear")
    @OperationLog(module = "批量分配", operation = "清空待分配数据")
    public Result<Void> clearPending(@RequestParam String batchNo) {
        batchAllocationService.clearPending(batchNo);
        return Result.success();
    }
}
