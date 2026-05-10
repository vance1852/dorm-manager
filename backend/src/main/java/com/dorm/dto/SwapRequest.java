package com.dorm.dto;

import lombok.Data;

@Data
public class SwapRequest {
    private Long studentId1;
    private Long bedId1;
    private Long studentId2;
    private Long bedId2;
}
