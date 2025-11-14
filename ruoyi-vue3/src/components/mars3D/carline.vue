<template>
  <div id="marsbox" style="height: 100%;">
    <div id="mars3dContainer" style="height: 100%;"/>
  </div>
</template>

<style>
/* 禁用弹窗动画 */
.custom-popup-no-animation,
.custom-popup-no-animation * {
  transition: none !important;
  animation: none !important;
}

/* 弹窗样式优化 */
.mars3d-popup-content-wrapper {
  transition: none !important;
  cursor: move !important;  /* 添加拖拽光标提示 */
  user-select: none;  /* 禁止文本选择 */
}

.mars3d-popup-tip {
  transition: none !important;
}

/* 拖拽时的样式 */
.mars3d-popup-content-wrapper:active {
  cursor: grabbing !important;
}

/* 弹窗标题栏样式提示 */
.custom-popup-no-animation h4 {
  cursor: move;
  user-select: none;
}
</style>
<script setup>
  import { ref,onBeforeMount ,onMounted, onUnmounted, getCurrentInstance } from 'vue'
  import linepng from '@/assets/images/line-color-yellow.png'
  import startpng from '@/assets/images/route-start.png'
  import endpng from '@/assets/images/route-end.png'
  //data return start
  const form = ref({
    name: ''
  })
  const map = ref(null)
  const open = ref(false)
  const locationBar = ref(null)
  const cameraHistory = ref(null)
  const graphicLayer_map3D = ref(null)
  const graphicLayer_map2D = ref(null)
  const graphicLayer_map2Dtdt = ref(null)
  const graphicLayer_carlines = ref({}) 
  const fenceList = ref([])
  const initCount =ref(0)
  const flag = ref(false)
  const id = ref('id')
  const targetId = ref('targetId')
  const twoFlag = ref(false)
  const poData = ref(undefined)
  const zList = ref([])
  const { proxy } = getCurrentInstance()
  const initype = ref(false)
  const arrayList = ref([])
  const graphicYellow = ref(null)
  const tempMarkerLayer = ref(null)  // 临时标记点图层
  // 点位选择相关
  const selectedPointIndex = ref(-1)  // 当前选中的点位索引
  const currentTrackPoints = ref([])  // 当前选中轨迹的所有点位图形对象
  const currentTrackData = ref([])    // 当前轨迹的原始数据
  const allTrackPoints = ref({})      // 所有轨迹的点位数组，按轨迹ID存储
  const currentTrackId = ref(null)    // 当前选中的轨迹ID
  // 弹窗位置相关
  const popupFixedMode = ref(false)   // 弹窗是否为固定位置模式
  const savedPopupPosition = ref(null) // 保存的弹窗位置
  //data return end
  //生命周期start
  onBeforeMount(()=>{

  })
  onMounted(()=>{
    // 添加键盘事件监听
    window.addEventListener('keydown', handleKeyDown)
    
    //initMap()
    // this.initMap()
    //   if (this.list && this.list.length !== 0) {
    //     this.moveCarDirection(this.graphicLayer_carlines, this.$route.query, this.listsetmor(this.list))
    //   }

  })
  onUnmounted(()=>{
    // 移除键盘事件监听
    window.removeEventListener('keydown', handleKeyDown)
    
    if(cameraHistory.value){
        cameraHistory.value.remove()
        cameraHistory.value = null
      }
      if(graphicLayer_map3D.value){
        graphicLayer_map3D.value.remove()
        graphicLayer_map3D.value = null
      }
      if(graphicLayer_map2D.value){
        graphicLayer_map2D.value.remove()
        graphicLayer_map2D.value = null
      }
      if(graphicLayer_map2Dtdt.value){
        graphicLayer_map2Dtdt.value.remove()
        graphicLayer_map2Dtdt.value = null
      }
      for(let ione in graphicLayer_carlines.value){
        if(!graphicLayer_carlines.value[ione]) return
        graphicLayer_carlines.value[ione].remove()
        graphicLayer_carlines.value[ione] = null
      }
      if(locationBar.value){
        locationBar.value.remove()
        locationBar.value = null
      }
      map.value = null
  })
  function delmars(item){
    //if(initype.value){}
    if(graphicLayer_carlines.value['draw' + item.id]){
      graphicLayer_carlines.value['draw' + item.id].remove()
      graphicLayer_carlines.value['draw' + item.id] = null
    }
  }
  function drawyellowsload(item){


    if(graphicYellow.value){
      graphicYellow.value.remove()
      graphicYellow.value = null
    }


    let grap = new mars3d.layer.GraphicLayer()
    map.value.addLayer(grap)
    graphicYellow.value = grap
    

    // console.log(item)
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,1,true, item.takBehaviorRecordDetailList, 'yellow_' + item.id)
  }



  function drawyellowsloadDel(){
    if(graphicYellow.value){
      graphicYellow.value.remove()
      graphicYellow.value = null
    }
  }


  function addmars(item){
    let grap = new mars3d.layer.GraphicLayer()
    map.value.addLayer(grap)
    graphicLayer_carlines.value['draw' + item.id] = grap
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,arrayList.value.length,true, item.takBehaviorRecordDetailList, 'track_' + item.id)
  }
  //生命周期 end
  //methods start
  
  // 键盘事件处理
  function handleKeyDown(event) {
    // 只在有选中点位时响应
    if (selectedPointIndex.value === -1 || currentTrackPoints.value.length === 0) {
      console.log('键盘事件被忽略 - 没有选中的点位或点位数组为空')
      return
    }
    
    console.log('键盘按键:', event.key, '当前索引:', selectedPointIndex.value)
    
    // 左箭头 = 上一个点，右箭头 = 下一个点
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault()
      console.log('触发上一个点位')
      selectPreviousPoint()
    } else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault()
      console.log('触发下一个点位')
      selectNextPoint()
    }
  }
  
  // 选择上一个点
  function selectPreviousPoint() {
    console.log('selectPreviousPoint - 当前索引:', selectedPointIndex.value)
    if (selectedPointIndex.value > 0) {
      selectPointByIndex(selectedPointIndex.value - 1)
    } else {
      console.log('已经是第一个点位')
    }
  }
  
  // 选择下一个点
  function selectNextPoint() {
    console.log('selectNextPoint - 当前索引:', selectedPointIndex.value, '总数:', currentTrackPoints.value.length)
    if (selectedPointIndex.value < currentTrackPoints.value.length - 1) {
      selectPointByIndex(selectedPointIndex.value + 1)
    } else {
      console.log('已经是最后一个点位')
    }
  }
  
  // 切换弹窗固定模式
  function togglePopupFixedMode() {
    const wasFixedMode = popupFixedMode.value
    popupFixedMode.value = !popupFixedMode.value
    
    console.log('切换弹窗模式，从', wasFixedMode ? '固定' : '跟随', '到', popupFixedMode.value ? '固定' : '跟随')
    
    // 如果切换回跟随模式，恢复弹窗的默认定位
    if (!popupFixedMode.value && selectedPointIndex.value >= 0) {
      const currentPoint = currentTrackPoints.value[selectedPointIndex.value]
      if (currentPoint && currentPoint._popup && currentPoint._popup._container) {
        const popupContainer = currentPoint._popup._container
        // 移除固定定位样式，让弹窗恢复跟随点位
        popupContainer.style.position = ''
        popupContainer.style.transform = ''
        console.log('已恢复弹窗跟随模式')
      }
    }
  }
  
  // 根据索引选择点位
  function selectPointByIndex(index) {
    console.log('selectPointByIndex 被调用，索引:', index, '当前选中:', selectedPointIndex.value, '总点位:', currentTrackPoints.value.length)
    
    if (index < 0 || index >= currentTrackPoints.value.length) {
      console.warn('索引超出范围:', index)
      return
    }
    
    // 取消之前的高亮
    if (selectedPointIndex.value >= 0 && selectedPointIndex.value < currentTrackPoints.value.length) {
      const prevPoint = currentTrackPoints.value[selectedPointIndex.value]
      if (prevPoint && prevPoint.attr) {
        const normalCanvas = prevPoint.attr.normalCanvas
        if (normalCanvas) {
          console.log('恢复上一个点位的普通状态，索引:', selectedPointIndex.value)
          prevPoint.setStyle({ image: normalCanvas })  // 恢复普通状态
        }
        // 关闭之前的弹窗
        if (prevPoint.closePopup) {
          prevPoint.closePopup()
        }
      }
    }
    
    // 设置新的选中点
    selectedPointIndex.value = index
    const currentPoint = currentTrackPoints.value[index]
    
    if (!currentPoint || !currentPoint.attr) {
      console.error('找不到点位对象，索引:', index)
      return
    }
    
    // 高亮当前点（替换为带黄色外圈的图标）
    const selectedCanvas = currentPoint.attr.selectedCanvas
    if (selectedCanvas) {
      console.log('设置选中状态（黄色外圈），索引:', index)
      currentPoint.setStyle({ image: selectedCanvas })
    } else {
      console.warn('未找到 selectedCanvas，索引:', index)
    }
    
    // 显示弹窗
    if (currentPoint.openPopup) {
      currentPoint.openPopup()
    }
    
    // 如果是固定位置模式，应用保存的位置
    if (popupFixedMode.value && savedPopupPosition.value) {
      console.log('固定位置模式，应用保存的位置:', savedPopupPosition.value)
      setTimeout(() => {
        if (currentPoint._popup && currentPoint._popup._container) {
          const popupContainer = currentPoint._popup._container
          popupContainer.style.position = 'fixed'  // 使用固定定位
          popupContainer.style.left = savedPopupPosition.value.left
          popupContainer.style.top = savedPopupPosition.value.top
          popupContainer.style.transform = 'none'  // 移除变换
          console.log('已应用固定位置')
        } else {
          console.warn('未找到弹窗容器')
        }
      }, 100)  // 增加延迟，确保弹窗已渲染
    } else {
      console.log('跟随移动模式或未保存位置')
    }
    
    // 相机飞到该点位（可选）
    // map.value.flyToGraphic(currentPoint, { duration: 0.5 })
  }
  
  // 格式化日期时间
  function formatDateTime(dateTimeStr) {
    if (!dateTimeStr || dateTimeStr === '暂无数据') {
      return { date: '暂无数据', time: '暂无数据' }
    }
    
    try {
      // 假设格式为 "yyyy-MM-dd HH:mm:ss"
      const parts = dateTimeStr.split(' ')
      if (parts.length === 2) {
        return {
          date: parts[0],  // yyyy-MM-dd
          time: parts[1]   // HH:mm:ss
        }
      }
    } catch (e) {
      console.error('日期格式化错误:', e)
    }
    
    return { date: dateTimeStr, time: '' }
  }
  
  function initMap(newlist) {
    arrayList.value = newlist
    // mars3d初始化
    // 第一步：先创建 Map 实例（不立即设置 msaaSamples）
// 第一步：创建地图实例
map.value = new mars3d.Map('mars3dContainer', {
  center: proxy.$center,

  scene: {
    showSun: true,
    showMoon: true,
    showSkyBox: true,
    showSkyAtmosphere: false,
    fog: true,
    fxaa: true, // 开启 FXAA 抗锯齿
    globe: {
      showGroundAtmosphere: false,
      depthTestAgainstTerrain: false,
      baseColor: '#546a53'
    },
    mapProjection: mars3d.CRS.EPSG3857,
    mapMode2D: Cesium.MapMode2D.INFINITE_SCROLL
  },

  contextOptions: {
    webgl: {
      antialias: false // 由我们控制抗锯齿
    }
  },

  resolutionScale: 0.8
  // msaaSamples 先不设置，动态检测后再赋值
});

// 第二步：获取 viewer 实例（map.value 就是 viewer）
const viewer = map.value;
map.value.setCameraView(proxy.$center, { duration: 0.1 })
// 使用 viewer.scene 的 postRender 事件（确保 scene 已初始化）
// 使用 once = true，只执行一次
const removeListener = viewer.scene.postRender.addEventListener(() => {
  try {
    const context = viewer.scene.context;
    const gl = context?._gl;

    if (!gl) {
      console.warn('WebGL context 未就绪');
      setMsaaSamples(1);
      removeListener(); // 移除监听
      return;
    }

    // 检查是否支持多采样渲染缓冲
    const ext = gl.getExtension('WEBGL_multisampled_renderbuffer');
    if (!ext) {
      console.warn('当前环境不支持 WEBGL_multisampled_renderbuffer');
      setMsaaSamples(1);
      removeListener();
      return;
    }

    // 获取最大支持的采样数
    const maxSamples = gl.getParameter(ext.MAX_SAMPLES_WEBGL);
    console.log('设备最大支持的 MSAA 采样数:', maxSamples);

    // 安全设置：取 min(4, maxSamples)
    const safeSamples = Math.min(4, maxSamples > 0 ? maxSamples : 1);

    // 设置 MSAA 采样数
    setMsaaSamples(safeSamples);

  } catch (error) {
    console.error('检测 MSAA 支持失败:', error);
    setMsaaSamples(1);
  }

  // 执行完成后移除监听
  removeListener();
});

// 封装设置 msaaSamples 的函数，避免重复代码
function setMsaaSamples(samples) {
  try {
    viewer.scene.msaaSamples = samples;
    console.log(`✅ 已设置 MSAA 采样数: ${samples}`);
  } catch (e) {
    console.warn('设置 msaaSamples 失败:', e);
  }
}
    // 矢量地图倾斜摄影加载
    addTileLayer()
    camerahistory()
    initdraw(newlist)
    
    // 将视角移动到董家镇TIFDOM中心区域，正视角度
    // setTimeout(() => {
    //   map.value.setCameraView({
    //     lat: 36.760777,  // 董家镇中心纬度
    //     lng: 117.268876,  // 董家镇中心经度
    //     alt: 800,  // 高度800米，适合查看整个区域
    //     heading: 0,  // 正北方向
    //     pitch: -90,  // 正视（俯视）角度
    //     roll: 0
    //   }, { duration: 2 })  // 2秒过渡动画
    // }, 500)  // 等待图层加载完成
  }
  
  function initdraw(newlist){
    graphicLayer_carlines.value = {} //new mars3d.layer.GraphicLayer()
    for (let index = 0; index < newlist.length; index++) {
      let grap = new mars3d.layer.GraphicLayer()
      graphicLayer_carlines.value['draw' + newlist[index].id] = grap
      map.value.addLayer(grap)
      const element = newlist[index];
      let end = false
      if(index == newlist.length-1){
        end = true
      }
      moveCarDirection(grap, listsetmor(element.takBehaviorRecordDetailList),end,element.color,newlist.length,true, element.takBehaviorRecordDetailList, 'track_' + element.id)
    }
  }
  function addTileLayer() {
    add2DTileLayer()
  }

  function add3DTileLayer(){
    graphicLayer_map2Dtdt.value = new mars3d.layer.WmtsLayer({
      url: proxy.$tdt_img,
      layer: "img",
      style: "default",
      tileMatrixSetID: "w",
      format: "tiles",
      maximumLevel: 18
    })
    map.value.addLayer(graphicLayer_map2Dtdt.value)

    // 加载本地 3D Tiles 模型
    graphicLayer_map3D.value = new mars3d.layer.TilesetLayer({
      name: "董家镇货场",
      url: proxy.$modelMapDongJiaZhen,
      position: { alt: 36.064268 },
      maximumScreenSpaceError: 1,
      flyTo: true
    })
    map.value.addLayer(graphicLayer_map3D.value)
  }

