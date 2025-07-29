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
    map.value = new mars3d.Map('mars3dContainer', {
      scene: {
        center: proxy.$center,
        showSun: true,
        showMoon: true,
        showSkyBox: true,
        showSkyAtmosphere: false, // 关闭球周边的白色轮廓 map.scene.skyAtmosphere = false
        fog: true,
        fxaa: false,//优化，抗锯齿关闭效果好点
        globe: {
          showGroundAtmosphere: false, // 关闭大气（球表面白蒙蒙的效果）
          depthTestAgainstTerrain: false,
          baseColor: '#546a53'
        },
        mapProjection: mars3d.CRS.EPSG3857, // 2D下展示墨卡托投影
        mapMode2D: Cesium.MapMode2D.INFINITE_SCROLL,// 2D下左右一直可以滚动重复世界地图
        resolutionScale: 0.8,//优化
        contextOptions: {//优化
          webgl: {
            antialias: false
          }
        },
        msaaSamples: 8,//优化
      }
    })
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
