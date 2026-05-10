package com.dorm.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchImportResult {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<ImportStudentDTO> successList;
    private List<ImportStudentDTO> failList;
}
