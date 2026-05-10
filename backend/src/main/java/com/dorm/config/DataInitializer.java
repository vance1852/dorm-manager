package com.dorm.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dorm.entity.*;
import com.dorm.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final BedMapper bedMapper;
    private final StudentMapper studentMapper;
    private final RepairRequestMapper repairMapper;
    private final VisitorRecordMapper visitorMapper;
    private final AnnouncementMapper announcementMapper;

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            log.info("数据库已有数据，跳过初始化");
            return;
        }
        log.info("开始初始化数据...");
        initUsers();
        initBuildings();
        initRooms();
        initBeds();
        initStudents();
        initRepairs();
        initVisitors();
        initAnnouncements();
        log.info("数据初始化完成！");
    }

    private void initUsers() {
        String pwd = BCrypt.hashpw("123456");
        List<SysUser> users = Arrays.asList(
            createUser("admin", pwd, "系统管理员", "13800000001", 1),
            createUser("sg001", pwd, "张宿管", "13800000002", 2),
            createUser("sg002", pwd, "李宿管", "13800000003", 2)
        );
        users.forEach(userMapper::insert);
    }

    private SysUser createUser(String username, String password, String realName, String phone, int role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private void initBuildings() {
        List<Building> buildings = Arrays.asList(
            createBuilding("梅园1号楼", "男生宿舍，共6层，每层20间房", 1, 2L),
            createBuilding("梅园2号楼", "男生宿舍，共6层，每层20间房", 1, 2L),
            createBuilding("兰园1号楼", "女生宿舍，共6层，每层20间房", 2, 3L),
            createBuilding("兰园2号楼", "女生宿舍，共6层，每层20间房", 2, 3L),
            createBuilding("竹园1号楼", "研究生宿舍，共8层，每层15间房", 1, 2L)
        );
        buildings.forEach(buildingMapper::insert);
    }

    private Building createBuilding(String name, String desc, int gender, Long managerId) {
        Building b = new Building();
        b.setName(name);
        b.setDescription(desc);
        b.setGender(gender);
        b.setManagerId(managerId);
        b.setStatus(1);
        return b;
    }

    private void initRooms() {
        int[][] roomData = {
            {1, 101, 1, 4}, {1, 102, 1, 4}, {1, 103, 1, 4}, {1, 104, 1, 4}, {1, 105, 1, 4},
            {1, 201, 2, 4}, {1, 202, 2, 4}, {1, 203, 2, 4}, {1, 204, 2, 4}, {1, 205, 2, 4},
            {1, 301, 3, 4}, {1, 302, 3, 4}, {1, 303, 3, 4}, {1, 304, 3, 4}, {1, 305, 3, 4},
            {1, 401, 4, 4}, {1, 402, 4, 4}, {1, 403, 4, 4}, {1, 404, 4, 4}, {1, 405, 4, 4},
            {2, 101, 1, 4}, {2, 102, 1, 4}, {2, 103, 1, 4}, {2, 104, 1, 4}, {2, 105, 1, 4},
            {2, 201, 2, 4}, {2, 202, 2, 4}, {2, 203, 2, 4}, {2, 204, 2, 4}, {2, 205, 2, 4},
            {3, 101, 1, 4}, {3, 102, 1, 4}, {3, 103, 1, 4}, {3, 104, 1, 4}, {3, 105, 1, 4},
            {3, 201, 2, 4}, {3, 202, 2, 4}, {3, 203, 2, 4}, {3, 204, 2, 4}, {3, 205, 2, 4},
            {4, 101, 1, 4}, {4, 102, 1, 4}, {4, 103, 1, 4}, {4, 104, 1, 4}, {4, 105, 1, 4},
            {4, 201, 2, 4}, {4, 202, 2, 4}, {4, 203, 2, 4}, {4, 204, 2, 4}, {4, 205, 2, 4},
            {5, 101, 1, 2}, {5, 102, 1, 2}, {5, 103, 1, 2}, {5, 104, 1, 2}, {5, 105, 1, 2}
        };
        for (int[] r : roomData) {
            Room room = new Room();
            room.setBuildingId((long) r[0]);
            room.setRoomNumber(String.valueOf(r[1]));
            room.setFloor(r[2]);
            room.setCapacity(r[3]);
            room.setCurrentCount(0);
            room.setStatus(1);
            roomMapper.insert(room);
        }
    }

    private void initBeds() {
        List<Room> rooms = roomMapper.selectList(null);
        for (Room room : rooms) {
            for (int i = 1; i <= room.getCapacity(); i++) {
                Bed bed = new Bed();
                bed.setRoomId(room.getId());
                bed.setBedNumber(String.valueOf(i));
                bed.setStatus(0);
                bedMapper.insert(bed);
            }
        }
    }

    private void initStudents() {
        String pwd = BCrypt.hashpw("123456");
        String[][] maleStudents = {
            {"2021001", "张伟", "计算机学院", "软件工程", "软件2101班"},
            {"2021002", "王强", "计算机学院", "软件工程", "软件2101班"},
            {"2021003", "李明", "计算机学院", "计算机科学", "计科2101班"},
            {"2021004", "刘洋", "计算机学院", "计算机科学", "计科2101班"},
            {"2021005", "陈浩", "电子信息学院", "电子信息", "电信2101班"},
            {"2021006", "杨帆", "电子信息学院", "电子信息", "电信2101班"},
            {"2021007", "赵磊", "电子信息学院", "通信工程", "通信2101班"},
            {"2021008", "黄涛", "电子信息学院", "通信工程", "通信2101班"},
            {"2021009", "周杰", "机械工程学院", "机械设计", "机械2101班"},
            {"2021010", "吴鹏", "机械工程学院", "机械设计", "机械2101班"}
        };
        String[][] femaleStudents = {
            {"2022001", "王芳", "计算机学院", "软件工程", "软件2201班"},
            {"2022002", "李娜", "计算机学院", "软件工程", "软件2201班"},
            {"2022003", "张敏", "计算机学院", "计算机科学", "计科2201班"},
            {"2022004", "刘婷", "计算机学院", "计算机科学", "计科2201班"},
            {"2022005", "陈静", "电子信息学院", "电子信息", "电信2201班"},
            {"2022006", "杨丽", "电子信息学院", "电子信息", "电信2201班"},
            {"2022007", "赵雪", "电子信息学院", "通信工程", "通信2201班"},
            {"2022008", "黄燕", "电子信息学院", "通信工程", "通信2201班"}
        };
        
        List<Bed> maleBeds = bedMapper.selectList(new LambdaQueryWrapper<Bed>()
            .inSql(Bed::getRoomId, "SELECT id FROM room WHERE building_id IN (1,2)").last("LIMIT 10"));
        List<Bed> femaleBeds = bedMapper.selectList(new LambdaQueryWrapper<Bed>()
            .inSql(Bed::getRoomId, "SELECT id FROM room WHERE building_id IN (3,4)").last("LIMIT 8"));
        
        for (int i = 0; i < maleStudents.length; i++) {
            String[] s = maleStudents[i];
            SysUser user = createUser(s[0], pwd, s[1], "139" + String.format("%08d", i + 1), 3);
            userMapper.insert(user);
            
            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNo(s[0]);
            student.setName(s[1]);
            student.setCollege(s[2]);
            student.setMajor(s[3]);
            student.setClassName(s[4]);
            student.setGender(1);
            student.setPhone(user.getPhone());
            student.setEnrollDate(LocalDate.of(2021, 9, 1));
            studentMapper.insert(student);
            
            if (i < maleBeds.size()) {
                Bed bed = maleBeds.get(i);
                bed.setStudentId(student.getId());
                bed.setStatus(1);
                bedMapper.updateById(bed);
                Room room = roomMapper.selectById(bed.getRoomId());
                room.setCurrentCount(room.getCurrentCount() + 1);
                roomMapper.updateById(room);
            }
        }
        
        for (int i = 0; i < femaleStudents.length; i++) {
            String[] s = femaleStudents[i];
            SysUser user = createUser(s[0], pwd, s[1], "139" + String.format("%08d", i + 11), 3);
            userMapper.insert(user);
            
            Student student = new Student();
            student.setUserId(user.getId());
            student.setStudentNo(s[0]);
            student.setName(s[1]);
            student.setCollege(s[2]);
            student.setMajor(s[3]);
            student.setClassName(s[4]);
            student.setGender(2);
            student.setPhone(user.getPhone());
            student.setEnrollDate(LocalDate.of(2022, 9, 1));
            studentMapper.insert(student);
            
            if (i < femaleBeds.size()) {
                Bed bed = femaleBeds.get(i);
                bed.setStudentId(student.getId());
                bed.setStatus(1);
                bedMapper.updateById(bed);
                Room room = roomMapper.selectById(bed.getRoomId());
                room.setCurrentCount(room.getCurrentCount() + 1);
                roomMapper.updateById(room);
            }
        }
    }

    private void initRepairs() {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>().last("LIMIT 5"));
        if (students.isEmpty()) return;
        
        String[][] repairs = {
            {"空调不制冷", "宿舍空调开了很久都不制冷，室内温度很高，影响正常休息。", "2", "已派维修人员处理，更换了空调压缩机。"},
            {"水龙头漏水", "洗手台水龙头一直滴水，浪费水资源。", "1", "已安排维修，预计明天处理。"},
            {"灯管不亮", "宿舍主灯不亮了，晚上学习很不方便。", "0", null},
            {"门锁损坏", "宿舍门锁有问题，钥匙很难插进去。", "0", null},
            {"热水器故障", "热水器打不着火，无法使用热水。", "2", "已更换热水器点火器，现已恢复正常。"}
        };
        
        for (int i = 0; i < Math.min(repairs.length, students.size()); i++) {
            Student student = students.get(i);
            Bed bed = bedMapper.selectOne(new LambdaQueryWrapper<Bed>().eq(Bed::getStudentId, student.getId()));
            if (bed == null) continue;
            
            RepairRequest repair = new RepairRequest();
            repair.setStudentId(student.getId());
            repair.setRoomId(bed.getRoomId());
            repair.setTitle(repairs[i][0]);
            repair.setDescription(repairs[i][1]);
            repair.setStatus(Integer.parseInt(repairs[i][2]));
            repair.setReply(repairs[i][3]);
            repairMapper.insert(repair);
        }
    }

    private void initVisitors() {
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>().last("LIMIT 4"));
        if (students.isEmpty()) return;
        
        String[][] visitors = {
            {"张父", "13800001111", "探望孩子", "1"},
            {"李母", "13800002222", "送生活用品", "1"},
            {"王朋友", "13800003333", "同学聚会", "0"},
            {"刘姐姐", "13800004444", "探望妹妹", "0"}
        };
        
        for (int i = 0; i < Math.min(visitors.length, students.size()); i++) {
            VisitorRecord visitor = new VisitorRecord();
            visitor.setStudentId(students.get(i).getId());
            visitor.setVisitorName(visitors[i][0]);
            visitor.setVisitorPhone(visitors[i][1]);
            visitor.setReason(visitors[i][2]);
            visitor.setVisitTime(LocalDateTime.now().minusDays(i));
            visitor.setStatus(Integer.parseInt(visitors[i][3]));
            if (visitor.getStatus() == 1) {
                visitor.setLeaveTime(visitor.getVisitTime().plusHours(3));
            }
            visitorMapper.insert(visitor);
        }
    }

    private void initAnnouncements() {
        String[][] announcements = {
            {"欢迎使用宿舍管理系统", "亲爱的同学们，欢迎使用宿舍管理系统！本系统提供宿舍管理、维修申请、访客登记等功能，请合理使用。", "1"},
            {"关于加强宿舍安全管理的通知", "为保障同学们的人身和财产安全，请注意：1.离开宿舍时请锁好门窗；2.不要在宿舍使用大功率电器；3.不要留宿外来人员。", "2"},
            {"宿舍文化节活动报名开始", "一年一度的宿舍文化节即将开始！活动包括：最美宿舍评选、宿舍才艺大赛、宿舍美食节等。欢迎各宿舍积极报名参加！", "3"},
            {"春季学期宿舍卫生检查通知", "根据学校安排，将于近期开始进行春季学期宿舍卫生大检查。请各宿舍做好准备，保持宿舍整洁卫生。", "1"},
            {"关于宿舍热水供应时间调整的通知", "因设备维护需要，热水供应时间临时调整为：早6:00-8:00，晚18:00-23:00。给大家带来不便，敬请谅解。", "1"}
        };
        
        for (String[] a : announcements) {
            Announcement announcement = new Announcement();
            announcement.setTitle(a[0]);
            announcement.setContent(a[1]);
            announcement.setPublisherId(1L);
            announcement.setType(Integer.parseInt(a[2]));
            announcement.setStatus(1);
            announcementMapper.insert(announcement);
        }
    }
}
