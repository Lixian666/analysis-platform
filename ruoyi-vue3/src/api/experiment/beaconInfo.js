import request from '@/utils/request'

// 查询信标信息列表
export function listBeaconInfo(query) {
  return request({
    url: '/experiment/beaconInfo/list',
    method: 'get',
    params: query
  })
}

// 查询信标ID列表
export function listBeaconId() {
  return request({
    url: '/experiment/beaconInfo/beaconIds',
    method: 'get'
  })
}

// 查询信标信息详细
export function getBeaconInfo(id) {
  return request({
    url: '/experiment/beaconInfo/' + id,
    method: 'get'
  })
}

// 新增信标信息
export function addBeaconInfo(data) {
  return request({
    url: '/experiment/beaconInfo',
    method: 'post',
    data: data
  })
}

// 修改信标信息
export function updateBeaconInfo(data) {
  return request({
    url: '/experiment/beaconInfo',
    method: 'put',
    data: data
  })
}

// 删除信标信息
export function delBeaconInfo(id) {
  return request({
    url: '/experiment/beaconInfo/' + id,
    method: 'delete'
  })
}

// 下载信标信息导入模板
export function importTemplate() {
  return request({
    url: '/experiment/beaconInfo/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}
