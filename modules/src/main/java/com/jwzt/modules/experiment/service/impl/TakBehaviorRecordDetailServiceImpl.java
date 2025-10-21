package com.jwzt.modules.experiment.service.impl;

import java.util.List;

import com.jwzt.modules.experiment.vo.TakBehaviorRecordDetailVo;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jwzt.modules.experiment.mapper.TakBehaviorRecordDetailMapper;
import com.jwzt.modules.experiment.domain.TakBehaviorRecordDetail;
import com.jwzt.modules.experiment.service.ITakBehaviorRecordDetailService;

/**
 * 行为记录详情Service业务层处理
 * 
 * @author lx
 * @date 2025-07-09
 */
@Service
public class TakBehaviorRecordDetailServiceImpl implements ITakBehaviorRecordDetailService 
{
    @Autowired
    private TakBehaviorRecordDetailMapper takBehaviorRecordDetailMapper;

    /**
     * 查询行为记录详情
     * 
     * @param id 行为记录详情主键
     * @return 行为记录详情
     */
    @Override
    public TakBehaviorRecordDetail selectTakBehaviorRecordDetailById(Long id)
    {
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailById(id);
    }

    /**
     * 查询行为记录详情列表
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 行为记录详情
     */
    @Override
    public List<TakBehaviorRecordDetail> selectTakBehaviorRecordDetailList(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailList(takBehaviorRecordDetail);
    }

    /**
     * 新增行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    @Override
    public int insertTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        takBehaviorRecordDetail.setCreateTime(DateUtils.getNowDate());
        takBehaviorRecordDetail.setId(IdUtils.snowflakeId());
        return takBehaviorRecordDetailMapper.insertTakBehaviorRecordDetail(takBehaviorRecordDetail);
    }

    @Override
    public void insertTakBehaviorRecordDetailAll(List<TakBehaviorRecordDetail> takBehaviorRecordDetailList){
        if (takBehaviorRecordDetailList == null || takBehaviorRecordDetailList.isEmpty()) {
            return;
        }
        
        // 设置ID和创建时间
        takBehaviorRecordDetailList.forEach(takBehaviorRecordDetail -> {
            takBehaviorRecordDetail.setCreateTime(DateUtils.getNowDate());
            takBehaviorRecordDetail.setId(IdUtils.snowflakeId());
        });
        
        // 分批插入，避免超过数据库参数限制（达梦数据库最大参数65535）
        // 每个点10个字段，安全起见每批最多1000个点（10000个参数）
        final int BATCH_SIZE = 1000;
        int totalSize = takBehaviorRecordDetailList.size();
        
        if (totalSize <= BATCH_SIZE) {
            // 数据量小，直接插入
            takBehaviorRecordDetailMapper.insertTakBehaviorRecordDetailAll(takBehaviorRecordDetailList);
        } else {
            // 数据量大，分批插入
            int batchCount = (int) Math.ceil((double) totalSize / BATCH_SIZE);
            System.out.println("📊 轨迹点位数量: " + totalSize + "，分 " + batchCount + " 批插入数据库");
            
            for (int i = 0; i < totalSize; i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, totalSize);
                List<TakBehaviorRecordDetail> batch = takBehaviorRecordDetailList.subList(i, endIndex);
                
                try {
                    takBehaviorRecordDetailMapper.insertTakBehaviorRecordDetailAll(batch);
                    System.out.println("✓ 第 " + ((i / BATCH_SIZE) + 1) + "/" + batchCount + " 批插入成功: " + batch.size() + " 条");
                } catch (Exception e) {
                    System.err.println("数据库异常日志 ❌ 第 " + ((i / BATCH_SIZE) + 1) + " 批插入失败: " + e.getMessage());
                    throw e; // 重新抛出异常，保证事务回滚
                }
            }
        }
    }

    /**
     * 修改行为记录详情
     * 
     * @param takBehaviorRecordDetail 行为记录详情
     * @return 结果
     */
    @Override
    public int updateTakBehaviorRecordDetail(TakBehaviorRecordDetail takBehaviorRecordDetail)
    {
        takBehaviorRecordDetail.setUpdateTime(DateUtils.getNowDate());
        return takBehaviorRecordDetailMapper.updateTakBehaviorRecordDetail(takBehaviorRecordDetail);
    }

    /**
     * 批量删除行为记录详情
     * 
     * @param ids 需要删除的行为记录详情主键
     * @return 结果
     */
    @Override
    public int deleteTakBehaviorRecordDetailByIds(Long[] ids)
    {
        return takBehaviorRecordDetailMapper.deleteTakBehaviorRecordDetailByIds(ids);
    }

    /**
     * 删除行为记录详情信息
     * 
     * @param id 行为记录详情主键
     * @return 结果
     */
    @Override
    public int deleteTakBehaviorRecordDetailById(Long id)
    {
        return takBehaviorRecordDetailMapper.deleteTakBehaviorRecordDetailById(id);
    }

    public void deleteByCreationTime(String s){
        takBehaviorRecordDetailMapper.deleteByCreationTime(s);
    }

    public List<TakBehaviorRecordDetailVo> selectTakBehaviorRecordDetailListByUserId(TakBehaviorRecordDetail takBehaviorRecordDetail){
        return takBehaviorRecordDetailMapper.selectTakBehaviorRecordDetailListByUserId(takBehaviorRecordDetail);
    }
}
