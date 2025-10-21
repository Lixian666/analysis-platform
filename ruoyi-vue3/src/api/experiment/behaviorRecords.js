import request from '@/utils/request'

// 查询行为记录列表
export function listBehaviorRecords(query) {
  return request({
    url: '/experiment/experiment/list',
    method: 'get',
    params: query
  })
}

// 查询行为记录用户列表
export function listBehaviorUserRecords(query) {
  return request({
    url: '/experiment/experiment/userList',
    method: 'get',
    params: query
  })
}

// 查询行为记录详细
export function getBehaviorRecords(id) {
  return request({
    url: '/experiment/experiment/' + id,
    method: 'get'
  })
}

// 新增行为记录
export function addBehaviorRecords(data) {
  return request({
    url: '/experiment/experiment',
    method: 'post',
    data: data
  })
}

// 修改行为记录
export function updateBehaviorRecords(data) {
  return request({
    url: '/experiment/experiment',
    method: 'put',
    data: data
  })
}

// 删除行为记录
export function delBehaviorRecords(id) {
  return request({
    url: '/experiment/experiment/' + id,
    method: 'delete'
  })
}

// 导出行为记录
export function exportBehaviorRecords(query) {
  return request({
    url: '/experiment/experiment/export',
    method: 'post',
    params: query,
    responseType: 'blob'
  })
}

// 下载行为记录导入模板
export function importBehaviorRecordsTemplate() {
  return request({
    url: '/experiment/experiment/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}

// 导入行为记录数据
export function importBehaviorRecords(data) {
  return request({
    url: '/experiment/experiment/importData',
    method: 'post',
    data: data
  })
}
