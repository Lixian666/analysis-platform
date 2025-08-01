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
  //data return end
  //生命周期start
  onBeforeMount(()=>{

  })
  onMounted(()=>{

    //initMap()
    // this.initMap()
    //   if (this.list && this.list.length !== 0) {
    //     this.moveCarDirection(this.graphicLayer_carlines, this.$route.query, this.listsetmor(this.list))
    //   }

  })
  onUnmounted(()=>{
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
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,1,true)
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
    moveCarDirection(grap, listsetmor(item.takBehaviorRecordDetailList),true,item.color,arrayList.value.length,true)
  }
  //生命周期 end
  //methods start
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
      moveCarDirection(grap, listsetmor(element.takBehaviorRecordDetailList),end,element.color,newlist.length,true)
    }
  }
  function addTileLayer() {
    graphicLayer_map2Dtdt.value = new mars3d.layer.XyzLayer({
      url: proxy.$tdt,
      opacity: 1
    })
    map.value.addLayer(graphicLayer_map2Dtdt.value)
    graphicLayer_map2D.value = new mars3d.layer.XyzLayer({
      url: proxy.$tifimg,
      opacity: 1
    })
    map.value.addLayer(graphicLayer_map2D.value)
  }
  function camerahistory (){
    cameraHistory.value = new mars3d.thing.CameraHistory({
      limit: {
        // 限定视角范围
        position: Cesium.Cartesian3.fromDegrees(106.733082, 29.620789, 34.85),
        radius: 5000.0,
        debugExtent: false
      },
      maxCacheCount: 999
    })
    map.value.addThing(cameraHistory.value)
    const eventTarget = new mars3d.BaseClass()
    cameraHistory.value.on(mars3d.EventType.change, function (event) {
      // 触发自定义事件
      const count = event.count
      eventTarget.fire("changeCamera", { count })
    })
  }
  function moveCarDirection(graphicLayer, pos,bool,color,num,showbool) {
    if(pos && pos.length==0){
      return
    }
    let cargo = pos
    let linecolor = color
    if(num<=1){
      const graphicqa = new mars3d.graphic.PolylinePrimitive({
        positions: cargo,
        show:showbool,
        style: {
          width: 7,
          materialType: mars3d.MaterialType.LineFlow,
          materialOptions: {
            image: linepng,
            speed: 8,
            //repeat: new Cesium.Cartesian2(cargo.length / 2, 1.0),
            // color: '#3388FF',
            // color: Cesium.Color.CHARTREUSE,
            // image: 'http://mars3d.cn/img/textures/line-arrow-dovetail.png',
            // speed: 20
          },
          //clampToGround: true
        }
      })
      graphicLayer.addGraphic(graphicqa)
      const graphicq = new mars3d.graphic.PolylinePrimitive({
        positions: cargo,show:showbool,
        style: {
          color: "#f5062",
          materialType: mars3d.MaterialType.LineFlowColor,
          lastMaterialType: "PolylineArrow",
          width: 3,
          materialOptions: {
            color: "#f50620",
            speed: 0.3,
            percent: 0.35,
            alpha: 0.55
          }
        },
      })
      graphicLayer.addGraphic(graphicq)
      const graphics = new mars3d.graphic.BillboardEntity({
        position:[ cargo[0][0],cargo[0][1],0],show:showbool,
        style: {
          image: startpng,
          scale: 1,
          horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
          verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          label: {
            text: "",
            font_size: 18,
            color: "#ffffff",
            pixelOffsetY: 0,
          }
        },
        attr: { remark: "" }
      })
      graphicLayer.addGraphic(graphics)
      const graphice = new mars3d.graphic.BillboardEntity({
        position:[ cargo[cargo.length - 1][0],cargo[cargo.length - 1][1],0],show:showbool,
        style: {
          image: endpng,
          scale: 1,
          horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
          verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
          label: {
            text: "",
            font_size: 18,
            color: "#ffffff",
            pixelOffsetY: 0,
          }
        },
        attr: { remark: "" }
      })
      graphicLayer.addGraphic(graphice)

    }else{
      const graphicq = new mars3d.graphic.PolylinePrimitive({
        positions: cargo,show:showbool,
        style: {
          color: linecolor,
        //  materialType: mars3d.MaterialType.LineFlowColor,
          lastMaterialType: "PolylineArrow",
          width: 3,
          materialOptions: {
            color: linecolor,
            speed: 0.3,
            percent: 0.35,
            alpha: 0.55
          }
        },
      })
      graphicLayer.addGraphic(graphicq)
    }    
    const graphics = new mars3d.graphic.BillboardEntity({
      position:[ cargo[0][0],cargo[0][1],0],show:showbool,
      style: {
        image: startpng,
        scale: 1,
        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        label: {
          text: "",
          font_size: 18,
          color: "#ffffff",
          pixelOffsetY: 0,
        }
      },
      attr: { remark: "" }
    })
    graphicLayer.addGraphic(graphics)
    const graphice = new mars3d.graphic.BillboardEntity({
      position:[ cargo[cargo.length - 1][0],cargo[cargo.length - 1][1],0],show:showbool,
      style: {
        image: endpng,
        scale: 1,
        horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
        verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
        label: {
          text: "",
          font_size: 18,
          color: "#ffffff",
          pixelOffsetY: 0,
        }
      },
      attr: { remark: "" }
    })
    graphicLayer.addGraphic(graphice)
    setTimeout(()=>{
      initype.value = bool
    },0)
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
