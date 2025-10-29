package com.jwzt.modules.experiment.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.jwzt.modules.experiment.domain.TakRfidRecord;
import com.jwzt.modules.experiment.mapper.TakRfidRecordMapper;
import com.jwzt.modules.experiment.service.ITakRfidRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RFID识别记录Service业务层处理
 * 
 * @author lx
 * @date 2025-01-20
 */
@Service
public class TakRfidRecordServiceImpl implements ITakRfidRecordService {
    @Autowired
    private TakRfidRecordMapper takRfidRecordMapper;

    @Override
    public TakRfidRecord selectTakRfidRecordById(Long id) {
        return takRfidRecordMapper.selectTakRfidRecordById(id);
    }

    @Override
    public List<TakRfidRecord> selectTakRfidRecordList(TakRfidRecord takRfidRecord) {
        return takRfidRecordMapper.selectTakRfidRecordList(takRfidRecord);
    }

    @Override
    public int insertTakRfidRecord(TakRfidRecord takRfidRecord) {
        if (takRfidRecord.getId() == null) {
            takRfidRecord.setId(IdUtils.snowflakeId());
        }
        takRfidRecord.setCreateTime(DateUtils.getNowDate());
        return takRfidRecordMapper.insertTakRfidRecord(takRfidRecord);
    }

    @Override
    public int batchInsertTakRfidRecord(List<TakRfidRecord> takRfidRecordList) {
        if (takRfidRecordList == null || takRfidRecordList.isEmpty()) {
            return 0;
        }
        for (TakRfidRecord record : takRfidRecordList) {
            if (record.getId() == null) {
                record.setId(IdUtils.snowflakeId());
            }
            if (record.getCreateTime() == null) {
                record.setCreateTime(DateUtils.getNowDate());
            }
        }
        return takRfidRecordMapper.batchInsertTakRfidRecord(takRfidRecordList);
    }

    @Override
    public int updateTakRfidRecord(TakRfidRecord takRfidRecord) {
        takRfidRecord.setUpdateTime(DateUtils.getNowDate());
        return takRfidRecordMapper.updateTakRfidRecord(takRfidRecord);
    }

    @Override
    public int batchUpdateMatchStatus(List<TakRfidRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        for (TakRfidRecord record : records) {
            record.setUpdateTime(DateUtils.getNowDate());
        }
        return takRfidRecordMapper.batchUpdateMatchStatus(records);
    }

    @Override
    public int deleteTakRfidRecordById(Long id) {
        return takRfidRecordMapper.deleteTakRfidRecordById(id);
    }

    @Override
    public int deleteTakRfidRecordByIds(Long[] ids) {
        return takRfidRecordMapper.deleteTakRfidRecordByIds(ids);
    }

    @Override
    public List<TakRfidRecord> selectTakRfidRecordByTimeRange(String startTime, String endTime, Integer matchStatus) {
        return takRfidRecordMapper.selectTakRfidRecordByTimeRange(startTime, endTime, matchStatus);
    }
}

