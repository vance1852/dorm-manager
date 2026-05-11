package com.dorm.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AllocationResultDTO {
    private int totalStudents;
    private int allocatedCount;
    private int unallocatedCount;
    private String batchNo;
    private List<BuildingAllocation> buildings = new ArrayList<>();
    private List<UnallocatedStudent> unallocatedStudents = new ArrayList<>();

    @Data
    public static class BuildingAllocation {
        private Long id;
        private String name;
        private String gender;
        private List<FloorAllocation> floors = new ArrayList<>();
    }

    @Data
    public static class FloorAllocation {
        private String floorName;
        private int floorNumber;
        private List<RoomAllocation> rooms = new ArrayList<>();
    }

    @Data
    public static class RoomAllocation {
        private Long id;
        private String roomNumber;
        private int capacity;
        private int currentCount;
        private List<BedAllocation> beds = new ArrayList<>();
    }

    @Data
    public static class BedAllocation {
        private Long id;
        private String bedNumber;
        private boolean occupied;
        private AllocatedStudent student;
    }

    @Data
    public static class AllocatedStudent {
        private Long id;
        private String studentNo;
        private String name;
        private String major;
        private String matchReason;
    }

    @Data
    public static class UnallocatedStudent {
        private Long id;
        private String studentNo;
        private String name;
        private String reason;
    }
}
