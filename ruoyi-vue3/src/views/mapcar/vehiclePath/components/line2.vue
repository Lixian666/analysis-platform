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
    <div class="activelist" :class="{ 'collapsed': isListCollapsed }">
      <div class="title">
        <div class="title-left">
          <span class="ptxt">路径信息方案二</span>
          <span class="ptxt-info" v-show="!isListCollapsed">
            <span v-if="route.query.vehicleThirdId">卡号: {{ route.query.vehicleThirdId }}</span>
            <span v-if="route.query.startTime || route.query.endTime" class="date-info">
              {{ formatDateRange(route.query.startTime, route.query.endTime) }}
            </span>
          </span>
        </div>
        <div class="title-right">
          <span class="ptxt2 is-active">方案二</span>
          <span class="ptxt1" @click="goputdown(0)">方案一</span>
          <el-button size="small" type="primary" plain @click="showMarkerDialog = true" :icon="Location" title="添加临时标记点" class="marker-btn">
            标记
          </el-button>
          <span class="collapse-btn" @click="toggleListCollapse" :title="isListCollapsed ? '展开列表' : '收起列表'">
            <el-icon v-if="isListCollapsed"><DArrowRight /></el-icon>
            <el-icon v-else><DArrowLeft /></el-icon>
          </span>
        </div>
      </div>
      <el-table
        id="tablescroll"

        :data="assignmentRecords"
        style="width: 100%"
        ref="eltableRef"
        @selection-change="handleSelectionChange"
        @cell-click="handleCellClick"
        border
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="selection" :width="isListCollapsed ? 35 : 35" />
        <el-table-column align="center" label="名称" :width="isListCollapsed ? 60 : undefined">
          <template v-slot="scope">
            <span>路线{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isListCollapsed" align="center" label="开始时间" width="80">
          <template v-slot="scope">
            <span v-if="scope.row.startTime">{{ scope.row.startTime.split(' ')[1] }}</span>
            <span v-else class="ml10">-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isListCollapsed" align="center" label="结束时间" width="80">
          <template v-slot="scope">
            <span v-if="scope.row.endTime">{{ scope.row.endTime.split(' ')[1] }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isListCollapsed" align="center" label="停留时间" width="85">
          <template v-slot="scope">
            <span v-text="gettimetxt(scope.row.duration)"></span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isListCollapsed" align="center" label="点数量" width="65">
          <template v-slot="scope">
            <span v-text="getlistlength(scope.row.takBehaviorRecordDetailList)"></span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="颜色" :width="isListCollapsed ? 50 : 50">
          <template v-slot="scope">
            <div class="block" :style="'background-color:' + scope.row.color + ';'"></div>
          </template>
        </el-table-column>
        <el-table-column align="center" label="类型" :width="isListCollapsed ? 95 : 110">
          <template v-slot="scope">
            <span v-text="getcartype(scope.row.type)"></span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="后处理" :width="isListCollapsed ? 85 : 90">
          <template v-slot="scope">
            <span 
              class="match-status-badge"
              :class="getMatchStatusClass(scope.row.matchStatus)"
            >
              {{ getMatchStatusText(scope.row.matchStatus) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="activeName" :class="{ 'expanded': isListCollapsed }">
      <div class="" style="height: 100%;">
        <carline :list="cargoline" ref="carlineRef"/>
      </div>
    </div>
    <!-- 修改滑块部分 -->
    <div class="time-axis-container" v-if="dwShow">
      <!-- 移动到这里的时间颗粒度控制按钮组 -->
      <div class="top">
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
            <div v-for="item in assignmentRecords" :class="getclass1(item)" :style="'background-color:'+item.color+';width:'+getWidth(item)+';left:'+getLeft(item)+'px;'"></div>
          </div>

          <div class="timeblock index_z9" >
            <div v-for="item in traignmentRecords" :class="getclass2(item)" :style="'width:'+getWidth(item)+';left:'+getLeft(item)+'px;'" @click="linecheck(item)"></div>
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
    
    <!-- 添加临时标记点对话框 -->
    <el-dialog
      v-model="showMarkerDialog"
      title="添加临时标记点"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="markerForm" label-width="80px">
        <el-form-item label="经度">
          <el-input
            v-model="markerForm.longitude"
            placeholder="请输入经度（例如：116.397428）"
            clearable
            @keyup.enter="addMarkerToMap"
          >
            <template #append>°</template>
          </el-input>
          <div style="color: #999; font-size: 12px; margin-top: 5px;">
            范围：-180 ~ 180
          </div>
        </el-form-item>
        <el-form-item label="纬度">
          <el-input
            v-model="markerForm.latitude"
            placeholder="请输入纬度（例如：39.909179）"
            clearable
            @keyup.enter="addMarkerToMap"
          >
            <template #append>°</template>
          </el-input>
          <div style="color: #999; font-size: 12px; margin-top: 5px;">
            范围：-90 ~ 90
          </div>
        </el-form-item>
        <el-form-item>
          <div style="padding: 12px; background: #f0f9ff; border-left: 3px solid #409EFF; color: #666; font-size: 13px; line-height: 1.6;">
            <div style="margin-bottom: 5px;">💡 <strong>提示：</strong></div>
            <div>1. 请输入有效的WGS84坐标系经纬度</div>
            <div>2. 标记点将自动定位到地图上</div>
            <div>3. 刷新地图后标记点会自动清除</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="clearAllMarkers">清除所有标记</el-button>
          <el-button @click="showMarkerDialog = false">取消</el-button>
          <el-button type="primary" @click="addMarkerToMap">添加标记</el-button>
        </span>
      </template>
    </el-dialog>
 
  </div>
  <div v-else>
    <div401 />
  </div>
</template>
 
 <script setup>
  import div401 from '@/views/error/401.vue'
  import { onMounted, ref, nextTick, computed } from "vue"
  import { DArrowLeft, DArrowRight, Location } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import carline from '@/components/mars3D/carline.vue'
  import { getlistByUserId } from '@/api/mapcar.js'
  const emits = defineEmits(['rest']);
  const route = useRoute()
  //data return start
  const isListCollapsed = ref(false)
  const showMarkerDialog = ref(false)
  const markerForm = ref({
    longitude: '',
    latitude: ''
  })
  const cargoline = ref(null)
  const listQuery = ref({
    page: 1,
    limit: 10
  })
  const starttime = ref( '00:00:00')
  const endtime = ref('00:00:00')
  const eltableRef = ref(null)
  const assignmentRecords = ref([])
  const traignmentRecords = ref([])
  const carlineRef = ref(null)
  const dwShow = ref(true)
  const timeGranularity = ref(60) // 最大0.5小时
  const dateTime = ref('')
  const dateTimeLIst = ref([])
  const isUserTriggered = ref(false)
  const timeGranularityLevels = ref( [1800, 900, 60]) // 1小时、30分钟、15分钟、1分钟
  const currentGranularityIndex = ref(2)
  const timeBlockWidth = ref(90)
  const timeS = ref(0)
  const position = ref({xL: 0, xR: 100}) // 初始位置
  const oldlist = ref([])
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
  // 添加标记到地图
  function addMarkerToMap() {
    const lng = parseFloat(markerForm.value.longitude)
    const lat = parseFloat(markerForm.value.latitude)
    
    // 验证经纬度
    if (!markerForm.value.longitude || !markerForm.value.latitude) {
      ElMessage.warning('请输入经度和纬度')
      return
    }
    
    if (isNaN(lng) || isNaN(lat)) {
      ElMessage.error('请输入有效的数字')
      return
    }
    
    if (lng < -180 || lng > 180) {
      ElMessage.error('经度范围应在 -180 到 180 之间')
      return
    }
    
    if (lat < -90 || lat > 90) {
      ElMessage.error('纬度范围应在 -90 到 90 之间')
      return
    }
    
    // 调用地图组件的方法添加标记
    if (carlineRef.value && carlineRef.value.addTempMarker) {
      const success = carlineRef.value.addTempMarker(lng, lat)
      if (success) {
        ElMessage.success('标记点添加成功')
        showMarkerDialog.value = false
        // 清空表单
        markerForm.value.longitude = ''
        markerForm.value.latitude = ''
      } else {
        ElMessage.error('添加标记点失败，请检查地图是否已初始化')
      }
    } else {
      ElMessage.error('地图组件未准备好')
    }
  }
  
  // 清除所有临时标记
  function clearAllMarkers() {
    if (carlineRef.value && carlineRef.value.clearTempMarkers) {
      carlineRef.value.clearTempMarkers()
      ElMessage.success('已清除所有临时标记点')
      showMarkerDialog.value = false
    }
  }
  
  function formatDateRange(startTime, endTime) {
    if (!startTime && !endTime) return ''
    if (startTime && endTime) {
      // 只显示日期部分，去掉时间
      const start = startTime.split(' ')[0]
      const end = endTime.split(' ')[0]
      if (start === end) {
        return `日期: ${start}`
      }
      return `日期: ${start} ~ ${end}`
    }
    if (startTime) {
      return `日期: ${startTime.split(' ')[0]}`
    }
    if (endTime) {
      return `日期: ${endTime.split(' ')[0]}`
    }
    return ''
  }
  function toggleListCollapse() {
    isListCollapsed.value = !isListCollapsed.value
  }
  function goputdown(id){
    emits('rest',id);
  }
  function getlistlength(list){
    let num = 0
    if(list&&Array.isArray(list)&&list.length>0){
      return list.length
    }
    return num
  }
  function gettimetxt(val){
    return val.split('秒')[0] + '秒'
  }
  function getcartype(val){
    let data = val
    if (data === 0) {
      data = '到达卸车' // #FF0000	🔴 白字清晰
    } else if (data === 1 ){
      data = '发运装车'  // #0000FF	🔵 白字清晰
    } else if (data === 2) {
      data = '轿运车卸车' // #FFFF00	🟨 黑字更清晰
    } else if (data === 3) {
      data = '轿运车装车' // #00FF00	🟩 黑字更清晰
    } else if (data === 4) {
      data = '地跑入库' // #FF00FF	🌈 白字清晰
    } else if (data === 5) {
      data = '地跑出库' // #800080	💜 白字清晰
    } else {
      data = '无法识别' // #FFA500	🟧 白字清晰
    }
    return data
  }
  
  // 获取匹配状态文本
  function getMatchStatusText(matchStatus) {
    if (matchStatus === null || matchStatus === undefined) {
      return '未处理';
    }
    const statusMap = {
      0: '未处理',
      1: '匹配成功',
      2: '匹配失败'
    };
    return statusMap[matchStatus] || '未处理';
  }

  // 获取匹配状态样式类
  function getMatchStatusClass(matchStatus) {
    if (matchStatus === null || matchStatus === undefined) {
      return 'match-status-gray';
    }
    const classMap = {
      0: 'match-status-gray',
      1: 'match-status-green',
      2: 'match-status-red'
    };
    return classMap[matchStatus] || 'match-status-gray';
  }
  
  async function init(){
    let vid ={
      cardId: route.query.vehicleThirdId,
      startTime: route.query.startTime || '',
      endTime: route.query.endTime || ''
    } 
    let jsonvid = JSON.stringify(vid)
    let res = await getlistByUserId(jsonvid)


    let color = ['#FF0000','#0000FF','#FFFF00','#00FF00','#FF00FF','#800080','#FFA500']

    if((res.code == '200' || res.code == 200) && res.rows){
      let list = JSON.stringify(res.rows) 
      let array = JSON.parse(list) 
      starttime.value = gettime(position.value.xL)
      endtime.value = gettime(position.value.xR)
      for (let index = 0; index < array.length; index++) {
        const element = array[index];
        element.color =  color[element.type]// getRandomHexColor()
        element.boxshaw = 'shaw'
        element.topline = false
      }
     assignmentRecords.value = array
     traignmentRecords.value = JSONRET(array)
     oldlist.value = JSONRET(array) //取消深度拷贝
      nextTick(()=>{
        assignmentRecords.value.forEach(row => {
          isUserTriggered.value = false; // 明确不是用户操作
          eltableRef.value.toggleRowSelection(row, true)
        })
        carlineRef.value.initMap(array)
        isUserTriggered.value = true; // 明确不是用户操作
      })
    }
  }

  function getclass1(item){
    return item.boxshaw == 'shaw'?'shaw':''
  }

  function getclass2(item){
    let classtxt1 = item.boxshaw == 'shaw'?'shaw':''
    let classtxt2 = item.topline?'red':'' 


    return classtxt1 + ' ' + classtxt2
  }


  function linecheck(row){
    traignmentRecords.value.forEach(item => {
      item.topline = '';
    });

    if (selectedRow.value === assignmentRecords.value.find(item=> item.id === row.id)) {
      selectedRow.value = null
      nextTick(()=>{
        carlineRef.value.drawyellowsloadDel(row)
        let toplog = 0
        const wrap = eltableRef.value.$el.querySelector('.el-scrollbar__wrap')
        if (wrap) {
          wrap.scrollBy({ top: toplog, left: 0, behavior: 'smooth' })
        }
      })
      changebool(traignmentRecords.value,row.id,'topline',false)
    } else {
      selectedRow.value = assignmentRecords.value.find(item=> item.id === row.id)
      nextTick(()=>{
        carlineRef.value.drawyellowsload(row)
        let toplog = eltableRef.value.$el.querySelector('.highlight-row').offsetTop
        let hegihtlog = eltableRef.value.$el.querySelector('.highlight-row').offsetHeight
        const wrap = eltableRef.value.$el.querySelector('.el-scrollbar__wrap')

        // 总高度  
        let allheight = wrap.scrollHeight -  hegihtlog
        // console.log('总高度 ',allheight)

        //总滑动距离
        let allmil = wrap.scrollHeight - wrap.clientHeight
        // console.log('总滑动距离 ',allmil)


        let topel =  toplog / allheight * allmil
        // console.log('topel',topel)
      //toplog

     

//最大滚动距离 = scrollHeight - clientHeight

        // // console.log(toplog,hegihtlog,wrap.scrollTop,wrap.scrollHeight - wrap.clientHeight,wrap.scrollHeight,wrap.clientHeight)
        if (wrap) {
          wrap.scrollTo({ top: topel, left: 0, behavior: 'smooth' })
        }
      })
      changebool(traignmentRecords.value,row.id,'topline',true)
    }
  }
  function germoreitem(arr1, arr2){
    const diff = arr1.filter(a => !arr2.find(b => b.id === a.id));
    return diff
  }
  function changebool( list,id,key,bool ){
    const target = list.find(item => item.id === id);
    if (target) {
      target[key]= bool;
    }
  }
  function handleCellClick(row, column, cell, event) {
    traignmentRecords.value.forEach(item => {
      item.topline = '';
    });
    if (selectedRow.value === row) {
      selectedRow.value = null
      nextTick(()=>{
        carlineRef.value.drawyellowsloadDel(row)
      })
      changebool(traignmentRecords.value,row.id,'topline',false)
    } else {
      selectedRow.value = row
      nextTick(()=>{
        carlineRef.value.drawyellowsload(row)
      })
      changebool(traignmentRecords.value,row.id,'topline',true)
    }
    let leftto = 0
    leftto = getLeft(row) 
    document.getElementsByClassName('time-axis-wrapper')[0].scrollTo({
      top: 0,
      left: leftto,
      behavior: 'smooth'
    });
  }
  // 当前选中的行
  const selectedRow = ref(null)
  // 动态设置行样式类名
  const tableRowClassName = (row) => {
    if(!selectedRow.value) return
    return row.row == selectedRow.value ? 'highlight-row' : ''
  }
  function handleSelectionChange(selection){
    if(!isUserTriggered.value){return}
    nextTick(()=>{
      if(oldlist.value.length>selection.length){ //减少
        let arr1 = JSONRET(oldlist.value)
        let arr2 = JSONRET(selection)
        let list = germoreitem(arr1,arr2)
        list.forEach((item)=>{
          carlineRef.value.delmars(item)
          changebool(assignmentRecords.value,item.id,'boxshaw','')
          changebool(traignmentRecords.value,item.id,'boxshaw','')
        })
      }else{
        let arr1 = JSONRET(oldlist.value)
        let arr2 = JSONRET(selection)
        let list = germoreitem(arr2,arr1)
        list.forEach((item)=>{
          carlineRef.value.addmars(item)
          changebool(assignmentRecords.value,item.id,'boxshaw','shaw')
          changebool(traignmentRecords.value,item.id,'boxshaw','shaw')
        })
      }
      //定位
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
      oldlist.value = JSONRET(selection) //取消深度拷贝
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
  function JSONRET(val){
    let json = JSON.stringify(val)
    return JSON.parse(json)
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
    currentGranularityIndex.value = 2;
    timeGranularity.value = timeGranularityLevels.value[2]; // 重置为1小时
    timeBlockWidth.value = 90;
    timeS.value = 0;
    queryParams.value.timeS = 0;
    nextTick(() => {
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
 :deep(.highlight-row) {
    background-color: #f0f7ff !important; /* 高亮背景色 */
  }
  .block {
    width: 20px;
    height: 20px;
    margin: auto;
    border-radius: 3px;
  }
 #body-box {
  height:  calc(100vh - 84px);
  overflow: hidden;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  
  .activelist{
    width: fit-content;
    min-width: 280px;
    max-width: 50%;
    height: calc(100% - 90px);
    flex-shrink: 0;
    transition: width 0.3s ease;
    box-sizing: border-box;
    overflow: visible;
    
    &.collapsed {
      overflow: visible;
    }
    
    .el-table {
      width: auto;
      min-width: 100%;
    }
    
    &.collapsed {
      .el-table {
        min-width: auto;
      }
    }
    
    .title{
      display: flex;
      justify-content: space-between;
      align-items: center;
      height: 40px;
      padding: 0 10px;
      box-sizing: border-box;
      
      .title-left {
        display: flex;
        align-items: center;
        flex: 1;
        overflow: hidden;
        min-width: 0;
      }
      
      .title-right {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        gap: 5px;
        
        .marker-btn {
          margin: 0 2px;
        }
      }
      
      .ptxt{
        margin: 0 0 0 10px;
        padding: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        flex-shrink: 0;
        height: 40px;
        line-height: 40px;
        font-weight: 500;
      }
      
      .ptxt-info {
        display: flex;
        align-items: center;
        height: 40px;
        line-height: 40px;
        font-size: 12px;
        color: #666;
        margin-left: 10px;
        transition: opacity 0.3s ease;
        overflow: hidden;
        flex: 1;
        min-width: 0;
        
        span {
          margin-right: 10px;
          white-space: nowrap;
          flex-shrink: 0;
        }
        
        .date-info {
          color: #888;
        }
      }
      
      .ptxt1{
        height: 40px;
        line-height: 40px;
        margin-right: 5px;
        margin-left: 5px;
        cursor: pointer;
        white-space: nowrap;
        flex-shrink: 0;
        font-size: 13px;
      }
      
      .ptxt2{
        height: 40px;
        line-height: 40px;
        margin-right: 5px;
        cursor: pointer;
        white-space: nowrap;
        flex-shrink: 0;
        font-size: 13px;
      }
      
      .is-active{
        color: #409EFF;
        cursor: context-menu;
      }
      
      .collapse-btn {
        height: 40px;
        line-height: 40px;
        margin-right: 5px;
        margin-left: 5px;
        cursor: pointer;
        font-size: 16px;
        color: #409EFF;
        transition: transform 0.3s ease;
        display: flex;
        align-items: center;
        flex-shrink: 0;
        
        &:hover {
          transform: scale(1.15);
        }
      }
    }
    :deep(.el-table--border){
      position: relative;
      height: calc(100% - 40px);
      width: auto;
      overflow-x: auto;
    }
    
    :deep(.el-table__body-wrapper) {
      overflow-x: auto;
    }
    
    :deep(.el-table__header-wrapper) {
      overflow-x: auto;
    }
    
    // 收起状态下的表格样式优化
    &.collapsed {
      min-width: 280px;
      max-width: 400px;
      width: fit-content;
      overflow: visible;
      
      :deep(.el-table) {
        font-size: 13px;
        width: auto !important;
        min-width: auto !important;
        table-layout: auto !important;
        
        th {
          padding: 8px 6px;
          font-size: 13px;
          font-weight: 500;
          background-color: #f5f7fa;
        }
        
        td {
          padding: 8px 6px;
        }
        
        .el-checkbox {
          transform: scale(0.9);
        }
        
        .cell {
          padding: 0 6px;
          white-space: nowrap;
          text-align: center;
        }
        
        // 收起状态下允许表格根据内容自适应
        .el-table__body-wrapper,
        .el-table__header-wrapper {
          overflow-x: visible !important;
        }
        
        // 优化表格边框和间距
        .el-table__header th {
          border-bottom: 2px solid #ebeef5;
        }
        
        .el-table__body tr:hover {
          background-color: #f5f7fa;
        }
      }
      
      .match-status-badge {
        padding: 3px 8px;
        font-size: 11px;
        border-radius: 3px;
      }
      
      .block {
        width: 24px;
        height: 18px;
        border-radius: 2px;
      }
    }
    
    // 展开状态下的表格样式优化
    :deep(.el-table) {
      width: auto !important;
      table-layout: auto !important;
      
      .cell {
        white-space: nowrap;
        overflow: visible;
        padding: 0 5px;
        text-align: center;
      }
      
      th {
        padding: 8px 5px;
        text-align: center;
      }
      
      td {
        padding: 8px 5px;
        text-align: center;
      }
    }
  }

  .block{
    width: 30px;
    height: 20px;
  }


 .activeName{
    flex: 1;
    height: calc(100% - 90px);
    min-width: 0;
    transition: margin-left 0.3s ease;
    box-sizing: border-box;
    
    &.expanded {
      margin-left: 0;
    }
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
    background: #f5f5f5;
    border-radius: 4px;
    position: absolute;
    padding: 0;
    overflow: visible; // 允许内容溢出显示
    bottom:0;

  }
  .top{
    height: 24px;
  }

  .time-axis-wrapper {
    width: 100%;
    height: calc(100% - 24px) ;
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

      div{
        position: absolute;
        width: 200px;
        height: 20px;
        bottom: 0px;
        //background-color: #03b31122;
      }
      .red{
        // box-shadow: 0 0 5px 8px #ff7474;
        // height: 45px;
        &::before{
          content: '';
          width: 100%;
          display: block;
          height: 4px;
          background: black;
          position: absolute;
          top: -30px;

        }
        // border-top: 2px solid red;
      }
      .shaw{
        //box-shadow: 0 0 5px 8px #636363;
        height: 45px;
        &::before{
          top: -5px;
        }
      }

      
    }
    .index_z9{
      cursor: pointer;
      z-index: 99999999999;
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
  
  .match-status-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    color: #fff;
    white-space: nowrap;
    box-sizing: border-box;
  }

  .match-status-gray {
    background-color: #909399;
  }

  .match-status-green {
    background-color: #67c23a;
  }

  .match-status-red {
    background-color: #f56c6c;
  }
 </style>
 
 