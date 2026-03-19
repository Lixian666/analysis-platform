import request from '@/utils/request'

// 查询当前货场启用的卡ID列表
export function getEnabledCardIds(yardId) {
  return request({
    url: '/experiment/locationInfo/enabledCardIds',
    method: 'get',
    params: yardId ? { yardId } : {}
  })
}

// 导出点位数据为JSON文件
export function exportPoints(data) {
  return request({
    url: '/experiment/locationInfo/exportPoints',
    method: 'post',
    params: data,
    responseType: 'blob'
  })
}
