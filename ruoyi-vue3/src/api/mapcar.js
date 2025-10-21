import request from '@/utils/request'
// 获取路由
export const getexperimentlist = () => {
  return request({
    url: '/experiment/experiment/list',
    method: 'get'
  })
}
export const getexperimentdetail = (id) => {
  return request({
    url: '/experiment/detail/list?trackId=' + id,
    method: 'get'
  })
}
export const getexperimentid = (id) => {
  return request({
    url: '/experiment/experiment/' + id,
    method: 'get'
  })
}
///experiment/experiment/list


export const getexperimentuserlist = (params) => {
  return request({
    url: '/experiment/experiment/userList',
    method: 'get',
    params: params
  })
}
export const getlistByUserId = (data) => {
  return request({
    url: '/experiment/detail/listByUserId',
    method: 'post',
    data: data
  })
}

// 删除行为记录（按条件删除）
export const deleteBehaviorRecords = (data) => {
  return request({
    url: '/experiment/experiment/delete',
    method: 'post',
    data: data
  })
}

// 删除行为记录（按ID删除）
export const deleteBehaviorRecordsByIds = (ids) => {
  return request({
    url: '/experiment/experiment/deleteByIds',
    method: 'post',
    data: ids
  })
}

// export const getexperimentid = (id) => {
//   return request({
//     url: '/experiment/experiment/' + id,
//     method: 'get'
//   })
// }
