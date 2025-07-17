<template>
  <div v-if="diagnosisRoles('order:list')" id="body-box">

         <div ref="conment" class="CommercialVehicle" v-if="false">
           <div class="LibraryLocationName">
             <p>VIN码：</p>
             <el-input
               v-model="listQuery.searcher.vehicleCode"
               placeholder="VIN码"
               maxlength="50"
               @input="vehicleCodechange"
             />
           </div>
           <div class="ArrivalTime">
             <p>上车时间：</p>
             <el-date-picker
               v-model="listQuery.searcher.arriveTime"
               type="datetimerange"
               range-separator="至"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
             />
           </div>
           <div class="StartTime">
             <p>下车时间：</p>
             <el-date-picker
               v-model="listQuery.searcher.leaveTime"
               type="datetimerange"
               range-separator="至"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
             />
           </div>

           <div class="State">
             <p>库区：</p>
             <el-select
               v-model="listQuery.searcher.partitionId"
               placeholder="请选择库区"
             >
               <el-option
                 v-for="item in ReservoirAreaData"
                 :key="item.partitionId"
                 :label="item.partitionName"
                 :value="item.partitionId"
               />
             </el-select>
           </div>
           <div v-if="seares" class="TopRight TopRight_novw">
             <el-button type="success" @click="search">查询</el-button>
             <el-button type="info" @click="reset">重置</el-button>
           </div>
           <div v-if="!seares" class="TopRight TopRight_novw">
             <el-popover
               popper-class="searicon"
               placement="top"
               title=""
               width="70"
               trigger="hover"
               content="搜索"
             >
               <el-button slot="reference" class="littesea" @click="search">
                 <i class="el-icon-search" />
               </el-button>
             </el-popover>
             <el-popover
               popper-class="searicon"
               placement="top"
               title=""
               width="70"
               trigger="hover"
               content="重置"
             >
               <el-button slot="reference" class="littesea" @click="reset">
                 <i class="el-icon-refresh" />
               </el-button>
             </el-popover>

           </div>
         </div>
    <div v-loading="dataTable_loading" class="main main_novw" style="height: 800px;">
      <el-table :data="TaskList" border style="width: 100%" height="100%">
             <!-- <el-table-column align="center" label="卡ID" class="mainCell1">
               <template v-slot="scope">
                 <span class="ml10">{{ scope.row.cardId }}</span>
               </template>
             </el-table-column> -->
             <!-- <el-table-column align="center" label="VIN" class="mainCell2">
               <template v-slot="scope">
                 <span v-if="scope.row.vehicleCode " class="ml10">{{ scope.row.vehicleCode }}</span>
                 <span v-else class="ml10">-</span>
               </template>
             </el-table-column> -->
             <el-table-column align="center" label="上车时间">
               <template v-slot="scope">
                 <span v-if="scope.row.startTime">{{ scope.row.startTime }}</span>
                 <span v-else class="ml10">-</span>
               </template>
             </el-table-column>
             <el-table-column align="center" label="下车时间">
               <template v-slot="scope">
                 <span v-if="scope.row.endTime">{{ scope.row.endTime  }}</span>
                 <span v-else>-</span>
               </template>
             </el-table-column>
             <el-table-column align="center" label="时间间隔" width="120px">
               <template v-slot="scope">
                 <!-- <span :style="getTimeColor(scope.row.startTime, scope.row.endTime)" v-text="stopTimeLength(scope.row.duration)" /> -->
                  <span>{{ scope.row.duration }}</span>
               </template>
             </el-table-column>
             <el-table-column align="center" label="点数量">
               <template v-slot="scope">
                 <span v-if="scope.row.pointCount">
                   {{ scope.row.pointCount }}
                 </span>
                 <span v-else>-</span>
               </template>
             </el-table-column>

             <el-table-column align="center" label="类型" width="100px">
               <template v-slot="scope">
                 <span v-text="getcartype(scope.row.type)">
                 </span>
               </template>
             </el-table-column>

             <el-table-column align="center" label="状态" width="80px">
               <template v-slot="scope">
                 <span>
                   {{ scope.row.state }}
                 </span>
               </template>
             </el-table-column>
             <!-- <el-table-column align="center" label="颜色" width="80px">
               <template v-slot="scope">
                 <span v-if="scope.row.vehicleColor">{{ scope.row.vehicleColor }}</span>
                 <span v-else>-</span>
               </template>
             </el-table-column> -->
             <el-table-column align="center" label="操作" width="100">
               <template v-slot="scope">
                 <el-button
                   v-if="diagnosisRoles('vehicle:details') && scope.row.trackId"
                   size="mini"
                   @click="handleEdit(scope.row.trackId,scope.row.id)"
                 >详情</el-button>
                 <el-button
                   v-if="false && diagnosisRoles('vehicle:update') && scope.row.recordThirdId"
                   size="mini"
                   @click="updateRecord(scope.row.recordThirdId)"
                 >修改</el-button>
               </template>
             </el-table-column>
           </el-table>
         </div>
         <div class="paging">
           <pagination
             v-show="listQuery.total != 0"
             :total="listQuery.total"
             :page="listQuery.page"
             :limit="listQuery.limit"
             @pagination="getPagination"
           />
         </div>

     <add-assignment
       v-show="dialogVisible"
       ref="addAssignmentFrom"
       @refreshDataList="search()"
     />
   </div>
   <div v-else id="body-box">
     <div401 />
   </div>
 </template>

 <script setup>
  import div401 from '@/views/error/401.vue'
  import { getexperimentlist } from '@/api/mapcar.js'
  import { onMounted, ref } from "vue"
  const route = useRoute()
  const router = useRouter()
  //data return start
  const listQuery = ref({
    page: 1, // 当前表格显示第几页数据
    limit: 20, // 表格一页显示几条数据
    total: 0,
    searcher: {
      orderId: null,
      partitionId: null,
      positionId: null,
      vehicleCode: '',
      leaveStartTime: null,
      leaveEndTime: null,
      arriveStartTime: null,
      arriveEndTime: null,
      arriveTime: [], // 出发时间[new Date(2000, 10, 10, 10, 10), new Date(2000, 10, 11, 10, 10)]
      leaveTime: [] // 到达时间
    }
  })
  const TaskList = ref([
    // {
    //   recordThirdId:7845645646,
    //   vehicleCode :45464654646,
    //   startTime:'2025-07-09 17:01',
    //   endTime:'2025-07-09 18:00',
    //   partitionNames:'示例1',
    //   positionCodes:1,
    //   getcartype:1,
    //   recordStatusFun:1,
    //   vehicleColor:1,
    //   positionCodes:1,
    //   recordThirdId:1
    // }

  ])
  const dataTable_loading = ref(false)
  const ReservoirAreaData = ref([]) // 搜索选择库区
  const seares = ref(true)
  //data return end

  onMounted(()=>{
    init()
  })

  //methods start
  function getcartype(val){
    console.log('a',val)
    let data = val
    if (data === 0) {
      data = '到达卸车'
    } else if (data === 1 ){
      data = '发运装车'
    } else if (data === 2) {
      data = '轿运车装车'
    } else if (data === 3) {
      data = '轿运车卸车'
    } else if (data === 4) {
      data = '地跑入库'
    } else if (data === 5) {
      data = '地跑出库'
    } else {
      data = '无法识别'
    }
    return data
  }
  async function init(){
    let res = await getexperimentlist()
    console.log('ngsb',res)
    if((res.code == 200 || res.code == '200') && res.rows ){
      TaskList.value = res.rows
    }
  }
  function handleEdit(vehicleThirdId,id) {
    // 操作
   // router.push('/mapcar/vehiclePath-detail?id='+id+'&vehicleThirdId=' + vehicleThirdId )
    router.push({
      path: "/mapcar/vehiclePath-detail",
      query: {
        id:String(id),
        vehicleThirdId: vehicleThirdId,
      }
    })
  }
  function diagnosisRoles(text){
    return true
  }
  function getTimeColor(startTime, endTime) {
    var time = 0
    if (endTime !== null && startTime !== null) {
      time = new Date(endTime).getTime() - new Date(startTime).getTime()
    } else {
      if (startTime != null) {
        time = new Date().getTime() - new Date(startTime).getTime()
      }
    }
    if (time / 1000 > 86400) {
      return 'color:red;'
    } else {
      return ''
    }
  }
  function stopTimeLength(row) {
    var time = 0
    if (row.endTime !== null && row.startTime !== null) {
      time =
        new Date(row.endTime).getTime() - new Date(row.startTime).getTime()
    } else if (row.startTime !== null && row.endTime === null) {
      time = new Date().getTime() - new Date(row.startTime).getTime()
    }
    return formatTimeLong(time / 1000)
  }
  function formatTimeLong(num) {
    // 秒
    var secondTime = parseInt(num)
    var minuteTime = 0
    var hourTime = 0
    if (secondTime > 60) {
      minuteTime = parseInt(secondTime / 60)
      secondTime = parseInt(secondTime % 60)
      if (minuteTime >= 60) {
        hourTime = parseInt(minuteTime / 60)
        minuteTime = parseInt(minuteTime % 60)
      }
    }
    var s =
      parseInt(secondTime) < 10
        ? '0' + parseInt(secondTime)
        : parseInt(secondTime)
    var m =
      parseInt(minuteTime) < 10
        ? '0' + parseInt(minuteTime)
        : parseInt(minuteTime)
    var h =
      parseInt(hourTime) < 10 ? '0' + parseInt(hourTime) : parseInt(hourTime)
    var result = '00″00″' + s
    if (minuteTime > 0 && hourTime === 0) {
      result = '00″' + m + '″' + s
    } else {
      if (hourTime >= 0 && hourTime <= 24) {
        result = h + '″' + m + '″' + s
      } else {
        var dayTime = parseInt(hourTime / 24)
        hourTime = parseInt(hourTime % 24)
        result = dayTime + '天' + hourTime + '小时'
      }
    }
    return result
  }
  //methods end
 </script>

 <style scoped lang="scss">
 #body-box {
   // background-color: #04262b;
   height: calc(100vh - 84px);
   position: relative;
   .title {
     border-width: 0px;
     background-color: white;
     width: 100%;
     font-family: "微软雅黑", sans-serif;
     font-weight: 400;
     font-style: normal;
     text-align: left;
     border-bottom: 1px solid rgba(228, 228, 228, 1);
   }
 }
 //头部页签样式
 .TopTitle ::v-deep .el-tabs__item {
   color: #848c8e !important;
   font-size: 20px;
   height: 72px;
   line-height: 72px;
 }
 .TopTitle ::v-deep .el-tabs__item.is-active {
   //color: #8acc48 !important;
   //background-color: inherit;
   //border-color: #8acc48;
   color: #fff !important;
   background-color: rgba(39, 77, 249, 1);
   border-color: rgba(39, 77, 249, 1);
   font-weight: 400;
 }
 .TopTitle ::v-deep .el-tabs__active-bar {
   background-color: #8acc48;
   width: 5em !important;
   left: 48px;
 }
 .TopTitle ::v-deep .el-tabs__nav-wrap::after {
   background-color: #5d6777;
 }
 .TopTitle ::v-deep .el-tabs__nav {
   padding: 0 0 0 0px;
 }
 .TopTitle ::v-deep .el-tabs__header {
   background-color:transparent;
   margin: 0 0 10px;
   border: unset;
   display: none
 }
 .TopTitle_novw{
   height: 100%;
   border: unset !important;
   box-shadow: unset !important;
   ::v-deep{
     .el-tabs__header{
       .el-tabs__item{
         width: 80px;
         height: 35px;
         line-height: 35px;
         padding: 0 !important;
         margin: 10px 0 0 20px;
         font-size: 14px;
         background-color: rgba(246, 247, 247, 1);
         border: 1px solid rgba(228, 228, 228, 1);
         text-align: center;
         border-radius: 2px;
       }
       .is-active{
         border-color: rgba(39, 77, 249, 1);
         background-color: rgba(39, 77, 249, 1);
       }
     }
     .el-tabs__content{
       padding: 0;
       height: calc(100% - 55px);
       .el-tab-pane{
         height: 100%;
         .main_novw{
           height: calc(100% - 120px);
         }
       }
     }
   }
 }
 //头部页签结束
 // 查询开始
 .CommercialVehicle {
   padding: 0 20px 10px;
   margin: 5px 0 10px;
   //font-size: 13px;
   font-size: 14px;
   height: 36px;
   line-height: 36px;
   // border-bottom: 1px solid #5d6777;
   box-sizing: initial;
 }
 .CommercialVehicle p {
   color: #848c8e;
   padding: 0;
   margin: 0;
   text-align: center;
 }
 .CommercialVehicle ::v-deep input{
   //font-size: 13px;
   font-size: 14px;
 }

 .CommercialVehicle ::v-deep .el-range-separator{
   //font-size: 13px;
   font-size: 14px;
 }

 .CommercialVehicle ::v-deep .el-input__inner {
   // background: #001c20;
   height: 36px;
   line-height: 36px;
   border: 1px solid rgb(228, 228, 228);
   //padding:0;
 }
 .CommercialVehicle ::v-deep .el-input__inner::placeholder {
   color: #848c8e;
 }

 .CommercialVehicle ::v-deep .el-date-editor .el-input__icon{
   line-height: 26px !important;
 }

 .CommercialVehicle ::v-deep .el-date-editor .el-range-input {
   // background: #001c20;
   height: 33px;
   line-height: 33px;

   color: #848c8e;
 }
 .CommercialVehicle ::v-deep .el-date-editor .el-range-input::placeholder {
   color: #848c8e;
 }
 .CommercialVehicle ::v-deep .el-date-editor .el-range-separator {
   color: #848c8e;
   height: 36px;
   line-height: 36px;
 }
 .CommercialVehicle .el-select{
   width: 100px;
   ::v-deep input{
     padding: 0 0 0 13px;
   }
 }
 .LibraryLocationName {
   height: 36px;
   float: left;
   display: flex;
   justify-content: left;
   align-items: flex-start;
   flex-direction: row;
   flex-wrap: wrap;
 }

 .LibraryLocationName .el-input {
   height: 36px;
   width: 161px;

 }
 .StartTime {
   height: 36px;
   float: left;
   display: flex;
   justify-content: left;
   align-items: flex-start;
   flex-direction: row;
   flex-wrap: wrap;
 }
 .StartTime p {
   height: 36px;
   // width: 99px;
   padding-left: 12px;
 }
 .StartTime ::v-deep .el-input__inner {
   height: 36px;
   width: 248px;
 }
 .ArrivalTime {
   height: 36px;
   float: left;
   display: flex;
   justify-content: left;
   align-items: flex-start;
   flex-direction: row;
   flex-wrap: wrap;
 }
 .ArrivalTime p {
   height: 36px;
   //width: 99px;
   padding-left: 12px;
 }
 .ArrivalTime ::v-deep .el-input__inner {
   height: 36px;
   width: 248px;
 }
 .State {
   height: 36px;
   float: left;
   display: flex;
   justify-content: left;
   align-items: flex-start;
   flex-direction: row;
   flex-wrap: wrap;
 }
 .State p {
   height: 36px;
   //width: 70px;
   padding-left: 12px;
 }
 .State .el-select {
   width: 100px;
   //font-size: 16px;
 }
 .State .el-select ::v-deep .el-input .el-select__caret{
   height:35px;
   line-height: 35px;
   font-size:16px;
 }

 .TopRight {
   height: 36px;
   float: right;
 }
 .TopRight button {
   width: 62px;
   height: 35px;
   margin: 0 0 0 8px;
   font-size: 14px;
   padding: 0;
 }
 .TopRight_novw button{
   width: 62px;
   height: 35px;
   margin: 0 0 0 8px;
   font-size: 14px;
 }
 .TopRight_novw span{
   ::v-deep{
     .el-popover__reference-wrapper{
       .littesea{
         width: 20px;

       }
       .el-icon-search{

       }
     }
   }
 }
 .TopRight .el-button--success {
   color: #fff;
   background-color: #274df9;;
   border-color: #274df9;;
 }
 .TopRight .el-button--info {
   color: rgb(153, 153, 153);
   // background-color: #3e5762;
   // border-color: #3e5762;
 }
 //查询结束
 //任务列表开始
 .main {
   margin: 0 19px 0;
 }
 .main .el-table {
   // border: 1px solid #465663;
   font-size: 16px;
   color: #b2bcc9;
   // background-color: #04262b;
 }
 .main ::v-deep .el-table--enable-row-hover .el-table__body tr:hover > td {
   background-color: inherit;
 }

 .main ::v-deep .el-table thead {
   color: #bac1c3;
 }
 .oneClass {
   color: rgb(39, 77, 249);
 }
 .twoClass {
   color: rgb(153, 153, 153);
 }
 .threeClass {
   color: rgb(251, 52, 52);
 }
 .fourClass {
   color: #274df9;
 }
 .el-button {
   background: inherit;
   border: 0;
   // font-size: 16px;
   // color: #6fab42;
 }
 .el-table::before {
   height: 0;
 }

 .main_novw .el-table{
   height: 100%;
   ::v-deep{
     td,tr,th{
       height: 44px;
       font-size: 14px;
       padding: 0;
       color: rgb(6, 6, 6);
       font-weight: 400;
       .cell{
         padding:0 !important;
       }
     }
     tr:hover{
       background-color: #f3f5f8 !important;
       td{
         background-color: transparent !important;
       }
     }

     td:nth-child(3){
       white-space: pre-wrap;
       text-align: left;

       .cell{
         padding-left: 10px !important;
         display: -webkit-box;
         -webkit-box-orient: vertical;
         -webkit-line-clamp: 2; /* 表示最多显示两行文本 */
         overflow: hidden;
         text-overflow: ellipsis;
       }

     }
     .el-table__header-wrapper{
       .el-table__header{
         background-color: blue;
         th{
           padding: 4.098px 0;
           background: #eff3fb;
           font-size: 15px;
           color: #222222;
         }
       }
     }
     .el-table__body-wrapper{
       // height: calc(100% - 45px);
       // overflow-y: auto;
       .el-table__body{
         td{
           padding: 4.098px 0;
           height: 2.732px;
         }
       }
       .el-button{
         font-size: 14px;
         color: #274df9;;
         padding: 0;
       }
     }
   }
 }
 //任务列表结束
 //翻页开始

 .pagination-container ::v-deep .el-pagination {
   display: flex;
   flex-direction: row;
   justify-content: flex-end;
   // color: #b2bcc9;
 }

 .pagination-container
   ::v-deep
   .el-pagination.is-background
   .el-pager
   li:not(.disabled).active {
   // background-color: #83c346;
   // color: #cddebf;
   background-color: #274df9;;

 }

 ::v-deep  .el-pagination  .el-input__inner {
   // background: #04262b;
   // border: 1px solid #465663;
   // color: #b2bcc9;
   font-size: 13px;
 }
 //翻页结束
 </style>

