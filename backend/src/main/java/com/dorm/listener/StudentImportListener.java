package com.dorm.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.dorm.dto.StudentImportDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentImportListener implements ReadListener<StudentImportDTO> {

    private final List<StudentImportDTO> dataList = new ArrayList<>();

    @Override
    public void invoke(StudentImportDTO data, AnalysisContext context) {
        if (data != null) {
            dataList.add(data);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
    }
}
