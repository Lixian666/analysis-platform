<template>
  <div v-if="diagnosisRoles('vehicle:details')" id="body-box">
    <!-- <div class="title titlenovw">
      <div class="item" @click="goBack()">
        <div class="img">
          <img src="@/assets/login/jiantou3.png" alt="">
        </div>
        <div class="text">商品车详情</div>
      </div>
    </div> -->
    <!-- 商品车信息开始 -->
    <div class="TopTaskInformationBox TopTaskInformationBox_novw TaskInformation1">
      <p class="TaskInformation TaskInformation_novw">车辆信息</p>
      <el-table v-if="assignmentRecords.length>0" :data="assignmentRecords" border style="width: 100%">
        <el-table-column align="center" label="到达时间">
               <template v-slot="scope">
                 <span v-if="scope.row.startTime">{{ scope.row.startTime }}</span>
                 <span v-else class="ml10">-</span>
               </template>
             </el-table-column>
             <el-table-column align="center" label="离开时间">
               <template v-slot="scope">
                 <span v-if="scope.row.endTime">{{ scope.row.endTime  }}</span>
                 <span v-else>-</span>
               </template>
             </el-table-column>
             <el-table-column align="center" label="停留时间" width="120px">
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
      </el-table>
    </div>
    <!-- 商品车信息 -->
    <el-tabs v-model="activeName" class="el-tabs_novw" @tab-click="clicktab">

      <el-tab-pane label="轨迹回放" name="2" />

    </el-tabs>
    <!-- 行驶轨迹 -->
    <div class="activeName" v-if="activeName == 2" >
      <div class="TopTaskInformationBox_novw" style="height: 100%;">
        <carline :list="cargoline" ref="carlineRef"/>
      </div>
    </div>
    <!-- 行驶轨迹 -->
    <!-- 轨迹回放 -->
   
    <!-- 轨迹回放 -->
    <!-- 其他信息 -->

    <!-- 其他信息 -->
 
  </div>
  <div v-else>
    <div401 />
  </div>
 </template>
 
 <script setup>
  import div401 from '@/views/error/401.vue'
  import { onMounted, ref, nextTick } from "vue"
  import carline from '@/components/mars3D/carline.vue'
  import { getexperimentdetail, getexperimentid} from '@/api/mapcar.js'
  const route = useRoute()
  const router = useRouter()
  //data return start
  const cargoline = ref(null)
  const activeName = ref('2')
  const listQuery = ref({
    page: 1,
    limit: 10
  })
  const assignmentRecords = ref([])
  const TaskInformation = ref([])// 商品车
  const TrackData = ref([])
  const CarRestData1 = ref([])
  const dialogVisible = ref(false)
  const partitionIdName = ref('')
  const positionName = ref('')
  const carlineRef = ref(null)
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
    let vid = route.query.vehicleThirdId
    let res = await getexperimentdetail(vid)
    console.log((res.code == '200' || res.code == 200) , res.rows,'aaaaa',res)
    if((res.code == '200' || res.code == 200) && res.rows){

      let list = res.rows
      nextTick(()=>{
        console.log('carlineRef.value',carlineRef.value)
        carlineRef.value.initMap(list)
      })
    }
    let id = route.query.id
    let resd = await getexperimentid(id)
    if((resd.code == '200' || resd.code == 200) && resd.data){
      assignmentRecords.value.push(resd.data) 
      console.log(assignmentRecords.value)
    }


  }
  
  function diagnosisRoles(text){
    return true
  }
  function clicktab(){
    
  }
  
  
  //methods end
 </script>
 
 <style scoped lang="scss">
 #body-box {
  height:  calc(100vh - 84px);;
 .activeName{
  height: calc(100% - 250px);
 }
  // background: #04262b; //黑版
  // min-height: 976px;
  // position: relative;
  // padding: 0 0 20px; //黑版
  > .title {
    height: 50px;
    //margin-top: 10px;
    border-bottom: 1px solid rgb(220, 220, 220);
    // border-bottom: 1px solid #5d6777; //黑版
    // padding: 10px 20px 0; //黑版

    .item {
      width: 150px;
      overflow: hidden;
      // margin: 20px 0 0;
      padding: 0 0 0 20px;
      .img {
        width: 10px;
        height: 20px;
        float: left;
        margin-top: 4px;
        cursor: pointer;

        img {
          width: 100%;
          height: 100%;
        }
      }

      .text {
        margin-left: 10px;
        float: left;
        font-size: 24px;
        // color: #848c8e; //黑版
      }

      .txt {
        float: left;
        margin-top: 4px;
      }

      .input {
        float: right;
        height: 29px;

        .el-select {
          height: 100%;

          ::v-deep {
            .el-input {
              height: 100%;

              input {
                height: 100%;
              }
            }
          }
        }
      }
    }
  }
  >.titlenovw{
    height: 50px;
    margin-bottom: 0px;
    .item {
      width: 150px;
      height: 50px;

      //margin: 20px 0 0;
      padding: 0 0 0 20px;
      position: relative;
      .img {
        width: 8px;
        height: 15px;
        position: absolute;
        top: 0;
        bottom:0;
        margin: auto;
      }
      .text {
        line-height: 50px;
        margin-left: 20px;
        font-size: 16px;
      }
      .txt {
        margin-top: 4px;
      }
      .input {
        height: 29px;
        .el-select {
          height: 100%;
          ::v-deep {
            .el-input {
              height: 100%;
              input {
                height: 100%;
              }
            }
          }
        }
      }
    }
  }
}
// 商品车信息开始
.TopTaskInformationBox {
  padding: 10px 33px 0;
}
.TaskInformation {

  color: #333333;
  //color: #848c8e; //黑版

  padding: 0 0 0 10px;

  //border-left: 3px solid #8acc48; //黑版
}
.TaskInformation_novw{
  font-size: 17px;
  height: 18px;
  line-height: 18px;
  border-left: 4px solid #405dae;
}

