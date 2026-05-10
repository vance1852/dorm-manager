package com.dorm.dto;

import lombok.Data;
import java.util.List;

@Data
public class AllocationPreview {
    private List<BuildingAllocation> buildings;
    private int totalAllocated;
    private int totalUnallocated;
    private List<UnallocatedStudent> unallocatedStudents;

    @Data
    public static class BuildingAllocation {
        private Long buildingId;
        private String buildingName;
        private Integer gender;
        private List<FloorAllocation> floors;
    }

    @Data
    public static class FloorAllocation {
        private Integer floor;
        private List<RoomAllocation> rooms;
    }

    @Data
    public static class RoomAllocation {
        private Long roomId;
        private String roomNumber;
        private Integer capacity;
        private Integer currentCount;
        private List<BedAllocation> beds;
    }

    @Data
    public static class BedAllocation {
        private Long bedId;
        private String bedNumber;
        private Long studentId;
        private String studentNo;
        private String studentName;
        private String matchReason;
    }

    @Data
    public static class UnallocatedStudent {
        private String studentNo;
        private String name;
        private String reason;
    }
}
