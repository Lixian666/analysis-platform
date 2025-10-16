<template>
  <div id="marsbox" style="height: 100%;">
    <div id="mars3dContainer" style="height: 100%;"/>
  </div>
</template>
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
  // 点位选择相关
  const selectedPointIndex = ref(-1)  // 当前选中的点位索引
  const currentTrackPoints = ref([])  // 当前轨迹的所有点位图形对象
  const currentTrackData = ref([])    // 当前轨迹的原始数据
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
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,1,true, item.takBehaviorRecordDetailList)
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
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,arrayList.value.length,true, item.takBehaviorRecordDetailList)
  }
  //生命周期 end
  //methods start
  
  // 键盘事件处理
  function handleKeyDown(event) {
    // 只在有选中点位时响应
    if (selectedPointIndex.value === -1 || currentTrackPoints.value.length === 0) {
      return
    }
    
    // 左箭头 = 上一个点，右箭头 = 下一个点
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault()
      selectPreviousPoint()
    } else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault()
      selectNextPoint()
    }
  }
  
  // 选择上一个点
  function selectPreviousPoint() {
    if (selectedPointIndex.value > 0) {
      selectPointByIndex(selectedPointIndex.value - 1)
    }
  }
  
  // 选择下一个点
  function selectNextPoint() {
    if (selectedPointIndex.value < currentTrackPoints.value.length - 1) {
      selectPointByIndex(selectedPointIndex.value + 1)
    }
  }
  
  // 根据索引选择点位
  function selectPointByIndex(index) {
    if (index < 0 || index >= currentTrackPoints.value.length) {
      return
    }
    
    // 取消之前的高亮
    if (selectedPointIndex.value >= 0 && currentTrackPoints.value[selectedPointIndex.value]) {
      const prevPoint = currentTrackPoints.value[selectedPointIndex.value]
      const normalCanvas = prevPoint.attr.normalCanvas
      if (normalCanvas) {
        prevPoint.setStyle({ image: normalCanvas })  // 恢复普通状态
      }
    }
    
    // 设置新的选中点
    selectedPointIndex.value = index
    const currentPoint = currentTrackPoints.value[index]
    
    // 高亮当前点（替换为带黄色外圈的图标）
    const selectedCanvas = currentPoint.attr.selectedCanvas
    if (selectedCanvas) {
      currentPoint.setStyle({ image: selectedCanvas })
    }
    
    // 显示弹窗
    currentPoint.openPopup()
    
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
      moveCarDirection(grap, listsetmor(element.takBehaviorRecordDetailList),end,element.color,newlist.length,true, element.takBehaviorRecordDetailList)
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
    graphicLayer_map2Dtdt.value = new mars3d.layer.WmtsLayer({
      url: proxy.$tdt_img,
      layer: "img",
      style: "default",
      tileMatrixSetID: "w",
      format: "tiles",
      maximumLevel: 18,
      show: true,
      zIndex: 1  // 底图层级
    })
    map.value.addLayer(graphicLayer_map2Dtdt.value)
    
    // 叠加董家镇本地TIF切片图层（TMS格式）
    graphicLayer_map2D.value = new mars3d.layer.XyzLayer({
      name: "董家镇DOM影像",
      url: proxy.$dongjiazhenTiles,
      tms: true, // 使用TMS坐标系（Y轴从下往上）
      minimumLevel: 10,
      maximumLevel: 18,
      opacity: 1,  // 完全不透明
      show: true,
      zIndex: 10,  // 更高的层级，显示在天地图之上
      // 使用tilemapresource.xml中的精确边界（EPSG:4326）
      rectangle: {
        xmin: 117.25785175068449,
        ymin: 36.75709311589477,
        xmax: 117.27989964583475,
        ymax: 36.76446135333450
      },
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
  function moveCarDirection(graphicLayer, pos, bool, color, num, showbool, originalData) {
    if (!pos || pos.length === 0) {
      return
    }
    let cargo = pos
    let linecolor = color
    // 保存原始数据，用于显示详细信息（recordTime, speed等）
    const dataPoints = originalData || []
    
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
      // 清空之前的点位数据
      currentTrackPoints.value = []
      currentTrackData.value = dataPoints
      selectedPointIndex.value = -1
      
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
            <div style="padding: 12px; min-width: 260px; background: #fff;">
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
                <div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; color: #999; font-size: 12px; text-align: center;">
                  💡 选中后显示黄色外圈，使用 ← → 方向键切换
                </div>
              </div>
            </div>
          `
        })
        
        // 添加点击事件监听
        pointGraphic.on('click', function(event) {
          const pointIndex = currentTrackPoints.value.indexOf(pointGraphic)
          if (pointIndex >= 0) {
            selectPointByIndex(pointIndex)
          }
        })
        
        graphicLayer.addGraphic(pointGraphic)
        currentTrackPoints.value.push(pointGraphic)  // 保存点位引用
      })
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
  //methods end
  defineExpose ({initMap, delmars, addmars, drawyellowsload, drawyellowsloadDel})
</script>
