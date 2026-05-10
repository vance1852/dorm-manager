package com.dorm.controller;

import com.dorm.annotation.OperationLog;
import com.dorm.annotation.RequireRole;
import com.dorm.common.Result;
import com.dorm.common.RoleConstants;
import com.dorm.dto.AllocationResultDTO;
import com.dorm.entity.PendingStudent;
import com.dorm.service.BatchAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @OperationLog(module = "批量分配", operation = "导入学生数据")
    public Result<List<PendingStudent>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(batchAllocationService.importExcel(file));
    }

    @GetMapping("/pending")
    public Result<List<PendingStudent>> getPendingStudents() {
        return Result.success(batchAllocationService.getPendingStudents());
    }

    @PostMapping("/allocate")
    @OperationLog(module = "批量分配", operation = "开始智能分配")
    public Result<List<AllocationResultDTO>> startAllocation() {
        return Result.success(batchAllocationService.startAllocation());
    }

    @PostMapping("/confirm")
    @OperationLog(module = "批量分配", operation = "确认入住")
    public Result<Void> confirmAllocation(@RequestBody List<AllocationResultDTO> results) {
        batchAllocationService.confirmAllocation(results);
        return Result.success();
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] data = batchAllocationService.downloadTemplate();
        String filename = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/sample")
    public ResponseEntity<byte[]> downloadSampleData() throws IOException {
        byte[] data = batchAllocationService.downloadSampleData();
        String filename = URLEncoder.encode("新生导入示例.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @DeleteMapping("/clear")
    @OperationLog(module = "批量分配", operation = "清空待分配数据")
    public Result<Void> clearPendingStudents() {
        batchAllocationService.clearPendingStudents();
        return Result.success();
    }
}
