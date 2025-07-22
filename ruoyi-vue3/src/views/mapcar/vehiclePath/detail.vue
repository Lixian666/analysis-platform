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
    <div class="activelist">
      <p class="">路径信息</p>
      <el-table 
        :data="assignmentRecords" 
        style="width: 100%" 
        ref="eltableRef"
        @selection-change="handleSelectionChange"
      >

        <el-table-column type="selection" width="35" />
        <el-table-column align="center" label="名称">
          <template v-slot="scope">
            <span>路线{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="到达时间">
          <template v-slot="scope">
            <span v-if="scope.row.startTime">{{ scope.row.startTime.split(' ')[1] }}</span>
            <span v-else class="ml10">-</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="离开时间">
          <template v-slot="scope">
            <span v-if="scope.row.endTime">{{ scope.row.endTime.split(' ')[1]  }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column align="center" label="停留时间" width="120px">
          <template v-slot="scope">
            
            <span>{{ scope.row.duration }}</span>
          </template>
        </el-table-column>
        <el-table-column  align="center" label="颜色" width="50px">
          <template v-slot="scope">
            <div class="block" :style="'background-color:'+scope.row.color+';'"></div>
          </template>
        </el-table-column>
        <el-table-column align="center" label="类型" width="100px">
          <template v-slot="scope">
            <span v-text="getcartype(scope.row.type)">
            </span>
          </template>
        </el-table-column>
        <!-- <el-table-column align="center" label="操作" width="55">
          <template v-slot="scope">
            <el-button
              type="text"
              size="mini"
              @click="handleEdit(scope.row.cardId,scope.row.id)"
              >详情</el-button>
          </template>
        </el-table-column> -->
      </el-table>
      


      <!-- <el-table v-if="assignmentRecords.length>0" :data="assignmentRecords" border style="width: 100%">
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
            <span :style="getTimeColor(scope.row.startTime, scope.row.endTime)" v-text="stopTimeLength(scope.row.duration)" />
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
      </el-table>  -->
    </div>

    <div class="activeName"  >
      <div class="" style="height: 100%;">
        <carline :list="cargoline" ref="carlineRef"/>
      </div>
    </div>

    <!-- 修改滑块部分 -->
    <div class="time-axis-container" v-if="dwShow">
    


      <!-- 移动到这里的时间颗粒度控制按钮组 -->
      <div style="margin-left: 10px">
        <el-button-group>
          <el-button size="small" icon="ZoomIn" @click="zoomInTimeGranularity" title="放大时间颗粒度"></el-button>
          <el-button size="small" icon="ZoomOut" @click="zoomOutTimeGranularity" title="缩小时间颗粒度"></el-button>
          <el-button size="small" icon="Refresh" @click="resetTimeGranularity" title="还原"></el-button>
        </el-button-group>
      </div>

  

      <div class="time-axis-wrapper" ref="zWidth">
        <div class="time-axis" style="height: 100%;" :style="{ width: totalWidth + 'px' }" ref="timeAxis">

          <!-- <div class="pushpinno" style="position: absolute; border-bottom: 45px solid aquamarine;"
               :style="{width: '10px', height: '50px', left: `${position.xL}px`}" @mousedown="startDragL">
          </div> -->
          <div class="timeblock" >
            <div v-for="item in assignmentRecords" :class="item.boxshaw?'shaw':''" :style="'background-color:'+item.color+';width:'+getWidth(item)+';left:'+getLeft(item)+'px;'"></div>
          </div>
          <div class="timecard" width="100%">
            <div
              v-for="(timeBlock, index) in processedTimeList"
              :key="index"
              class="time-block"
              :class="{ 'has-activity': timeBlock.data > 0 }"
              :style="{
                  width: timeBlockWidth + 'px',
                  minWidth: timeBlockWidth + 'px'
                }"
              :data-time="timeBlock.value"
            >
              <div class="time-label" v-if="shouldShowLabel(index)">
                {{ timeBlock.showTime }}
              </div>
              <div class="time-marker"></div>
            </div>
          </div>

          <!-- <div class="pushpinno" style="position: absolute; border-bottom: 45px solid red;"
               :style="{width: '10px', height: '50px', left: `${position.xR}px`}"
               @mousedown="startDragR">
          </div> -->


        </div>
      </div>
    </div>
 
  </div>
  <div v-else>
    <div401 />
  </div>
 </template>
 
 <script setup>
  import div401 from '@/views/error/401.vue'
  import { onMounted, ref, nextTick, computed } from "vue"
  import carline from '@/components/mars3D/carline.vue'
  import { getlistByUserId } from '@/api/mapcar.js'
  import { get } from '@vueuse/core'
  const route = useRoute()
  const router = useRouter()
  //data return start
  const cargoline = ref(null)
  const listQuery = ref({
    page: 1,
    limit: 10
  })

  const starttime = ref( '00:00:00')
  const endtime = ref('00:00:00')

  const eltableRef = ref(null)
  const assignmentRecords = ref([])
  const TaskInformation = ref([])// 商品车
  const TrackData = ref([])
  const CarRestData1 = ref([])
  const dialogVisible = ref(false)
  const partitionIdName = ref('')
  const positionName = ref('')
  const carlineRef = ref(null)
  const dwShow = ref(true)
  const timeGranularity = ref(1800) // 最大0.5小时
  const minTimeGranularity = ref(30) // 最小30秒
  const maxTimeGranularity = ref(1800) // 最大0.5小时
  const dateTime = ref('')
  const dateTimeLIst = ref([])
  const isUserTriggered = ref(false)
  
  const timeGranularityLevels = ref( [1800, 900, 60]) // 1小时、30分钟、15分钟、1分钟
  const currentGranularityIndex = ref(0)
  const timeblock = ref('<div></div><div></div>')

  const timeBlockWidth = ref(120)
  const minBlockWidth = ref(30)
  const maxBlockWidth = ref(120)
  const timeS = ref(0)
  const sliderStep = ref(0.1) // 添加更小的步进值
  const totalSteps = ref(144) // 12小时 * 12个刻度(5分钟一个刻度)
  const changeDataTimer = ref(null)
  const position = ref({xL: 0, xR: 100}) // 初始位置
  const draggingL = ref(false) // 是否正在拖动
  const draggingR = ref(false) // 是否正在拖动
  const offset = ({xL: 0, xR: 100}) // 偏移量
  const queryParams = ref({
    dzwl: true,
  })
  //data return end
  
  //computed start
  const totalWidth = computed(() => {
    return processedTimeList.value.length * timeBlockWidth.value;
  })
  const processedTimeList = computed(() => {
   // if (!dateTimeLIst.value.length) return [];
    let time = getCurrentDate()
    dateTime.value = time
    const startTime = new Date(dateTime.value + 'T09:00:00');
    const endTime = new Date(dateTime.value + 'T21:00:00');
    const result = [];
    for (let time = startTime; time <= endTime; time = new Date(time.getTime() + timeGranularity.value * 1000)) {
      const timeString = time.toTimeString().substr(0, 8);
      result.push({
        value: timeString,
        showTime: formatTimeLabel(time),
        data: getActivityData(timeString)
      });
    }
    return result;
  })
  //computed end

  onMounted(()=>{
    init()
  })

  //methods start
  function getcartype(val){
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
    let vid ={
      cardId: route.query.vehicleThirdId
    } 
    let jsonvid = JSON.stringify(vid)
    let res = await getlistByUserId(jsonvid)
    if((res.code == '200' || res.code == 200) && res.rows){
      let list = JSON.stringify(res.rows) 
      let array = JSON.parse(list) 
      starttime.value = gettime(position.value.xL)
      endtime.value = gettime(position.value.xR)
      for (let index = 0; index < array.length; index++) {
        const element = array[index];
        element.color =  getRandomHexColor()
        element.boxshaw = true
      }
     assignmentRecords.value = array
      nextTick(()=>{
        assignmentRecords.value.forEach(row => {
          isUserTriggered.value = false; // 明确不是用户操作
          eltableRef.value.toggleRowSelection(row, true)
        })
        carlineRef.value.initMap(array)
        isUserTriggered.value = true; // 明确不是用户操作
      })
    }
    // let id = route.query.id
    // let resd = await getexperimentid(id)
    // if((resd.code == '200' || resd.code == 200) && resd.data){
    //   assignmentRecords.value.push(resd.data) 
    // }
  }

  
  function handleSelectionChange(selection){
    if(!isUserTriggered.value){return}
    nextTick(()=>{
      // console.log(selection)
      carlineRef.value.delmars()
      carlineRef.value.initdraw(selection)
      const exclude = [];
      selection.forEach((item)=>{
        exclude.push(item.id)
      })
      assignmentRecords.value.forEach(item => {
        item.boxshaw= exclude.includes(item.id) ? true : false
      });
      selection.sort((a, b) => {
        const timeA = new Date(a.createTime).getTime();
        const timeB = new Date(b.createTime).getTime();
        return timeA - timeB; // 降序排列
      });
      let leftto = 0
      if(selection.length>0){
        leftto = getLeft(selection[0]) 
      }
      document.getElementsByClassName('time-axis-wrapper')[0].scrollTo({
        top: 0,
        left: leftto,
        behavior: 'smooth'
      });
    })
  }


  function diagnosisRoles(text){
    return true
  }

  function getWidth(item){
    let start = item.startTime.split(' ')[1]
    let endrt = item.endTime.split(' ')[1]
    let leftpx1 = (getsecond(start)-getsecond('09:00:00'))/(getsecond('21:00:00')-getsecond('09:00:00'))*(  totalWidth.value - timeBlockWidth.value) 
    let leftpx2 = (getsecond(endrt)-getsecond('09:00:00'))/(getsecond('21:00:00')-getsecond('09:00:00'))*(  totalWidth.value - timeBlockWidth.value) 
    return leftpx2 - leftpx1 + 'px'
  }
  function getLeft(item){
    let start = item.startTime.split(' ')[1]
    let leftpx = (getsecond(start)-getsecond('09:00:00'))/(getsecond('21:00:00')-getsecond('09:00:00'))*(  totalWidth.value - timeBlockWidth.value) 
    return leftpx
  }

  function zoomInTimeGranularity() {
    if (starttime.value == '00:00:00' && endtime.value == '00:00:00') {
      return
    }
    if(queryParams.value.timeS === 2){
      return;
    }
    if (currentGranularityIndex.value < timeGranularityLevels.value.length - 1) {
      currentGranularityIndex.value++;
      timeGranularity.value = timeGranularityLevels.value[currentGranularityIndex.value];
      timeS.value = currentGranularityIndex.value;
      queryParams.value.timeS = currentGranularityIndex.value;
      // 根据时间粒度调整块宽度
      switch (timeGranularity.value) {
        // case 3600: // 1小时
        //   timeBlockWidth.value = 120;
        //   break;
        case 1800: // 30分钟
          timeBlockWidth.value = 90;
          break;
        case 900: // 15分钟
          timeBlockWidth.value = 60;
          break;
        case 60: // 1分钟
          timeBlockWidth.value = 60;
          break;
      }
      nextTick(() => {
        //getPosiTionList(queryParams.value.idCard, false)
        position.value.xL = getleft(getmatSeconds(starttime.value))
        position.value.xR = getleft(getmatSeconds(endtime.value))
      });
    }
  }
  function zoomOutTimeGranularity() {
    if (starttime.value == '00:00:00' && endtime.value == '00:00:00') {
      return
    }
    if (currentGranularityIndex.value > 0) {
      currentGranularityIndex.value--;
      timeGranularity.value = timeGranularityLevels.value[currentGranularityIndex.value];
      timeS.value = currentGranularityIndex.value;
      queryParams.value.timeS = currentGranularityIndex.value;
      // 根据时间粒度调整块宽度
      switch (timeGranularity.value) {
        // case 3600: // 1小时
        //   timeBlockWidth.value = 120;
        //   break;
        case 1800: // 30分钟
          timeBlockWidth.value = 90;
          break;
        case 900: // 15分钟
          timeBlockWidth.value = 60;
          break;
        case 60: // 1分钟
          timeBlockWidth.value = 30;
          break;
      }
      nextTick(() => {
        // scrollToCenterTime(centerTime);
        // changeData(value.value);
        position.value.xL = getleft(getmatSeconds(starttime.value))
        position.value.xR = getleft(getmatSeconds(endtime.value))
      });
    }
  }

  function resetTimeGranularity() {
    if (starttime.value == '00:00:00' && endtime.value == '00:00:00') {
      return
    }
    currentGranularityIndex.value = 0;
    timeGranularity.value = timeGranularityLevels.value[0]; // 重置为1小时
    timeBlockWidth.value = 90;
    timeS.value = 0;
    queryParams.value.timeS = 0;

    nextTick(() => {
      // getPosiTionList(queryParams.value.idCard, false)
      // scrollToCurrentTime();
      // changeData(this.value);
      position.value.xL = getleft(getmatSeconds(starttime.value))
      position.value.xR = getleft(getmatSeconds(endtime.value))
    });
  }

  function formatTimeLabel(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');

    // 始终返回完整时间格式
    return `${hours}:${minutes}`;
  }

  function gettime(time) {
    let leftpx = time - (- 10)
    let xx = 43200 * leftpx / (totalWidth.value -timeBlockWidth.value)
    return formatSeconds(xx)
  }

  function formatSeconds(seconds) {
    let hours = Math.floor(seconds / 3600) + 9; // 计算小时
    let minutes = Math.floor((seconds % 3600) / 60); // 计算分钟
    let secs = parseInt(seconds % 60); // 计算秒数
    // 补零操作，确保格式始终是两位数
    return [hours, minutes, secs].map(unit => String(unit).padStart(2, '0')).join(':');
  }

  function getActivityData(timeString) {
    //const timeData = dateTimeLIst.value.find(item => (getsecond(item.createTime.split(' ')[1])-getsecond(timeString)>0&&getsecond(item.createTime.split(' ')[1])-getsecond(timeString) < timeGranularity.value));
    const timeData = dateTimeLIst.value.find(item => (gethms(item.recordTime)-getsecond(timeString)>0&&gethms(item.recordTime)-getsecond(timeString) < timeGranularity.value));
    return timeData ? 1:0//timeData.data : 0;
  }
  function getCurrentDate() {
    const now = new Date();
    const year = now.getFullYear();
    let month = now.getMonth() + 1;
    month = month.toString().padStart(2, '0')
    const day = now.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function getRandomHexColor() {
    const color = '#' + Math.floor(Math.random() * 0xffffff).toString(16).padStart(6, '0');
    return color+'ff';
  }
  
  function shouldShowLabel(index) {
    // 修改为始终返回 true，显示所有时间刻度
    return true;
  }

  function gethms(time){
    let newdate = new Date(time)
    //return newdate.getHours()+':'+newdate.getMinutes()+':'+newdate.getSeconds()
    return Number(newdate.getSeconds()) + Number(newdate.getMinutes())*60 + Number(newdate.getHours())*60*60
  }

  function getsecond(val){
    let aaa = val.split(':')
    return Number(aaa[2]) + Number(aaa[1])*60 + Number(aaa[0])*60*60

  }
  function getmatSeconds(str) {
    const [hours, minutes, seconds] = str.split(":").map(Number);
    return (hours - 9) * 3600 + minutes * 60 + seconds;
  }
  
  function getleft(second) {
    let pers = second / 43200
    let leftpx = pers * (totalWidth.value - timeBlockWidth.value) + (- 10)
    return leftpx
  }
  //methods end
 </script>
 
 <style scoped lang="scss">
 #body-box {
  height:  calc(100vh - 84px);
  overflow: hidden;
  .activelist{
    width: 545px;
    height: calc(100% - 90px);
  //  background-color: #03b311;
    float: left;
    overflow: hidden;
    overflow-y: scroll;
  }

  .block{
    width: 30px;
    height: 20px;
  }


 .activeName{
    width: calc(100% - 550px);
    height: calc(100% - 90px);
   // background-color: red;
    float: right;
  }
  .time-axis-container{
    //margin-top: calc(250px);
    width: 100%;
    height: 80px;
   // margin-top: 20px;
    background: #f5f5f5;
    border-radius: 4px;
    position: absolute;
    bottom:0;
    padding: 0;
    overflow: visible; // 允许内容溢出显示

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

.time-axis-container {
    width: 100%;
    height: 80px;
    margin-top: 20px;
    background: #f5f5f5;
    border-radius: 4px;
    position: relative;
    padding: 0;
    overflow: visible; // 允许内容溢出显示
  }

  .time-axis-wrapper {
    width: 100%;
    height: 50px;
    overflow-x: auto;
    overflow-y: visible; // 允许垂直方向内容溢出
    padding: 0 30px ;

    position: absolute; // 添加定位上下文
    bottom: 0;
    z-index: 1; // 确保正确的层叠顺序
   
  }

  .time-axis {


    position: relative;

    // height: 40px;
    // transition: all 0.3s ease;
    // margin: 0 20px;
    //
    .timecard {
      position: absolute;
      align-items: center;
      display: flex;
      height: 100%;
    
    }
    .timeblock{
      position: absolute;
      width: 100%;
      height: 100%;
      .shaw{
        box-shadow: 0 0 5px 2px #636363;
      }
      div{
        position: absolute;
        width: 200px;
        height: 100%;
        //background-color: #03b31122;
      }
    }
  }

  .time-block {
    position: relative;
    flex-shrink: 0;
    height: 20px;
    border-left: 1px solid #ddd;
    transition: all 0.3s ease;
    // margin-top: 15px;

    &.has-activity {
      background-color: rgba(82, 196, 26, 0.2);
    }

    .time-label {
      position: absolute;
      top: 0px;
      // left: 50%;
      transform: translateX(-50%); // 添加45度旋转
      font-size: 12px;
      color: #666;
      white-space: nowrap;
      z-index: 1;
      transform-origin: center;
      margin-top: -5px; // 向上调整位置
    }

    .time-marker {
      position: absolute;
      bottom: -2px;
      left: 50%;
      transform: translateX(-50%);
      width: 1px;
      height: 20px;
      background-color: #999;
    }
  }
  .time-block:last-child{
    .time-marker{
      width: 0;
    }
  }

  // 移除渐变效果，因为现在所有内容都在灰条内
  .time-axis-wrapper::before,
  .time-axis-wrapper::after {
    display: none;
  }

  /* 优化滑块样式 */
  ::v-deep .el-slider {
    &__runway {
      height: 4px;
      margin: 16px 0;
    }

    &__bar {
      height: 4px;
      background-color: #409EFF;
    }

    &__button {
      width: 16px;
      height: 16px;
      border: 2px solid #409EFF;
      background-color: #fff;
      transition: transform 0.1s ease;

      &:hover, &.hover {
        transform: scale(1.2);
      }

      &:active, &.active {
        transform: scale(1.1);
      }
    }

    &__stop {
      width: 2px;
      height: 4px;
    }

    &__marks {
      font-size: 12px;
      color: #909399;
    }
  }

  /* 添加loading遮罩样式 */
  .loading-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.7);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }

  .pushpinno {
    z-index: 99999;
    width: 0;
    height: 0;
    border-left: 10px solid transparent;
    border-right: 10px solid transparent;
    border-bottom: 45px solid transparent; //#00bfff;
    transform: rotate(180deg);
  }
 </style>
 
 