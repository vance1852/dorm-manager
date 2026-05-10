package com.dorm.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dorm.common.BusinessException;
import com.dorm.dto.AllocationResultDTO;
import com.dorm.dto.StudentImportDTO;
import com.dorm.entity.*;
import com.dorm.listener.StudentImportListener;
import com.dorm.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchAllocationService {

    private final PendingStudentMapper pendingStudentMapper;
    private final StudentMapper studentMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper userMapper;

    public List<PendingStudent> importExcel(MultipartFile file) throws IOException {
        StudentImportListener listener = new StudentImportListener();
        EasyExcel.read(file.getInputStream(), StudentImportDTO.class, listener).sheet().doRead();
        List<StudentImportDTO> dataList = listener.getDataList();

        pendingStudentMapper.delete(null);

        Set<String> studentNoSet = new HashSet<>();
        List<PendingStudent> resultList = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            StudentImportDTO dto = dataList.get(i);
            int rowNum = i + 2;
            PendingStudent pending = new PendingStudent();
            pending.setRowNum(rowNum);

            List<String> errors = new ArrayList<>();

            if (StrUtil.isBlank(dto.getStudentNo())) {
                errors.add("学号不能为空");
            } else if (studentNoSet.contains(dto.getStudentNo())) {
                errors.add("学号在文件中重复");
            } else if (studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                    .eq(Student::getStudentNo, dto.getStudentNo())) > 0) {
                errors.add("学号已存在于系统中");
            } else {
                studentNoSet.add(dto.getStudentNo());
            }

            if (StrUtil.isBlank(dto.getName())) {
                errors.add("姓名不能为空");
            }

            Integer gender = null;
            if (StrUtil.isBlank(dto.getGender())) {
                errors.add("性别不能为空");
            } else if ("男".equals(dto.getGender())) {
                gender = 1;
            } else if ("女".equals(dto.getGender())) {
                gender = 2;
            } else {
                errors.add("性别只能是男或女");
            }

            if (StrUtil.isBlank(dto.getPhone())) {
                errors.add("手机号不能为空");
            }

            Integer isSmoker = 0;
            if (StrUtil.isNotBlank(dto.getIsSmoker())) {
                if ("是".equals(dto.getIsSmoker())) {
                    isSmoker = 1;
                } else if (!"否".equals(dto.getIsSmoker())) {
                    errors.add("是否吸烟只能是是或否");
                }
            }

            pending.setStudentNo(dto.getStudentNo());
            pending.setName(dto.getName());
            pending.setGender(gender);
            pending.setMajor(dto.getMajor());
            pending.setPhone(dto.getPhone());
            pending.setSleepPreference(dto.getSleepPreference());
            pending.setIsSmoker(isSmoker);

            if (!errors.isEmpty()) {
                pending.setStatus(2);
                pending.setErrorMsg("第" + rowNum + "行：" + String.join("；", errors));
            } else {
                pending.setStatus(0);
            }

            pendingStudentMapper.insert(pending);
            pending.setId(pending.getId());
            resultList.add(pending);
        }

        return resultList;
    }

    public List<PendingStudent> getPendingStudents() {
        return pendingStudentMapper.selectList(null);
    }

    public List<AllocationResultDTO> startAllocation() {
        List<PendingStudent> pendingStudents = pendingStudentMapper.selectList(
                new LambdaQueryWrapper<PendingStudent>().eq(PendingStudent::getStatus, 0)
        );

        if (pendingStudents.isEmpty()) {
            throw new BusinessException("没有待分配的学生");
        }

        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(Building::getStatus, 1)
        );

        Map<Long, List<Room>> buildingRoomsMap = new HashMap<>();
        Map<Long, List<Bed>> roomBedsMap = new HashMap<>();

        for (Building building : buildings) {
            List<Room> rooms = roomMapper.selectList(
                    new LambdaQueryWrapper<Room>()
                            .eq(Room::getBuildingId, building.getId())
                            .eq(Room::getStatus, 1)
            );
            buildingRoomsMap.put(building.getId(), rooms);

            for (Room room : rooms) {
                List<Bed> beds = bedMapper.selectList(
                        new LambdaQueryWrapper<Bed>()
                                .eq(Bed::getRoomId, room.getId())
                                .eq(Bed::getStatus, 0)
                );
                roomBedsMap.put(room.getId(), beds);
            }
        }

        List<AllocationResultDTO> results = new ArrayList<>();
        List<PendingStudent> unassigned = new ArrayList<>();

        Map<Integer, List<PendingStudent>> genderGroups = pendingStudents.stream()
                .collect(Collectors.groupingBy(PendingStudent::getGender));

        for (Map.Entry<Integer, List<PendingStudent>> entry : genderGroups.entrySet()) {
            Integer gender = entry.getKey();
            List<PendingStudent> students = entry.getValue();

            Building targetBuilding = buildings.stream()
                    .filter(b -> b.getGender().equals(gender))
                    .findFirst()
                    .orElse(null);

            if (targetBuilding == null) {
                for (PendingStudent s : students) {
                    AllocationResultDTO dto = createAllocationDTO(s, null, null, null, "没有找到对应性别的楼栋");
                    results.add(dto);
                }
                continue;
            }

            List<Room> rooms = buildingRoomsMap.get(targetBuilding.getId());
            if (rooms == null || rooms.isEmpty()) {
                for (PendingStudent s : students) {
                    AllocationResultDTO dto = createAllocationDTO(s, targetBuilding, null, null, "该楼栋没有可用房间");
                    results.add(dto);
                }
                continue;
            }

            Map<String, List<PendingStudent>> majorGroups = students.stream()
                    .collect(Collectors.groupingBy(s -> s.getMajor() != null ? s.getMajor() : ""));

            for (Map.Entry<String, List<PendingStudent>> majorEntry : majorGroups.entrySet()) {
                String major = majorEntry.getKey();
                List<PendingStudent> majorStudents = majorEntry.getValue();

                majorStudents.sort((a, b) -> {
                    int smokerCompare = Integer.compare(b.getIsSmoker(), a.getIsSmoker());
                    if (smokerCompare != 0) return smokerCompare;
                    if (a.getSleepPreference() == null) return 1;
                    if (b.getSleepPreference() == null) return -1;
                    return a.getSleepPreference().compareTo(b.getSleepPreference());
                });

                for (PendingStudent student : majorStudents) {
                    boolean assigned = false;
                    for (Room room : rooms) {
                        List<Bed> beds = roomBedsMap.get(room.getId());
                        if (beds == null || beds.isEmpty()) continue;

                        Bed bed = beds.remove(0);
                        List<String> reasons = new ArrayList<>();
                        reasons.add("性别匹配");

                        if (StrUtil.isNotBlank(major)) {
                            reasons.add("同专业优先");
                        }

                        if (student.getIsSmoker() == 1) {
                            reasons.add("吸烟学生集中");
                        }

                        if (StrUtil.isNotBlank(student.getSleepPreference())) {
                            reasons.add("作息偏好匹配");
                        }

                        AllocationResultDTO dto = createAllocationDTO(student, targetBuilding, room, bed, String.join("、", reasons));
                        results.add(dto);
                        assigned = true;
                        break;
                    }

                    if (!assigned) {
                        unassigned.add(student);
                    }
                }
            }
        }

        for (PendingStudent s : unassigned) {
            Building building = buildings.stream()
                    .filter(b -> b.getGender().equals(s.getGender()))
                    .findFirst()
                    .orElse(null);
            AllocationResultDTO dto = createAllocationDTO(s, building, null, null, "没有空余床位");
            results.add(dto);
        }

        return results;
    }

    private AllocationResultDTO createAllocationDTO(PendingStudent student, Building building, Room room, Bed bed, String reason) {
        AllocationResultDTO dto = new AllocationResultDTO();
        dto.setPendingStudentId(student.getId());
        dto.setStudentNo(student.getStudentNo());
        dto.setStudentName(student.getName());
        dto.setGender(student.getGender());
        dto.setMajor(student.getMajor());
        dto.setSleepPreference(student.getSleepPreference());
        dto.setIsSmoker(student.getIsSmoker());
        if (building != null) {
            dto.setBuildingId(building.getId());
            dto.setBuildingName(building.getName());
        }
        if (room != null) {
            dto.setRoomId(room.getId());
            dto.setRoomNumber(room.getRoomNumber());
        }
        if (bed != null) {
            dto.setBedId(bed.getId());
            dto.setBedNumber(bed.getBedNumber());
        }
        dto.setMatchReason(reason);
        return dto;
    }

    @Transactional
    public void confirmAllocation(List<AllocationResultDTO> results) {
        List<AllocationResultDTO> validResults = results.stream()
                .filter(r -> r.getBedId() != null)
                .collect(Collectors.toList());

        for (AllocationResultDTO result : validResults) {
            PendingStudent pending = pendingStudentMapper.selectById(result.getPendingStudentId());
            if (pending == null) continue;

            Student student = new Student();
            student.setStudentNo(pending.getStudentNo());
            student.setName(pending.getName());
            student.setGender(pending.getGender());
            student.setMajor(pending.getMajor());
            student.setPhone(pending.getPhone());
            student.setEnrollDate(LocalDate.now());

            SysUser user = new SysUser();
            user.setUsername(pending.getStudentNo());
            user.setPassword(BCrypt.hashpw("123456"));
            user.setRealName(pending.getName());
            user.setPhone(pending.getPhone());
            user.setRole(3);
            user.setStatus(1);
            userMapper.insert(user);
            student.setUserId(user.getId());

            studentMapper.insert(student);

            Bed bed = bedMapper.selectById(result.getBedId());
            bed.setStudentId(student.getId());
            bed.setStatus(1);
            bedMapper.updateById(bed);

            Room room = roomMapper.selectById(result.getRoomId());
            room.setCurrentCount(room.getCurrentCount() + 1);
            roomMapper.updateById(room);

            pending.setStatus(1);
            pendingStudentMapper.updateById(pending);
        }
    }

    public byte[] downloadTemplate() throws IOException {
        List<StudentImportDTO> templateData = new ArrayList<>();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, StudentImportDTO.class)
                    .sheet("学生导入模板")
                    .doWrite(templateData);
            return out.toByteArray();
        }
    }

    public byte[] downloadSampleData() throws IOException {
        List<StudentImportDTO> sampleData = generateSampleData();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, StudentImportDTO.class)
                    .sheet("新生导入示例")
                    .doWrite(sampleData);
            return out.toByteArray();
        }
    }

    private List<StudentImportDTO> generateSampleData() {
        List<StudentImportDTO> list = new ArrayList<>();
        String[] majors = {"计算机科学与技术", "软件工程", "电子信息工程", "机械工程", "土木工程"};
        String[] sleeps = {"早睡型", "晚睡型"};
        String[] smokers = {"是", "否"};

        for (int i = 1; i <= 20; i++) {
            StudentImportDTO dto = new StudentImportDTO();
            dto.setStudentNo(String.format("2024%04d", i));
            dto.setName("学生" + i);
            dto.setGender(i % 2 == 1 ? "男" : "女");
            dto.setMajor(majors[i % majors.length]);
            dto.setPhone("138" + String.format("%08d", 10000000 + i));
            dto.setSleepPreference(sleeps[i % sleeps.length]);
            dto.setIsSmoker(smokers[i % smokers.length]);
            list.add(dto);
        }
        return list;
    }

    public void clearPendingStudents() {
        pendingStudentMapper.delete(null);
    }
}
