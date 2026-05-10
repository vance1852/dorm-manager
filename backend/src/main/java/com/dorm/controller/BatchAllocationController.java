package com.dorm.controller;

import com.alibaba.excel.EasyExcel;
import com.dorm.annotation.OperationLog;
import com.dorm.annotation.RequireRole;
import com.dorm.common.Result;
import com.dorm.common.RoleConstants;
import com.dorm.dto.AllocationPreview;
import com.dorm.dto.BatchImportResult;
import com.dorm.dto.SwapRequest;
import com.dorm.service.BatchAllocationService;
import com.dorm.util.TestDataGenerator;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/batch-allocation")
@RequiredArgsConstructor
@RequireRole({RoleConstants.ADMIN, RoleConstants.DORM_MANAGER})
public class BatchAllocationController {

    private final BatchAllocationService batchAllocationService;

    @GetMapping("/template")
    @OperationLog(module = "批量分配", operation = "下载导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("新生导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), com.dorm.dto.ImportStudentDTO.class)
                .sheet("新生信息")
                .doWrite(new java.util.ArrayList<>());
    }

    @PostMapping("/import")
    @OperationLog(module = "批量分配", operation = "导入新生Excel")
    public Result<BatchImportResult> importExcel(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("batchId") String batchId) {
        BatchImportResult result = batchAllocationService.importExcel(file, batchId);
        return Result.success(result);
    }

    @PostMapping("/allocate")
    @OperationLog(module = "批量分配", operation = "智能分配")
    public Result<AllocationPreview> allocate(@RequestParam("batchId") String batchId) {
        AllocationPreview preview = batchAllocationService.allocate(batchId);
        return Result.success(preview);
    }

    @GetMapping("/preview")
    public Result<AllocationPreview> getPreview(@RequestParam("batchId") String batchId) {
        AllocationPreview preview = batchAllocationService.getPreview(batchId);
        return Result.success(preview);
    }

    @PostMapping("/confirm")
    @OperationLog(module = "批量分配", operation = "确认入住")
    public Result<Void> confirm(@RequestParam("batchId") String batchId) {
        batchAllocationService.confirm(batchId);
        return Result.success();
    }

    @PostMapping("/swap")
    @OperationLog(module = "批量分配", operation = "手动调整床位")
    public Result<AllocationPreview> swap(@RequestParam("batchId") String batchId,
                                           @RequestBody SwapRequest swapRequest) {
        AllocationPreview preview = batchAllocationService.swapStudents(batchId, swapRequest);
        return Result.success(preview);
    }

    @PostMapping("/batch-id")
    public Result<String> createBatchId() {
        return Result.success(batchAllocationService.createBatchId());
    }

    @GetMapping("/test-data")
    @OperationLog(module = "批量分配", operation = "下载测试数据")
    public void downloadTestData(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("新生测试数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), com.dorm.dto.ImportStudentDTO.class)
                .sheet("新生信息")
                .doWrite(TestDataGenerator.generateTestStudents());
    }
}
