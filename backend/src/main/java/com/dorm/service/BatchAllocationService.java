package com.dorm.service;

import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dorm.common.BusinessException;
import com.dorm.dto.*;
import com.dorm.entity.*;
import com.dorm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchAllocationService {

    private final StudentMapper studentMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final BedMapper bedMapper;
    private final SysUserMapper sysUserMapper;

    private final ConcurrentHashMap<String, List<ImportStudentDTO>> pendingStudents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AllocationPreview> previewCache = new ConcurrentHashMap<>();

    public BatchImportResult importExcel(MultipartFile file, String batchId) {
        AtomicInteger rowIndex = new AtomicInteger(0);
        List<ImportStudentDTO> allRows = Collections.synchronizedList(new ArrayList<>());

        try {
            EasyExcel.read(file.getInputStream(), ImportStudentDTO.class, new ReadListener<ImportStudentDTO>() {
                @Override
                public void invoke(ImportStudentDTO data, AnalysisContext context) {
                    int currentRow = rowIndex.incrementAndGet() + 1;
                    data.setRowNum(currentRow);
                    allRows.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().headRowNumber(1).doRead();
        } catch (IOException e) {
            throw new BusinessException("Excel文件读取失败: " + e.getMessage());
        }

        List<String> existingStudentNos = studentMapper.selectList(null).stream()
                .map(Student::getStudentNo)
                .collect(Collectors.toList());

        Set<String> seenStudentNos = new HashSet<>();
        List<ImportStudentDTO> successList = new ArrayList<>();
        List<ImportStudentDTO> failList = new ArrayList<>();

        for (ImportStudentDTO row : allRows) {
            List<String> errors = new ArrayList<>();

            if (row.getStudentNo() == null || row.getStudentNo().trim().isEmpty()) {
                errors.add("学号不能为空");
            } else {
                if (seenStudentNos.contains(row.getStudentNo().trim())) {
                    errors.add("学号与文件内其他行重复");
                } else if (existingStudentNos.contains(row.getStudentNo().trim())) {
                    errors.add("学号已存在于系统中");
                }
                seenStudentNos.add(row.getStudentNo().trim());
            }

            if (row.getName() == null || row.getName().trim().isEmpty()) {
                errors.add("姓名不能为空");
            }

            if (row.getGenderStr() == null || row.getGenderStr().trim().isEmpty()) {
                errors.add("性别不能为空");
            } else if (!"男".equals(row.getGenderStr().trim()) && !"女".equals(row.getGenderStr().trim())) {
                errors.add("性别只能填'男'或'女'");
            } else {
                row.setGender("男".equals(row.getGenderStr().trim()) ? 1 : 2);
            }

            if (row.getMajor() == null || row.getMajor().trim().isEmpty()) {
                errors.add("专业不能为空");
            }

            if (row.getPhone() != null && !row.getPhone().trim().isEmpty()) {
                if (!row.getPhone().trim().matches("^1[3-9]\\d{9}$")) {
                    errors.add("手机号格式不正确");
                }
            }

            if (row.getSchedulePreferenceStr() == null || row.getSchedulePreferenceStr().trim().isEmpty()) {
                errors.add("作息偏好不能为空");
            } else if (!"早睡型".equals(row.getSchedulePreferenceStr().trim()) && !"晚睡型".equals(row.getSchedulePreferenceStr().trim())) {
                errors.add("作息偏好只能填'早睡型'或'晚睡型'");
            } else {
                row.setSchedulePreference("早睡型".equals(row.getSchedulePreferenceStr().trim()) ? 0 : 1);
            }

            if (row.getSmokingStr() == null || row.getSmokingStr().trim().isEmpty()) {
                errors.add("是否吸烟不能为空");
            } else if (!"是".equals(row.getSmokingStr().trim()) && !"否".equals(row.getSmokingStr().trim())) {
                errors.add("是否吸烟只能填'是'或'否'");
            } else {
                row.setSmoking("是".equals(row.getSmokingStr().trim()) ? 1 : 0);
            }

            if (!errors.isEmpty()) {
                row.setValid(false);
                row.setErrorMsg(String.join("; ", errors));
                failList.add(row);
            } else {
                row.setStudentNo(row.getStudentNo().trim());
                row.setName(row.getName().trim());
                row.setMajor(row.getMajor().trim());
                if (row.getPhone() != null) row.setPhone(row.getPhone().trim());
                successList.add(row);
            }
        }

        BatchImportResult result = new BatchImportResult();
        result.setTotalCount(allRows.size());
        result.setSuccessCount(successList.size());
        result.setFailCount(failList.size());
        result.setSuccessList(successList);
        result.setFailList(failList);

        if (!successList.isEmpty()) {
            pendingStudents.put(batchId, successList);
            result.setSuccessList(successList);
        }

        return result;
    }

    public AllocationPreview allocate(String batchId) {
        List<ImportStudentDTO> students = pendingStudents.get(batchId);
        if (students == null || students.isEmpty()) {
            throw new BusinessException("没有待分配的学生数据，请先导入Excel");
        }

        List<Building> buildings = buildingMapper.selectList(new LambdaQueryWrapper<Building>()
                .eq(Building::getStatus, 1));

        List<Room> allRooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, 1));

        List<Bed> allBeds = bedMapper.selectList(new LambdaQueryWrapper<Bed>()
                .eq(Bed::getStatus, 0));

        Map<Long, Building> buildingMap = buildings.stream().collect(Collectors.toMap(Building::getId, b -> b));
        Map<Long, Room> roomMap = allRooms.stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, List<Bed>> bedsByRoom = allBeds.stream().collect(Collectors.groupingBy(Bed::getRoomId));
        Map<Long, List<Room>> roomsByBuilding = allRooms.stream().collect(Collectors.groupingBy(Room::getBuildingId));

        Map<Long, Integer> roomOccupiedCount = new HashMap<>();
        for (Room room : allRooms) {
            List<Bed> occupiedBeds = bedMapper.selectList(new LambdaQueryWrapper<Bed>()
                    .eq(Bed::getRoomId, room.getId())
                    .eq(Bed::getStatus, 1));
            roomOccupiedCount.put(room.getId(), occupiedBeds.size());
        }

        Map<Long, Integer> roomAvailableBeds = new HashMap<>();
        for (Room room : allRooms) {
            List<Bed> available = bedsByRoom.getOrDefault(room.getId(), Collections.emptyList());
            roomAvailableBeds.put(room.getId(), available.size());
        }

        List<Building> maleBuildings = buildings.stream().filter(b -> b.getGender() == 1).collect(Collectors.toList());
        List<Building> femaleBuildings = buildings.stream().filter(b -> b.getGender() == 2).collect(Collectors.toList());

        List<ImportStudentDTO> maleStudents = students.stream().filter(s -> s.getGender() == 1).collect(Collectors.toList());
        List<ImportStudentDTO> femaleStudents = students.stream().filter(s -> s.getGender() == 2).collect(Collectors.toList());

        maleStudents.sort(Comparator.comparing(ImportStudentDTO::getMajor)
                .thenComparing(s -> s.getSchedulePreference() == null ? 0 : s.getSchedulePreference())
                .thenComparing(s -> s.getSmoking() == null ? 0 : s.getSmoking()));
        femaleStudents.sort(Comparator.comparing(ImportStudentDTO::getMajor)
                .thenComparing(s -> s.getSchedulePreference() == null ? 0 : s.getSchedulePreference())
                .thenComparing(s -> s.getSmoking() == null ? 0 : s.getSmoking()));

        Map<Long, Long> studentBedMapping = new LinkedHashMap<>();
        Map<Long, String> matchReasons = new HashMap<>();
        List<AllocationPreview.UnallocatedStudent> unallocatedStudents = new ArrayList<>();

        allocateGroup(maleStudents, maleBuildings, roomsByBuilding, roomMap, bedsByRoom,
                roomAvailableBeds, studentBedMapping, matchReasons, unallocatedStudents);
        allocateGroup(femaleStudents, femaleBuildings, roomsByBuilding, roomMap, bedsByRoom,
                roomAvailableBeds, studentBedMapping, matchReasons, unallocatedStudents);

        AllocationPreview preview = buildPreview(studentBedMapping, matchReasons, buildings, roomsByBuilding, roomMap, bedsByRoom, unallocatedStudents);
        previewCache.put(batchId, preview);
        return preview;
    }

    private void allocateGroup(List<ImportStudentDTO> students, List<Building> buildings,
                               Map<Long, List<Room>> roomsByBuilding, Map<Long, Room> roomMap,
                               Map<Long, List<Bed>> bedsByRoom, Map<Long, Integer> roomAvailableBeds,
                               Map<Long, Long> studentBedMapping, Map<Long, String> matchReasons,
                               List<AllocationPreview.UnallocatedStudent> unallocatedStudents) {
        if (buildings.isEmpty() && !students.isEmpty()) {
            for (ImportStudentDTO s : students) {
                AllocationPreview.UnallocatedStudent u = new AllocationPreview.UnallocatedStudent();
                u.setStudentNo(s.getStudentNo());
                u.setName(s.getName());
                u.setReason("没有对应性别的楼栋");
                unallocatedStudents.add(u);
            }
            return;
        }

        Map<Long, Integer> availableBedsCopy = new HashMap<>(roomAvailableBeds);
        Map<Long, Bed> nextAvailableBed = new HashMap<>();

        for (Room room : roomMap.values()) {
            List<Bed> beds = bedsByRoom.getOrDefault(room.getId(), Collections.emptyList());
            if (!beds.isEmpty()) {
                nextAvailableBed.put(room.getId(), beds.get(0));
            }
        }

        Map<String, List<Room>> majorFloorRooms = new HashMap<>();
        for (Building building : buildings) {
            List<Room> buildingRooms = roomsByBuilding.getOrDefault(building.getId(), Collections.emptyList());
            for (Room room : buildingRooms) {
                if (availableBedsCopy.getOrDefault(room.getId(), 0) > 0) {
                    for (ImportStudentDTO student : students) {
                        String key = student.getMajor() + "_" + building.getId() + "_" + getFloor(room);
                        majorFloorRooms.computeIfAbsent(key, k -> new ArrayList<>());
                        if (!majorFloorRooms.get(key).contains(room)) {
                            majorFloorRooms.get(key).add(room);
                        }
                    }
                }
            }
        }

        for (ImportStudentDTO student : students) {
            Bed assignedBed = null;
            String reason = "";
            Long assignedBuildingId = null;

            List<Room> candidateRooms = new ArrayList<>();

            for (Building building : buildings) {
                String majorFloorKey = student.getMajor() + "_" + building.getId();
                List<Room> sameMajorFloorRooms = new ArrayList<>();

                for (Map.Entry<String, List<Room>> entry : majorFloorRooms.entrySet()) {
                    if (entry.getKey().startsWith(majorFloorKey + "_") && !entry.getValue().isEmpty()) {
                        sameMajorFloorRooms.addAll(entry.getValue());
                    }
                }

                if (!sameMajorFloorRooms.isEmpty()) {
                    if (student.getSmoking() != null && student.getSmoking() == 1) {
                        int maxFloor = sameMajorFloorRooms.stream()
                                .mapToInt(r -> getFloor(r))
                                .max().orElse(1);
                        for (Room r : sameMajorFloorRooms) {
                            if (getFloor(r) == maxFloor && availableBedsCopy.getOrDefault(r.getId(), 0) > 0) {
                                candidateRooms.add(r);
                            }
                        }
                    } else {
                        for (Room r : sameMajorFloorRooms) {
                            if (availableBedsCopy.getOrDefault(r.getId(), 0) > 0) {
                                candidateRooms.add(r);
                            }
                        }
                    }
                    if (!candidateRooms.isEmpty()) {
                        assignedBuildingId = building.getId();
                        break;
                    }
                }
            }

            if (candidateRooms.isEmpty()) {
                for (Building building : buildings) {
                    List<Room> buildingRooms = roomsByBuilding.getOrDefault(building.getId(), Collections.emptyList());
                    for (Room room : buildingRooms) {
                        if (availableBedsCopy.getOrDefault(room.getId(), 0) > 0) {
                            candidateRooms.add(room);
                        }
                    }
                    if (!candidateRooms.isEmpty() && assignedBuildingId == null) {
                        assignedBuildingId = building.getId();
                    }
                    if (!candidateRooms.isEmpty()) break;
                }
            }

            if (candidateRooms.isEmpty()) {
                AllocationPreview.UnallocatedStudent u = new AllocationPreview.UnallocatedStudent();
                u.setStudentNo(student.getStudentNo());
                u.setName(student.getName());
                u.setReason("没有空余床位");
                unallocatedStudents.add(u);
                continue;
            }

            candidateRooms.sort((r1, r2) -> {
                int score1 = computeRoomScore(r1, student, roomMap, bedsByRoom, availableBedsCopy, studentBedMapping);
                int score2 = computeRoomScore(r2, student, roomMap, bedsByRoom, availableBedsCopy, studentBedMapping);
                return score2 - score1;
            });

            Room bestRoom = candidateRooms.get(0);

            if (student.getSmoking() != null && student.getSmoking() == 1) {
                int bestFloor = getFloor(bestRoom);
                List<Room> smokingFloorRooms = candidateRooms.stream()
                        .filter(r -> getFloor(r) == bestFloor)
                        .collect(Collectors.toList());
                if (!smokingFloorRooms.isEmpty()) {
                    bestRoom = smokingFloorRooms.get(0);
                }
            }

            List<Bed> roomBeds = bedsByRoom.getOrDefault(bestRoom.getId(), Collections.emptyList());
            if (roomBeds.isEmpty()) {
                AllocationPreview.UnallocatedStudent u = new AllocationPreview.UnallocatedStudent();
                u.setStudentNo(student.getStudentNo());
                u.setName(student.getName());
                u.setReason("没有空余床位");
                unallocatedStudents.add(u);
                continue;
            }

            List<String> reasonParts = new ArrayList<>();

            boolean sameMajorInFloor = true;
            reasonParts.add("同专业优先同楼层");

            long sameScheduleCount = countSameScheduleInRoom(bestRoom.getId(), student.getSchedulePreference(), studentBedMapping, roomMap, bedsByRoom);
            if (sameScheduleCount > 0) {
                reasonParts.add("作息偏好一致");
            } else {
                reasonParts.add("分配至空闲房间");
            }

            if (student.getSmoking() != null && student.getSmoking() == 1) {
                reasonParts.add("吸烟学生集中楼层");
            }

            Bed bed = roomBeds.remove(0);
            assignedBed = bed;
            reason = String.join("、", reasonParts);

            availableBedsCopy.put(bestRoom.getId(), availableBedsCopy.getOrDefault(bestRoom.getId(), 0) - 1);

            studentBedMapping.put(generateTempStudentId(student), bed.getId());
            matchReasons.put(generateTempStudentId(student), reason);
        }
    }

    private int computeRoomScore(Room room, ImportStudentDTO student,
                                  Map<Long, Room> roomMap, Map<Long, List<Bed>> bedsByRoom,
                                  Map<Long, Integer> availableBedsCopy, Map<Long, Long> studentBedMapping) {
        int score = 0;

        score += 10;

        long sameMajorCount = countSameMajorInRoom(room.getId(), student.getMajor(), studentBedMapping, roomMap, bedsByRoom);
        score += sameMajorCount * 15;

        long sameScheduleCount = countSameScheduleInRoom(room.getId(), student.getSchedulePreference(), studentBedMapping, roomMap, bedsByRoom);
        score += sameScheduleCount * 10;

        if (student.getSmoking() != null && student.getSmoking() == 1) {
            List<Room> buildingRooms = roomMap.values().stream()
                    .filter(r -> r.getBuildingId().equals(room.getBuildingId()))
                    .collect(Collectors.toList());
            int maxFloor = buildingRooms.stream().mapToInt(this::getFloor).max().orElse(1);
            if (getFloor(room) == maxFloor) {
                score += 20;
            }
        }

        int available = availableBedsCopy.getOrDefault(room.getId(), 0);
        int totalCapacity = room.getCapacity();
        int occupancyRate = (totalCapacity - available) * 100 / Math.max(totalCapacity, 1);
        if (occupancyRate > 0 && occupancyRate < 100) {
            score += 5;
        }

        return score;
    }

    private long countSameMajorInRoom(Long roomId, String major, Map<Long, Long> studentBedMapping,
                                       Map<Long, Room> roomMap, Map<Long, List<Bed>> bedsByRoom) {
        List<Bed> beds = bedsByRoom.getOrDefault(roomId, Collections.emptyList());
        long count = 0;
        for (Bed bed : beds) {
            if (studentBedMapping.containsValue(bed.getId())) {
                for (Map.Entry<Long, Long> entry : studentBedMapping.entrySet()) {
                    if (entry.getValue().equals(bed.getId()) && !entry.getKey().equals(generateTempStudentId(null))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private long countSameScheduleInRoom(Long roomId, Integer schedulePreference, Map<Long, Long> studentBedMapping,
                                          Map<Long, Room> roomMap, Map<Long, List<Bed>> bedsByRoom) {
        return 0;
    }

    private int getFloor(Room room) {
        if (room.getFloor() != null && room.getFloor() > 0) {
            return room.getFloor();
        }
        try {
            String roomNumber = room.getRoomNumber();
            return Integer.parseInt(roomNumber.substring(0, Math.min(roomNumber.length(), 1)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Long generateTempStudentId(ImportStudentDTO student) {
        if (student == null) return -1L;
        return -(long) student.getStudentNo().hashCode();
    }

    private AllocationPreview buildPreview(Map<Long, Long> studentBedMapping, Map<Long, String> matchReasons,
                                            List<Building> buildings, Map<Long, List<Room>> roomsByBuilding,
                                            Map<Long, Room> roomMap, Map<Long, List<Bed>> bedsByRoom,
                                            List<AllocationPreview.UnallocatedStudent> unallocatedStudents) {
        AllocationPreview preview = new AllocationPreview();

        Map<Long, ImportStudentDTO> tempStudentMap = new HashMap<>();
        for (List<ImportStudentDTO> list : pendingStudents.values()) {
            for (ImportStudentDTO s : list) {
                tempStudentMap.put(generateTempStudentId(s), s);
            }
        }

        Map<Long, Long> bedToTempStudent = new HashMap<>();
        for (Map.Entry<Long, Long> entry : studentBedMapping.entrySet()) {
            bedToTempStudent.put(entry.getValue(), entry.getKey());
        }

        Set<Long> affectedBuildingIds = new HashSet<>();
        for (Map.Entry<Long, Long> entry : studentBedMapping.entrySet()) {
            Long bedId = entry.getValue();
            Bed bed = bedMapper.selectById(bedId);
            if (bed != null) {
                Room room = roomMapper.selectById(bed.getRoomId());
                if (room != null) {
                    affectedBuildingIds.add(room.getBuildingId());
                }
            }
        }

        List<AllocationPreview.BuildingAllocation> buildingAllocations = new ArrayList<>();
        for (Building building : buildings) {
            if (!affectedBuildingIds.contains(building.getId())) continue;

            AllocationPreview.BuildingAllocation ba = new AllocationPreview.BuildingAllocation();
            ba.setBuildingId(building.getId());
            ba.setBuildingName(building.getName());
            ba.setGender(building.getGender());

            List<Room> buildingRooms = roomsByBuilding.getOrDefault(building.getId(), Collections.emptyList());
            Map<Integer, List<Room>> roomsByFloor = buildingRooms.stream()
                    .collect(Collectors.groupingBy(this::getFloor));

            List<AllocationPreview.FloorAllocation> floorAllocations = new ArrayList<>();
            for (Map.Entry<Integer, List<Room>> floorEntry : roomsByFloor.entrySet()) {
                boolean floorHasAllocation = false;
                AllocationPreview.FloorAllocation fa = new AllocationPreview.FloorAllocation();
                fa.setFloor(floorEntry.getKey());

                List<AllocationPreview.RoomAllocation> roomAllocations = new ArrayList<>();
                for (Room room : floorEntry.getValue()) {
                    List<Bed> roomBeds = bedMapper.selectByRoomId(room.getId());

                    AllocationPreview.RoomAllocation ra = new AllocationPreview.RoomAllocation();
                    ra.setRoomId(room.getId());
                    ra.setRoomNumber(room.getRoomNumber());
                    ra.setCapacity(room.getCapacity());
                    ra.setCurrentCount(room.getCurrentCount());

                    List<AllocationPreview.BedAllocation> bedAllocations = new ArrayList<>();
                    for (Bed bed : roomBeds) {
                        AllocationPreview.BedAllocation bedAlloc = new AllocationPreview.BedAllocation();
                        bedAlloc.setBedId(bed.getId());
                        bedAlloc.setBedNumber(bed.getBedNumber());

                        Long tempStudentId = bedToTempStudent.get(bed.getId());
                        if (tempStudentId != null) {
                            ImportStudentDTO studentDTO = tempStudentMap.get(tempStudentId);
                            if (studentDTO != null) {
                                bedAlloc.setStudentNo(studentDTO.getStudentNo());
                                bedAlloc.setStudentName(studentDTO.getName());
                                bedAlloc.setMatchReason(matchReasons.getOrDefault(tempStudentId, ""));
                                floorHasAllocation = true;
                            }
                        }

                        bedAllocations.add(bedAlloc);
                    }
                    ra.setBeds(bedAllocations);
                    roomAllocations.add(ra);
                }
                fa.setRooms(roomAllocations);
                floorAllocations.add(fa);
            }

            floorAllocations.sort(Comparator.comparing(AllocationPreview.FloorAllocation::getFloor));
            ba.setFloors(floorAllocations);
            buildingAllocations.add(ba);
        }

        preview.setBuildings(buildingAllocations);
        preview.setTotalAllocated(studentBedMapping.size());
        preview.setTotalUnallocated(unallocatedStudents.size());
        preview.setUnallocatedStudents(unallocatedStudents);

        return preview;
    }

    @Transactional
    public void confirm(String batchId) {
        List<ImportStudentDTO> students = pendingStudents.get(batchId);
        if (students == null || students.isEmpty()) {
            throw new BusinessException("没有待确认的分配方案");
        }

        AllocationPreview preview = previewCache.get(batchId);
        if (preview == null) {
            throw new BusinessException("分配方案已过期，请重新分配");
        }

        for (ImportStudentDTO dto : students) {
            SysUser user = new SysUser();
            user.setUsername(dto.getStudentNo());
            user.setPassword(BCrypt.hashpw("123456"));
            user.setRealName(dto.getName());
            user.setPhone(dto.getPhone());
            user.setRole(3);
            user.setStatus(1);
            sysUserMapper.insert(user);

            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNo(dto.getStudentNo());
            student.setName(dto.getName());
            student.setGender(dto.getGender());
            student.setMajor(dto.getMajor());
            student.setPhone(dto.getPhone());
            student.setSchedulePreference(dto.getSchedulePreference());
            student.setSmoking(dto.getSmoking());
            studentMapper.insert(student);

            Long tempId = generateTempStudentId(dto);
            Long bedId = findBedIdForTempStudent(preview, tempId);
            if (bedId != null) {
                Bed bed = bedMapper.selectById(bedId);
                if (bed != null && bed.getStatus() == 0) {
                    bed.setStudentId(student.getId());
                    bed.setStatus(1);
                    bedMapper.updateById(bed);

                    Room room = roomMapper.selectById(bed.getRoomId());
                    if (room != null) {
                        room.setCurrentCount(room.getCurrentCount() + 1);
                        roomMapper.updateById(room);
                    }
                }
            }
        }

        pendingStudents.remove(batchId);
        previewCache.remove(batchId);
    }

    private Long findBedIdForTempStudent(AllocationPreview preview, Long tempStudentId) {
        for (AllocationPreview.BuildingAllocation ba : preview.getBuildings()) {
            for (AllocationPreview.FloorAllocation fa : ba.getFloors()) {
                for (AllocationPreview.RoomAllocation ra : fa.getRooms()) {
                    for (AllocationPreview.BedAllocation bedAlloc : ra.getBeds()) {
                        if (bedAlloc.getStudentNo() != null && tempStudentId != null) {
                            for (ImportStudentDTO dto : pendingStudents.values().stream().flatMap(List::stream).collect(Collectors.toList())) {
                                if (dto.getStudentNo().equals(bedAlloc.getStudentNo()) && generateTempStudentId(dto).equals(tempStudentId)) {
                                    return bedAlloc.getBedId();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public AllocationPreview swapStudents(String batchId, SwapRequest swapRequest) {
        AllocationPreview preview = previewCache.get(batchId);
        if (preview == null) {
            throw new BusinessException("分配方案不存在，请重新分配");
        }

        AllocationPreview.BedAllocation bed1 = findBedAllocation(preview, swapRequest.getBedId1());
        AllocationPreview.BedAllocation bed2 = findBedAllocation(preview, swapRequest.getBedId2());

        if (bed1 == null || bed2 == null) {
            throw new BusinessException("床位不存在");
        }

        String tempNo1 = bed1.getStudentNo();
        String tempName1 = bed1.getStudentName();
        String tempReason1 = bed1.getMatchReason();
        String tempNo2 = bed2.getStudentNo();
        String tempName2 = bed2.getStudentName();
        String tempReason2 = bed2.getMatchReason();

        bed1.setStudentNo(tempNo2);
        bed1.setStudentName(tempName2);
        bed1.setMatchReason("手动调整");
        bed2.setStudentNo(tempNo1);
        bed2.setStudentName(tempName1);
        bed2.setMatchReason("手动调整");

        return preview;
    }

    private AllocationPreview.BedAllocation findBedAllocation(AllocationPreview preview, Long bedId) {
        for (AllocationPreview.BuildingAllocation ba : preview.getBuildings()) {
            for (AllocationPreview.FloorAllocation fa : ba.getFloors()) {
                for (AllocationPreview.RoomAllocation ra : fa.getRooms()) {
                    for (AllocationPreview.BedAllocation bedAlloc : ra.getBeds()) {
                        if (bedAlloc.getBedId().equals(bedId)) {
                            return bedAlloc;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String createBatchId() {
        return UUID.randomUUID().toString();
    }

    public AllocationPreview getPreview(String batchId) {
        return previewCache.get(batchId);
    }
}
