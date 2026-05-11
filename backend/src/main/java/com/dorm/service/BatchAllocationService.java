package com.dorm.service;

import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dorm.common.BusinessException;
import com.dorm.dto.AllocationResultDTO;
import com.dorm.dto.ImportResultDTO;
import com.dorm.dto.StudentImportDTO;
import com.dorm.entity.*;
import com.dorm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchAllocationService {

    private final PendingStudentMapper pendingStudentMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;

    private static final Map<String, Integer> GENDER_MAP = new HashMap<>();
    private static final Map<String, Integer> SMOKING_MAP = new HashMap<>();

    static {
        GENDER_MAP.put("男", 1);
        GENDER_MAP.put("女", 2);
        SMOKING_MAP.put("是", 1);
        SMOKING_MAP.put("否", 0);
    }

    public ImportResultDTO importStudents(MultipartFile file) {
        List<StudentImportDTO> importList;
        try {
            importList = EasyExcel.read(file.getInputStream())
                    .head(StudentImportDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (IOException e) {
            throw new BusinessException("Excel文件解析失败");
        }

        ImportResultDTO result = new ImportResultDTO();
        result.setTotalCount(importList.size());
        String batchNo = "BATCH_" + System.currentTimeMillis();
        result.setBatchNo(batchNo);

        Set<String> studentNoSet = new HashSet<>();
        List<PendingStudent> validStudents = new ArrayList<>();

        for (int i = 0; i < importList.size(); i++) {
            StudentImportDTO dto = importList.get(i);
            int rowNum = i + 2;
            List<String> errors = new ArrayList<>();

            if (dto.getStudentNo() == null || dto.getStudentNo().trim().isEmpty()) {
                errors.add("学号不能为空");
            } else if (studentNoSet.contains(dto.getStudentNo())) {
                errors.add("学号在导入文件中重复");
            } else if (studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                    .eq(Student::getStudentNo, dto.getStudentNo())) > 0) {
                errors.add("学号已在系统中存在");
            } else if (pendingStudentMapper.selectCount(new LambdaQueryWrapper<PendingStudent>()
                    .eq(PendingStudent::getStudentNo, dto.getStudentNo())
                    .eq(PendingStudent::getStatus, 0)) > 0) {
                errors.add("学号已在待分配列表中");
            }

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                errors.add("姓名不能为空");
            }

            if (dto.getGender() == null || dto.getGender().trim().isEmpty()) {
                errors.add("性别不能为空");
            } else if (!GENDER_MAP.containsKey(dto.getGender())) {
                errors.add("性别只能是'男'或'女'");
            }

            if (dto.getSleepPreference() != null && !dto.getSleepPreference().isEmpty()
                    && !"早睡型".equals(dto.getSleepPreference()) && !"晚睡型".equals(dto.getSleepPreference())) {
                errors.add("作息偏好只能是'早睡型'或'晚睡型'");
            }

            if (dto.getSmoking() != null && !dto.getSmoking().isEmpty()
                    && !SMOKING_MAP.containsKey(dto.getSmoking())) {
                errors.add("是否吸烟只能是'是'或'否'");
            }

            if (!errors.isEmpty()) {
                result.getErrors().add(new ImportResultDTO.ImportError(
                        rowNum,
                        dto.getStudentNo(),
                        dto.getName(),
                        String.join("; ", errors)
                ));
            } else {
                studentNoSet.add(dto.getStudentNo());
                PendingStudent ps = new PendingStudent();
                ps.setStudentNo(dto.getStudentNo());
                ps.setName(dto.getName());
                ps.setGender(GENDER_MAP.get(dto.getGender()));
                ps.setMajor(dto.getMajor());
                ps.setPhone(dto.getPhone());
                ps.setSleepPreference(dto.getSleepPreference());
                ps.setSmoking(SMOKING_MAP.getOrDefault(dto.getSmoking(), 0));
                ps.setStatus(0);
                ps.setBatchNo(batchNo);
                validStudents.add(ps);
            }
        }

        for (PendingStudent ps : validStudents) {
            pendingStudentMapper.insert(ps);
            result.getPendingStudents().add(new ImportResultDTO.PendingStudentPreview(
                    ps.getId(),
                    ps.getStudentNo(),
                    ps.getName(),
                    ps.getGender() == 1 ? "男" : "女",
                    ps.getMajor(),
                    ps.getPhone(),
                    ps.getSleepPreference(),
                    ps.getSmoking() == 1 ? "是" : "否"
            ));
        }

        result.setSuccessCount(validStudents.size());
        result.setErrorCount(result.getErrors().size());
        return result;
    }

    public AllocationResultDTO startAllocation(String batchNo) {
        List<PendingStudent> pendingStudents = pendingStudentMapper.selectList(
                new LambdaQueryWrapper<PendingStudent>()
                        .eq(PendingStudent::getBatchNo, batchNo)
                        .eq(PendingStudent::getStatus, 0)
        );

        if (pendingStudents.isEmpty()) {
            throw new BusinessException("没有待分配的学生");
        }

        AllocationResultDTO result = new AllocationResultDTO();
        result.setBatchNo(batchNo);
        result.setTotalStudents(pendingStudents.size());

        List<PendingStudent> maleStudents = pendingStudents.stream()
                .filter(s -> s.getGender() == 1)
                .collect(Collectors.toList());
        List<PendingStudent> femaleStudents = pendingStudents.stream()
                .filter(s -> s.getGender() == 2)
                .collect(Collectors.toList());

        List<Building> maleBuildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(Building::getGender, 1).eq(Building::getStatus, 1)
        );
        List<Building> femaleBuildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(Building::getGender, 2).eq(Building::getStatus, 1)
        );

        Map<Long, BuildingAllocationContext> maleAllocationMap = prepareAllocationContext(maleBuildings);
        Map<Long, BuildingAllocationContext> femaleAllocationMap = prepareAllocationContext(femaleBuildings);

        List<PendingStudent> allocated = new ArrayList<>();
        List<PendingStudent> unallocated = new ArrayList<>();

        allocateStudents(maleStudents, maleAllocationMap, allocated, unallocated, true);
        allocateStudents(femaleStudents, femaleAllocationMap, allocated, unallocated, false);

        result.setAllocatedCount(allocated.size());
        result.setUnallocatedCount(unallocated.size());

        for (PendingStudent s : unallocated) {
            AllocationResultDTO.UnallocatedStudent us = new AllocationResultDTO.UnallocatedStudent();
            us.setId(s.getId());
            us.setStudentNo(s.getStudentNo());
            us.setName(s.getName());
            us.setReason("没有可用床位");
            result.getUnallocatedStudents().add(us);
        }

        buildAllocationResult(result, maleAllocationMap, femaleAllocationMap, allocated);

        return result;
    }

    private Map<Long, BuildingAllocationContext> prepareAllocationContext(List<Building> buildings) {
        Map<Long, BuildingAllocationContext> map = new LinkedHashMap<>();
        for (Building b : buildings) {
            BuildingAllocationContext ctx = new BuildingAllocationContext();
            ctx.building = b;
            ctx.rooms = roomMapper.selectAvailableRoomsByGender(b.getGender()).stream()
                    .filter(r -> r.getBuildingId().equals(b.getId()))
                    .collect(Collectors.toList());
            ctx.roomBeds = new HashMap<>();
            for (Room r : ctx.rooms) {
                List<Bed> beds = bedMapper.selectList(
                        new LambdaQueryWrapper<Bed>().eq(Bed::getRoomId, r.getId()).eq(Bed::getStatus, 0)
                );
                ctx.roomBeds.put(r.getId(), new LinkedList<>(beds));
            }
            map.put(b.getId(), ctx);
        }
        return map;
    }

    private void allocateStudents(List<PendingStudent> students,
                                  Map<Long, BuildingAllocationContext> buildingMap,
                                  List<PendingStudent> allocated,
                                  List<PendingStudent> unallocated,
                                  boolean isMale) {
        Map<String, List<PendingStudent>> majorGroups = students.stream()
                .collect(Collectors.groupingBy(s -> s.getMajor() == null ? "" : s.getMajor()));

        List<PendingStudent> sortedStudents = new ArrayList<>();
        for (List<PendingStudent> group : majorGroups.values()) {
            group.sort((a, b) -> {
                if (a.getSmoking() != b.getSmoking()) return b.getSmoking() - a.getSmoking();
                return (a.getSleepPreference() == null ? "" : a.getSleepPreference())
                        .compareTo(b.getSleepPreference() == null ? "" : b.getSleepPreference());
            });
            sortedStudents.addAll(group);
        }

        for (PendingStudent student : sortedStudents) {
            boolean success = allocateSingleStudent(student, buildingMap);
            if (success) {
                allocated.add(student);
            } else {
                unallocated.add(student);
            }
        }
    }

    private boolean allocateSingleStudent(PendingStudent student, Map<Long, BuildingAllocationContext> buildingMap) {
        for (BuildingAllocationContext ctx : buildingMap.values()) {
            Building building = ctx.building;
            int maxFloor = getMaxFloor(ctx.rooms);

            List<Room> sortedRooms = new ArrayList<>(ctx.rooms);
            sortedRooms.sort((a, b) -> {
                int floorA = getFloorFromRoomNumber(a.getRoomNumber());
                int floorB = getFloorFromRoomNumber(b.getRoomNumber());

                if (student.getSmoking() == 1) {
                    if (floorA == maxFloor && floorB != maxFloor) return -1;
                    if (floorA != maxFloor && floorB == maxFloor) return 1;
                }

                return Integer.compare(floorA, floorB);
            });

            for (Room room : sortedRooms) {
                Queue<Bed> beds = ctx.roomBeds.get(room.getId());
                if (beds == null || beds.isEmpty()) continue;

                Bed bed = beds.peek();
                List<String> reasons = new ArrayList<>();

                if (student.getMajor() != null) {
                    reasons.add("同专业优先");
                }
                if (student.getSmoking() == 1 && getFloorFromRoomNumber(room.getRoomNumber()) == getMaxFloor(ctx.rooms)) {
                    reasons.add("吸烟学生集中楼层");
                }
                if (student.getSleepPreference() != null) {
                    reasons.add("作息偏好匹配");
                }

                bed = beds.poll();
                student.setBedId(bed.getId());
                student.setRoomId(room.getId());
                student.setBuildingId(building.getId());
                student.setBuildingName(building.getName());
                student.setRoomNumber(room.getRoomNumber());
                student.setBedNumber(bed.getBedNumber());
                student.setMatchReason(String.join("、", reasons));
                return true;
            }
        }
        return false;
    }

    private int getFloorFromRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.length() < 2) return 1;
        try {
            return Integer.parseInt(roomNumber.substring(0, roomNumber.length() - 2));
        } catch (Exception e) {
            return 1;
        }
    }

    private int getMaxFloor(List<Room> rooms) {
        int max = 1;
        for (Room r : rooms) {
            max = Math.max(max, getFloorFromRoomNumber(r.getRoomNumber()));
        }
        return max;
    }

    private void buildAllocationResult(AllocationResultDTO result,
                                       Map<Long, BuildingAllocationContext> maleMap,
                                       Map<Long, BuildingAllocationContext> femaleMap,
                                       List<PendingStudent> allocatedStudents) {
        Map<Long, List<PendingStudent>> bedStudentMap = allocatedStudents.stream()
                .collect(Collectors.toMap(PendingStudent::getBedId, s -> Collections.singletonList(s)));

        List<BuildingAllocationContext> allContexts = new ArrayList<>();
        allContexts.addAll(maleMap.values());
        allContexts.addAll(femaleMap.values());

        for (BuildingAllocationContext ctx : allContexts) {
            if (ctx.rooms.isEmpty()) continue;

            AllocationResultDTO.BuildingAllocation buildingDTO = new AllocationResultDTO.BuildingAllocation();
            buildingDTO.setId(ctx.building.getId());
            buildingDTO.setName(ctx.building.getName());
            buildingDTO.setGender(ctx.building.getGender() == 1 ? "男生楼" : "女生楼");

            Map<Integer, AllocationResultDTO.FloorAllocation> floorMap = new TreeMap<>();

            for (Room room : ctx.rooms) {
                int floorNum = getFloorFromRoomNumber(room.getRoomNumber());
                AllocationResultDTO.FloorAllocation floorDTO = floorMap.computeIfAbsent(floorNum, k -> {
                    AllocationResultDTO.FloorAllocation f = new AllocationResultDTO.FloorAllocation();
                    f.setFloorNumber(k);
                    f.setFloorName(k + "层");
                    return f;
                });

                AllocationResultDTO.RoomAllocation roomDTO = new AllocationResultDTO.RoomAllocation();
                roomDTO.setId(room.getId());
                roomDTO.setRoomNumber(room.getRoomNumber());
                roomDTO.setCapacity(room.getCapacity());

                List<Bed> allBeds = bedMapper.selectList(
                        new LambdaQueryWrapper<Bed>().eq(Bed::getRoomId, room.getId())
                );
                int occupiedCount = 0;
                for (Bed bed : allBeds) {
                    AllocationResultDTO.BedAllocation bedDTO = new AllocationResultDTO.BedAllocation();
                    bedDTO.setId(bed.getId());
                    bedDTO.setBedNumber(bed.getBedNumber());

                    if (bedStudentMap.containsKey(bed.getId())) {
                        PendingStudent s = bedStudentMap.get(bed.getId()).get(0);
                        AllocationResultDTO.AllocatedStudent studentDTO = new AllocationResultDTO.AllocatedStudent();
                        studentDTO.setId(s.getId());
                        studentDTO.setStudentNo(s.getStudentNo());
                        studentDTO.setName(s.getName());
                        studentDTO.setMajor(s.getMajor());
                        studentDTO.setMatchReason(s.getMatchReason());
                        bedDTO.setStudent(studentDTO);
                        bedDTO.setOccupied(true);
                        occupiedCount++;
                    } else if (bed.getStatus() == 1) {
                        bedDTO.setOccupied(true);
                    }
                    roomDTO.getBeds().add(bedDTO);
                }
                roomDTO.setCurrentCount(occupiedCount);
                floorDTO.getRooms().add(roomDTO);
            }

            buildingDTO.getFloors().addAll(floorMap.values());
            result.getBuildings().add(buildingDTO);
        }
    }

    @Transactional
    public void confirmAllocation(String batchNo, List<Long> pendingStudentIds) {
        List<PendingStudent> students = pendingStudentMapper.selectList(
                new LambdaQueryWrapper<PendingStudent>()
                        .in(PendingStudent::getId, pendingStudentIds)
                        .eq(PendingStudent::getStatus, 0)
        );

        for (PendingStudent ps : students) {
            Student student = new Student();
            student.setStudentNo(ps.getStudentNo());
            student.setName(ps.getName());
            student.setGender(ps.getGender());
            student.setMajor(ps.getMajor());
            student.setPhone(ps.getPhone());
            student.setEnrollDate(LocalDate.now());

            SysUser user = new SysUser();
            user.setUsername(ps.getStudentNo());
            user.setPassword(BCrypt.hashpw("123456"));
            user.setRealName(ps.getName());
            user.setPhone(ps.getPhone());
            user.setRole(3);
            user.setStatus(1);
            userMapper.insert(user);
            student.setUserId(user.getId());
            studentMapper.insert(student);

            Bed bed = bedMapper.selectById(ps.getBedId());
            bed.setStudentId(student.getId());
            bed.setStatus(1);
            bedMapper.updateById(bed);

            Room room = roomMapper.selectById(bed.getRoomId());
            room.setCurrentCount(room.getCurrentCount() + 1);
            roomMapper.updateById(room);

            ps.setStatus(1);
            pendingStudentMapper.updateById(ps);
        }
    }

    public void clearPending(String batchNo) {
        pendingStudentMapper.update(null,
                new LambdaUpdateWrapper<PendingStudent>()
                        .eq(PendingStudent::getBatchNo, batchNo)
                        .set(PendingStudent::getStatus, 2));
    }

    private static class BuildingAllocationContext {
        Building building;
        List<Room> rooms;
        Map<Long, Queue<Bed>> roomBeds;
    }
}
