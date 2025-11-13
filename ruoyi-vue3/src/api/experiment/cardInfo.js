import request from '@/utils/request'

// 查询定位卡信息列表
export function listCardInfo(query) {
  return request({
    url: '/experiment/cardInfo/list',
    method: 'get',
    params: query
  })
}

// 下载定位卡导入模板
export function importTemplate() {
  return request({
    url: '/experiment/cardInfo/importTemplate',
    method: 'get',
    responseType: 'blob'
  })
}

// 查询定位卡信息详细
export function getCardInfo(id) {
  return request({
    url: '/experiment/cardInfo/' + id,
    method: 'get'
  })
}

// 新增定位卡信息
export function addCardInfo(data) {
  return request({
    url: '/experiment/cardInfo',
    method: 'post',
    data: data
  })
}

// 修改定位卡信息
export function updateCardInfo(data) {
  return request({
    url: '/experiment/cardInfo',
    method: 'put',
    data: data
  })
}

// 修改定位卡启用状态
export function changeCardStatus(data) {
  return request({
    url: '/experiment/cardInfo/status',
    method: 'put',
    data: data
  })
}

// 删除定位卡信息
export function delCardInfo(id) {
  return request({
    url: '/experiment/cardInfo/' + id,
    method: 'delete'
  })
}

