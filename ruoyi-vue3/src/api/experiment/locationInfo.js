import request from '@/utils/request'

// 导出点位数据为JSON文件
export function exportPoints(data) {
  return request({
    url: '/experiment/locationInfo/exportPoints',
    method: 'post',
    params: data,
    responseType: 'blob'
  })
}