function add2DTileLayer(){
  // graphicLayer_map2Dtdt.value = new mars3d.layer.XyzLayer({
  //   url: proxy.$tdt,
  //   opacity: 1
  // })
  // map.value.addLayer(graphicLayer_map2Dtdt.value)
  // 使用天地图影像服务作为底图
  // graphicLayer_map2Dtdt.value = new mars3d.layer.WmtsLayer({
  //   url: proxy.$tdt_img,
  //   layer: "img",
  //   style: "default",
  //   tileMatrixSetID: "w",
  //   format: "tiles",
  //   maximumLevel: 18,
  //   show: true,
  //   zIndex: 1  // 底图层级
  // })
  // map.value.addLayer(graphicLayer_map2Dtdt.value)

  // 叠加董家镇本地TIF切片图层（TMS格式）
  graphicLayer_map2D.value = new mars3d.layer.XyzLayer({
    name: "底图影像",
    url: proxy.$dataTiles,
    tms: true, // 使用TMS坐标系（Y轴从下往上）
    minimumLevel: 10,
    maximumLevel: 18,
    opacity: 1,  // 完全不透明
    show: true,
    zIndex: 10,  // 更高的层级，显示在天地图之上
    // 使用tilemapresource.xml中的精确边界（EPSG:4326）
    rectangle: proxy.$rectangle ? Cesium.Rectangle.fromDegrees(
        proxy.$rectangle.xmin,  // west
        proxy.$rectangle.ymin,  // south
        proxy.$rectangle.xmax,  // east
        proxy.$rectangle.ymax   // north
    ) : undefined,
    // 仅在覆盖范围内加载切片
    enablePickFeatures: false
  })
  map.value.addLayer(graphicLayer_map2D.value)
}

  function camerahistory (){
    // cameraHistory.value = new mars3d.thing.CameraHistory({
    //   limit: {
    //     // 限定视角范围
    //     position: Cesium.Cartesian3.fromDegrees(106.733082, 29.620789, 34.85),
    //     radius: 5000.0,
    //     debugExtent: false
    //   },
    //   maxCacheCount: 999
    // })
    // map.value.addThing(cameraHistory.value)
    // const eventTarget = new mars3d.BaseClass()
    // cameraHistory.value.on(mars3d.EventType.change, function (event) {
    //   // 触发自定义事件
    //   const count = event.count
    //   eventTarget.fire("changeCamera", { count })
    // })
  }
  function moveCarDirection(graphicLayer, pos, bool, color, num, showbool, originalData, trackId) {
    if (!pos || pos.length === 0) {
      return
    }
    let cargo = pos
    let linecolor = color
    // 保存原始数据，用于显示详细信息（recordTime, speed等）
    const dataPoints = originalData || []
    // 生成轨迹ID（如果未提供）
    const currentTrackIdentifier = trackId || 'track_' + Date.now()
    
    // === 性能优化：使用固定高度2米，不使用 clampToGround ===
    const fixedHeight = 0  // 固定高度0米
    const positions = cargo.map(([lon, lat, height]) => [lon, lat, fixedHeight])
    
    if (num <= 1) {
      // === 主线条（性能优化：固定高度）===
      const graphicq = new mars3d.graphic.PolylinePrimitive({
        positions: positions,
        show: showbool,
        style: {
          color: "#f50620",
          width: 5
          // 移除 clampToGround 以提升性能
        }
      })
      graphicLayer.addGraphic(graphicq)
    } else {
      // === 线条（性能优化：固定高度）===
      const graphicq = new mars3d.graphic.PolylinePrimitive({
        positions: positions,
        show: showbool,
        style: {
          color: linecolor,
          width: 3
          // 移除 clampToGround 以提升性能
        }
      })
      graphicLayer.addGraphic(graphicq)
    }

    // === 起点（性能优化：固定高度）===
    const graphics = new mars3d.graphic.BillboardEntity({
      position: [cargo[0][0], cargo[0][1], fixedHeight],
      show: showbool,
      style: {
        image: startpng,
        scale: 1,
        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        // 移除 heightReference 以提升性能
        disableDepthTestDistance: Number.POSITIVE_INFINITY
      },
      attr: { remark: "" }
    })
    graphicLayer.addGraphic(graphics)

    // === 终点（性能优化：固定高度）===
    const graphice = new mars3d.graphic.BillboardEntity({
      position: [cargo[cargo.length - 1][0], cargo[cargo.length - 1][1], fixedHeight],
      show: showbool,
      style: {
        image: endpng,
        scale: 1,
        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        // 移除 heightReference 以提升性能
        disableDepthTestDistance: Number.POSITIVE_INFINITY
      },
      attr: { remark: "" }
    })
    graphicLayer.addGraphic(graphice)

    // === 所有轨迹点（性能优化版 + 键盘选择）===
    const showTrackPoints = true  // 是否显示轨迹点
    
    if (showTrackPoints) {
      // 为当前轨迹初始化点位数组
      const trackPoints = []
      allTrackPoints.value[currentTrackIdentifier] = trackPoints
      
      // 性能优化：复用 canvas，只创建一次
      // 普通状态的点（缩小尺寸，半径从6px改为4px）
      const normalCanvas = document.createElement('canvas')
      normalCanvas.width = 14
      normalCanvas.height = 14
      const normalCtx = normalCanvas.getContext('2d')
      normalCtx.beginPath()
      normalCtx.arc(7, 7, 4, 0, 2 * Math.PI)  // 半径4px
      normalCtx.fillStyle = num <= 1 ? '#f50620' : linecolor
      normalCtx.fill()
      normalCtx.strokeStyle = '#ffffff'
      normalCtx.lineWidth = 1.5
      normalCtx.stroke()
      
      // 选中状态的点（外圈黄色，更大）
      const selectedCanvas = document.createElement('canvas')
      selectedCanvas.width = 22
      selectedCanvas.height = 22
      const selectedCtx = selectedCanvas.getContext('2d')
      // 绘制黄色外圈
      selectedCtx.beginPath()
      selectedCtx.arc(11, 11, 8, 0, 2 * Math.PI)
      selectedCtx.strokeStyle = '#FFD700'  // 金黄色
      selectedCtx.lineWidth = 3
      selectedCtx.stroke()
      // 绘制内部圆点
      selectedCtx.beginPath()
      selectedCtx.arc(11, 11, 5, 0, 2 * Math.PI)  // 半径5px（比普通状态稍大）
      selectedCtx.fillStyle = num <= 1 ? '#f50620' : linecolor
      selectedCtx.fill()
      selectedCtx.strokeStyle = '#ffffff'
      selectedCtx.lineWidth = 1.5
      selectedCtx.stroke()
      
      let trackPointIndex = 0  // currentTrackPoints 数组的实际索引
      cargo.forEach((point, index) => {
        // 跳过起点和终点（已经有独立图标）
        if (index === 0 || index === cargo.length - 1) return
        
        // 获取对应的原始数据
        const originalPoint = dataPoints[index] || {}
        const recordTimeStr = originalPoint.recordTime || '暂无数据'
        const { date, time } = formatDateTime(recordTimeStr)
        const speed = originalPoint.speed !== undefined && originalPoint.speed !== null 
          ? originalPoint.speed.toFixed(2) + ' m/s' 
          : '暂无数据'
        
        // 保存当前点在 currentTrackPoints 中的索引
        const currentPointIndex = trackPointIndex
        trackPointIndex++
        
        // 显示所有中间点 - 使用固定高度，复用 canvas
        const pointGraphic = new mars3d.graphic.BillboardEntity({
          position: [point[0], point[1], fixedHeight],  // 固定高度0米
          show: showbool,
          style: {
            image: normalCanvas,  // 默认使用普通状态的canvas
            scale: 1,
            horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
            verticalOrigin: Cesium.VerticalOrigin.CENTER,
            // 移除 heightReference 以提升性能
            disableDepthTestDistance: Number.POSITIVE_INFINITY
          },
          attr: {
            index: index,
            trackPointIndex: currentPointIndex,  // 在当前轨迹点位数组中的索引
            trackId: currentTrackIdentifier,     // 轨迹ID
            longitude: point[0],
            latitude: point[1],
            totalPoints: cargo.length,
            recordTime: recordTimeStr,
            date: date,
            time: time,
            speed: speed,
            normalCanvas: normalCanvas,      // 保存普通状态canvas
            selectedCanvas: selectedCanvas   // 保存选中状态canvas
          },
          // 添加点击弹窗
          popup: `
            <div class="custom-popup-no-animation" style="padding: 12px; min-width: 260px; background: #fff;">
              <h4 style="margin: 0 0 12px 0; color: #333; font-size: 16px; border-bottom: 2px solid #409EFF; padding-bottom: 8px;">
                📍 轨迹点信息
              </h4>
              <div style="line-height: 2; font-size: 14px;">
                <div style="display: flex; justify-content: space-between; padding: 4px 0;">
                  <span style="color: #666;">点位序号：</span>
                  <span style="color: #333; font-weight: 500;">${index + 1} / ${cargo.length}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding: 4px 0;">
                  <span style="color: #666;">经度：</span>
                  <span style="color: #333; font-weight: 500;">${point[0].toFixed(6)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding: 4px 0;">
                  <span style="color: #666;">纬度：</span>
                  <span style="color: #333; font-weight: 500;">${point[1].toFixed(6)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding: 4px 0; border-top: 1px dashed #eee; margin-top: 4px; padding-top: 8px;">
                  <span style="color: #666;">日期：</span>
                  <span style="color: #333; font-weight: 500;">${date}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding: 4px 0;">
                  <span style="color: #666;">时间：</span>
                  <span style="color: #333; font-weight: 500;">${time}</span>
                </div>
                <div style="display: flex; justify-content: space-between; padding: 4px 0;">
                  <span style="color: #666;">速度：</span>
                  <span style="color: #409EFF; font-weight: 600;">${speed}</span>
                </div>
                <div style="margin-top: 12px; padding-top: 8px; border-top: 1px solid #eee;">
                  <button id="togglePositionBtn_${index}" style="width: 100%; padding: 8px; background: #409EFF; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; transition: background 0.3s;">
                    <span id="btnText_${index}">📌 切换为固定位置</span>
                  </button>
                </div>
                <div style="margin-top: 8px; color: #999; font-size: 12px; text-align: center;">
                  💡 使用 ← → 方向键切换点位
                </div>
              </div>
            </div>
          `,
          popupOptions: {
            anchor: [0, -10],
            closeButton: true,
            className: 'custom-popup-no-animation'
          }
        })
        
        // 添加点击事件监听
        pointGraphic.on('click', function(event) {
          // 获取点击点位的轨迹ID和索引
          const clickedTrackId = pointGraphic.attr.trackId
          const clickedIndex = pointGraphic.attr.trackPointIndex
          
          console.log('点击了点位，轨迹ID:', clickedTrackId, '索引:', clickedIndex)
          
          // 切换到该轨迹
          if (allTrackPoints.value[clickedTrackId]) {
            currentTrackId.value = clickedTrackId
            currentTrackPoints.value = allTrackPoints.value[clickedTrackId]
            console.log('切换到轨迹:', clickedTrackId, '点位数:', currentTrackPoints.value.length)
            
            // 选择该点位
            if (clickedIndex >= 0 && clickedIndex < currentTrackPoints.value.length) {
              selectPointByIndex(clickedIndex)
            }
          } else {
            console.error('未找到轨迹:', clickedTrackId)
          }
        })
        
        // 添加弹窗打开事件监听，用于绑定按钮事件和拖拽监听
        pointGraphic.on('popupOpen', function(event) {
          setTimeout(() => {
            const btn = document.getElementById(`togglePositionBtn_${index}`)
            const btnText = document.getElementById(`btnText_${index}`)
            
            console.log('弹窗打开，查找按钮 ID:', `togglePositionBtn_${index}`, '找到按钮:', !!btn)
            
            if (btn && btnText) {
              // 更新按钮文本显示当前状态
              btnText.textContent = popupFixedMode.value ? '🔓 切换为跟随移动' : '📌 切换为固定位置'
              
              // 移除之前的事件监听（避免重复绑定）
              btn.onclick = null
              
              // 绑定按钮点击事件
              btn.addEventListener('click', function(e) {
                e.preventDefault()
                e.stopPropagation()
                
                console.log('按钮被点击！当前模式:', popupFixedMode.value ? '固定位置' : '跟随移动')
                
                // 如果当前不是固定模式，切换前先保存位置
                if (!popupFixedMode.value) {
                  // 保存当前弹窗位置
                  const popup = pointGraphic._popup
                  if (popup && popup._container) {
                    const rect = popup._container.getBoundingClientRect()
                    savedPopupPosition.value = {
                      left: rect.left + 'px',
                      top: rect.top + 'px'
                    }
                    console.log('保存弹窗位置:', savedPopupPosition.value)
                  }
                }
                
                // 切换模式
                togglePopupFixedMode()
                
                // 更新按钮文本和颜色
                btnText.textContent = popupFixedMode.value ? '🔓 切换为跟随移动' : '📌 切换为固定位置'
                btn.style.background = popupFixedMode.value ? '#67C23A' : '#409EFF'
                
                console.log('切换后模式:', popupFixedMode.value ? '固定位置' : '跟随移动')
              })
              
              // 设置按钮颜色反映当前状态
              btn.style.background = popupFixedMode.value ? '#67C23A' : '#409EFF'
            } else {
              console.warn('未找到按钮元素:', `togglePositionBtn_${index}`)
            }
            
            // 手动实现弹窗拖拽功能
            const popup = pointGraphic._popup
            if (popup && popup._container) {
              const popupElement = popup._container
              
              // 查找弹窗的标题栏（用于拖拽）
              const popupTip = popupElement.querySelector('.mars3d-popup-tip')
              const popupContent = popupElement.querySelector('.mars3d-popup-content-wrapper')
              const dragHandle = popupContent || popupElement  // 使用内容区域作为拖拽手柄
              
              let isDragging = false
              let currentX = 0
              let currentY = 0
              let initialX = 0
              let initialY = 0
              
              // 鼠标按下开始拖拽
              const handleMouseDown = function(e) {
                // 不要阻止按钮、链接、输入框等交互元素的点击
                const tagName = e.target.tagName.toLowerCase()
                const isInteractive = ['button', 'a', 'input', 'select', 'textarea'].includes(tagName)
                const hasButton = e.target.closest('button')
                
                if (isInteractive || hasButton) {
                  console.log('点击了交互元素，跳过拖拽')
                  return
                }
                
                // 允许在整个弹窗区域拖拽（除了按钮）
                isDragging = true
                const rect = popupElement.getBoundingClientRect()
                initialX = e.clientX - rect.left
                initialY = e.clientY - rect.top
                
                popupElement.style.cursor = 'grabbing'
                console.log('开始拖拽弹窗，初始位置:', { x: e.clientX, y: e.clientY })
              }
              
              // 鼠标移动
              const handleMouseMove = function(e) {
                if (!isDragging) return
                
                e.preventDefault()
                e.stopPropagation()
                
                currentX = e.clientX - initialX
                currentY = e.clientY - initialY
                
                // 应用新位置
                popupElement.style.position = 'fixed'
                popupElement.style.left = currentX + 'px'
                popupElement.style.top = currentY + 'px'
                popupElement.style.transform = 'none'
                popupElement.style.zIndex = '99999'  // 确保在最上层
                
                // console.log('拖拽中，位置:', { left: currentX, top: currentY })
              }
              
              // 鼠标释放结束拖拽
              const handleMouseUp = function(e) {
                if (isDragging) {
                  isDragging = false
                  popupElement.style.cursor = 'move'  // 恢复为可移动光标
                  
                  const rect = popupElement.getBoundingClientRect()
                  const finalPosition = {
                    left: rect.left + 'px',
                    top: rect.top + 'px'
                  }
                  
                  // 保存新位置（无论是否在固定模式）
                  savedPopupPosition.value = finalPosition
                  console.log('拖拽结束，保存新位置:', savedPopupPosition.value)
                  
                  // 如果在固定模式，自动保持固定
                  if (popupFixedMode.value) {
                    popupElement.style.position = 'fixed'
                    popupElement.style.transform = 'none'
                    console.log('固定模式，位置已保存')
                  }
                }
              }
              
              // 绑定拖拽事件
              dragHandle.addEventListener('mousedown', handleMouseDown)
              document.addEventListener('mousemove', handleMouseMove)
              document.addEventListener('mouseup', handleMouseUp)
              
              console.log('已启用弹窗拖拽功能')
            }
          }, 50)  // 增加延迟时间，确保DOM已渲染
        })
        
        graphicLayer.addGraphic(pointGraphic)
        trackPoints.push(pointGraphic)  // 保存点位引用到当前轨迹数组
      })
      
      console.log('轨迹', currentTrackIdentifier, '创建了', trackPoints.length, '个点位')
    }

    setTimeout(() => {
      initype.value = bool
    }, 0)
  }
  function listsetmor(newval) {
    const relist = []
    // // // console.log(newval)
    if (newval && newval.length > 0) {
      const porplist = newval
      for (let i = 0; i < porplist.length; i++) {
        const porpitem = porplist[i]
        const listone = [porpitem.longitude, porpitem.latitude, '1']
        relist.push(listone)
      }
    }
    return relist
  }
  
  // 添加临时标记点
  function addTempMarker(longitude, latitude) {
    if (!map.value) {
      console.error('地图未初始化')
      return false
    }
    
    // 验证经纬度
    if (!longitude || !latitude || isNaN(longitude) || isNaN(latitude)) {
      console.error('无效的经纬度')
      return false
    }
    
    // 如果临时图层不存在，创建一个
    if (!tempMarkerLayer.value) {
      tempMarkerLayer.value = new mars3d.layer.GraphicLayer()
      map.value.addLayer(tempMarkerLayer.value)
    }
    
    // 创建临时标记点
    const tempMarker = new mars3d.graphic.PointEntity({
      position: [longitude, latitude, 0],
      style: {
        color: '#FF4500',
        pixelSize: 12,
        outlineColor: '#ffffff',
        outlineWidth: 2,
        scaleByDistance: new window.Cesium.NearFarScalar(1000, 1.0, 500000, 0.3),
        disableDepthTestDistance: Number.POSITIVE_INFINITY
      },
      attr: {
        type: 'tempMarker',
        longitude: longitude,
        latitude: latitude
      },
      popup: `
        <div style="padding: 15px; min-width: 220px; background: white; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1);">
          <h4 style="margin: 0 0 12px 0; color: #FF4500; font-size: 16px; font-weight: 600; border-bottom: 2px solid #FF4500; padding-bottom: 8px;">
            📍 临时标记点
          </h4>
          <div style="margin: 8px 0;">
            <span style="color: #666; font-size: 13px;">经度：</span>
            <span style="color: #333; font-weight: 500; font-size: 14px;">${longitude.toFixed(6)}</span>
          </div>
          <div style="margin: 8px 0;">
            <span style="color: #666; font-size: 13px;">纬度：</span>
            <span style="color: #333; font-weight: 500; font-size: 14px;">${latitude.toFixed(6)}</span>
          </div>
          <div style="margin-top: 12px; padding-top: 12px; border-top: 1px solid #eee; color: #999; font-size: 12px;">
            提示：刷新地图后此标记将被清除
          </div>
        </div>
      `,
      popupOptions: {
        closeOnClick: false,
        autoClose: false,
        anchor: [0, -10]
      }
    })
    
    // 添加到图层
    tempMarkerLayer.value.addGraphic(tempMarker)
    
    // 飞行到该点位
    map.value.flyToPoint(tempMarker.position, {
      radius: 1000,
      duration: 1
    })
    
    // 自动打开弹窗
    setTimeout(() => {
      tempMarker.openPopup()
    }, 1000)
    
    return true
  }
  
  // 清除所有临时标记点
  function clearTempMarkers() {
    if (tempMarkerLayer.value) {
      tempMarkerLayer.value.clear()
    }
  }
  
  //methods end
  defineExpose ({initMap, delmars, addmars, drawyellowsload, drawyellowsloadDel, addTempMarker, clearTempMarkers})
</script>