// .TopTaskInformationBox ::v-deep .el-table {
//   background: #04262b; //黑版
// }
.TopTaskInformationBox ::v-deep .el-table th {
  background: #f8f9fb;
  //background: #04262b; //黑版
  font-size: 18px;
  color: #060606;
  //color: #848c8e; //黑版
  font-weight: 400;
  text-align: center;
}
.TopTaskInformationBox ::v-deep .el-table--group,
::v-deep .el-table--border {
  //border: 1px solid #465663; //黑版
  border-bottom: 0;
  border-right: 0;
}
// .TopTaskInformationBox ::v-deep .el-table::before,
// ::v-deep .el-table--group::after,
// ::v-deep .el-table--border::after {
//   background-color: #465663; //黑版
// }
// .TopTaskInformationBox ::v-deep .el-table th.is-leaf,
// ::v-deep .el-table td {
//   border-bottom: 1px solid #465663; //黑版
//   border-right: 1px solid #465663; //黑版
// }
.TopTaskInformationBox ::v-deep .el-table td {
  font-size: 16px;
  color: #333333;
  //color: #848c8e; //黑版
  text-align: center;
  // background: #04262b; //黑版
}
// .TopTaskInformationBox
//   ::v-deep
//   .el-table--enable-row-hover
//   .el-table__body
//   tr:hover
//   > td {
//   background-color: #04262b; //黑版
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(1) {
//   width: 196px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(2) {
//   width: 230px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(3) {
//   width: 230px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(4) {
//   width: 140px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(5) {
//   width: 140px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(6) {
//   width: 140px !important;
// }
// .TaskInformation1 ::v-deep colgroup col:nth-of-type(7) {
//   width: 140px !important;
// }

.TopTaskInformationBox{
  ::v-deep {
    .el-table {
      //border-top: 1px solid #dfe6ec;
      tr,th,td{
        height: 40px ;
        line-height: 40px ;
        padding: 0 ;
      }
      th{
        background: #eff3fb;
        color: #222222;
        font-weight: 400;
        text-align: center;
      }
    }
  }
}

.TopTaskInformationBox_novw{
  padding: 10px 33px 0;
  ::v-deep {
    .el-table {
      th,td,tr{
        font-size: 14px;
        height: 44px !important;
        line-height: unset !important;
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
      .el-table__header-wrapper{
        .el-table__header{
          th{
            padding: 4.098px 0;
            background: #eff3fb;
            font-size: 15px;
            color: #222222;
          }
        }
      }
      .el-table__body-wrapper{
        .el-table__body{
          td{
            padding: 4.098px 0;
            height: 2.732px;
          }
        }
      }
    }
  }
}

// 任务信息结束
//轨迹开始
.track {
  padding: 6.83px 13.66px 0;
}
.track ::v-deep .el-timeline-item__node {
  background: url("~@/assets/newimg/u3073.svg");
  background-size: 100%;
  width: 10.928px;
  height: 10.928px;
  left: 0px;
  top: 5.61966667px;
}
.track ::v-deep .el-timeline-item__tail {
  left: 5px;
  // border-left-color: #465663; //黑版
}
.el-timeline {
  padding: 0 0 0 109.28px;
}
.track ::v-deep .el-timeline-item__timestamp {
  position: absolute;
  top: 5.464px;
  left: -92.888px;
  font-size: 18px;
  color: #333333;
  font-weight: 700;
  margin: 0;
}

.tracknovw::v-deep .el-timeline-item__timestamp {
  font-size: 15px;
}

.track ::v-deep .el-timeline-item__content {
  padding-top: 3px;
  font-size: 18px;
  color: #333333;
  //color: #848c8e; //黑版
  font-weight: 700;
}
.tracknovw ::v-deep .el-timeline-item__content {
  font-size: 15px;
  .cetiso{
    height: 30px;

    .title{
      display: block;
      float: left;
      height: 30px;
      line-height: 30px;
    }
    .secoud{
      display: block;
      float: left;
      height: 30px;
      line-height: 30px;
      font-weight: 400;
      margin-left: 10px;
      position: relative;
      color: #107DFD;
      img{
        width: 20px;
        height: 20px;
        position: absolute;
        top: 0;
        bottom: 0;
        margin: auto;
      }
      span{
        margin-left: 25px;
      }
    }
    .strnum{
      height: 30px;
      display: block;
      line-height: 30px;
      font-weight: 400;
      margin-left: 10px;
      float: left;
    }
    .videoip{
      height: 30px;
      display: block;
      line-height: 30px;
      float: left;
      font-weight: 400;
      margin-left: 10px;
      position: relative;
      width: 117px;
      img{
        width: 24px;
        height: 24px;
        position: absolute;
        top: 0;
        bottom: 0;
        right: 0;
        margin: auto;
      }
      span{
        float: left;

      }
    }
  }
}

.tracknovw .el-timeline-item:first-child {
  ::v-deep .el-timeline-item__content {
    .cetiso{
      .title{
        color: #274DF9;
      }
    }
  }
}

.trackimg {
  margin: 6.83px 0 0;
  padding: 0;
  display: flex;
  justify-content: left;
  align-items: flex-start;
  flex-direction: row;
  flex-wrap: wrap;
}
.trackimg li {
  list-style: none;
  width: 240px;
  height: 135px;
  margin: 0 16px 0 0;
}
.trackli div {
  width: 100%;
  height: 100%;
}
.trackli ::v-deep .el-image__inner {
  width: 100%;
  object-fit: cover;
  cursor: pointer;
}
//轨迹结束
.oneClass {
  color: #274DF9;
}
.twoClass {
  color: #666666;
}
.threeClass {
  color: #FB3434;
}
.fourClass {
  color: #03b311;
}

.el-tabs_novw{
  margin: 0 33px 20px;
  ::v-deep {
    .el-tabs__header{
      margin: 0;
      .el-tabs__nav-wrap{
        .el-tabs__nav-scroll{
          .el-tabs__nav{
            .el-tabs__item{
              width:125px;
              height: 52px;
              line-height: 52px;
              text-align: center;
              padding: 0;
              font-size: 16px;
              font-weight: 400;
              background-color: transparent;
              color: #333333;
            }
            .el-tabs__active-bar{
              width:125px !important;
              background-color: rgb(39, 77, 249);
            }
            .is-active{
              color: rgb(39, 77, 249) !important;
            }
          }
        }
      }
      .el-tabs__nav-wrap::after{
        background-color: #dfe4ed;
        height: 2px;
      }
    }
  }
}
 </style>
 
 