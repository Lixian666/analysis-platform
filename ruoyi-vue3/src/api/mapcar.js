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


export const getexperimentuserlist = () => {
  return request({
    url: '/experiment/experiment/userList',
    method: 'get'
  })
}
export const getlistByUserId = (data) => {
  return request({
    url: '/experiment/detail/listByUserId',
    method: 'post',
    data: data
  })
}



// export const getexperimentid = (id) => {
//   return request({
//     url: '/experiment/experiment/' + id,
//     method: 'get'
//   })
// }
