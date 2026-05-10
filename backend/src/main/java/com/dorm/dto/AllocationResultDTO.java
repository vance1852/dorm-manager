package com.dorm.dto;

import lombok.Data;

@Data
public class AllocationResultDTO {
    private Long pendingStudentId;
    private String studentNo;
    private String studentName;
    private Integer gender;
    private String major;
    private String sleepPreference;
    private Integer isSmoker;
    private Long buildingId;
    private String buildingName;
    private Long roomId;
    private String roomNumber;
    private Long bedId;
    private String bedNumber;
    private String matchReason;
}
