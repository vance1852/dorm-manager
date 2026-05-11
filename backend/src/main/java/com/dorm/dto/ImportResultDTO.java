package com.dorm.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultDTO {
    private int totalCount;
    private int successCount;
    private int errorCount;
    private String batchNo;
    private List<ImportError> errors = new ArrayList<>();
    private List<PendingStudentPreview> pendingStudents = new ArrayList<>();

    @Data
    public static class ImportError {
        private int rowNum;
        private String studentNo;
        private String name;
        private String errorMessage;

        public ImportError(int rowNum, String studentNo, String name, String errorMessage) {
            this.rowNum = rowNum;
            this.studentNo = studentNo;
            this.name = name;
            this.errorMessage = errorMessage;
        }
    }

    @Data
    public static class PendingStudentPreview {
        private Long id;
        private String studentNo;
        private String name;
        private String gender;
        private String major;
        private String phone;
        private String sleepPreference;
        private String smoking;

        public PendingStudentPreview(Long id, String studentNo, String name, String gender, 
                                     String major, String phone, String sleepPreference, String smoking) {
            this.id = id;
            this.studentNo = studentNo;
            this.name = name;
            this.gender = gender;
            this.major = major;
            this.phone = phone;
            this.sleepPreference = sleepPreference;
            this.smoking = smoking;
        }
    }
}
