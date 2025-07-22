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
  const graphicLayer_3D =ref(null) 
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
  //data return end
  //生命周期start
  onBeforeMount(()=>{

  })
  onMounted(()=>{

    //initMap()
    // this.initMap()
    //   if (this.list && this.list.length !== 0) {
    //     this.moveCarDirection(this.graphicLayer_3D, this.$route.query, this.listsetmor(this.list))
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
      if(graphicLayer_3D.value){
         graphicLayer_3D.value.remove()
        graphicLayer_3D.value = null
      }
      if(locationBar.value){
        locationBar.value.remove()
        locationBar.value = null
      }
      map.value = null
      // for (let i = 0; i < graphicobja.value.length; i++) {
      //  // graphicobja.value[i].remove()
      // }
  })
  function delmars(){
    if(initype.value){
      if(graphicLayer_3D.value){
        // // // // console.log('状态1',graphicLayer_3D.value)
        graphicLayer_3D.value.remove()
        // // // // console.log('状态2',graphicLayer_3D.value)
        graphicLayer_3D.value = null
        // // // // console.log('状态3',graphicLayer_3D.value)
      }
    }
  }
  
  //生命周期 end
  //methods start
  function initMap(newlist) {

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





   // 
  }
  function initdraw(newlist){
    graphicLayer_3D.value = new mars3d.layer.GraphicLayer()
    map.value.addLayer(graphicLayer_3D.value)
    // // // // console.log(graphicLayer_3D.value)
    // // // console.log(newlist)
    for (let index = 0; index < newlist.length; index++) {
      
      const element = newlist[index];
      // // // // console.log(element.takBehaviorRecordDetailList)
      let end = false
      if(index == newlist.length-1){
        end = true
      }
      // // // console.log(element.takBehaviorRecordDetailList)
      moveCarDirection(graphicLayer_3D.value, listsetmor(element.takBehaviorRecordDetailList),end,element.color,newlist.length)
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

  function moveCarDirection(graphicLayer, pos,bool,color,num) {
    if(pos && pos.length==0){
      return
    }
    let cargo = pos
    let linecolor = color
    if(num<=1){
      const graphicqa = new mars3d.graphic.PolylinePrimitive({
        positions: cargo,
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
        positions: cargo,
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
        position:[ cargo[0][0],cargo[0][1],0],
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
        position:[ cargo[cargo.length - 1][0],cargo[cargo.length - 1][1],0],
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
        positions: cargo,
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
        position:[ cargo[0][0],cargo[0][1],0],
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
        position:[ cargo[cargo.length - 1][0],cargo[cargo.length - 1][1],0],
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
    


    //graphicLayer.addGraphic(fixedRoute)
    // const that = this
    // if (maptype) {
    //   fixedRoute.autoSurfaceHeight().then(function (e) {
    //     startFly(fixedRoute)
    //   })
    // } else {
    //   startFly(fixedRoute)
    // }
    setTimeout(()=>{
      initype.value = bool
    },0)
   
  }

  function startFly(fixedRoute) {
    fixedRoute.start()
  }

  function listsetmor(newval) {
    const relist = []
    // console.log(newval)
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



  defineExpose ({initMap, delmars,initdraw})



  // export default {

    
  
  //   methods: {
  //   /*  async addDrawTool() {
  //       this.flag = false
  //       this.twoFlag = false
  //       // 添加绘制工具
  //       this.map.graphicLayer.startDraw({
  //         type: 'polygon', // 绘制类型
  //         style: {
  //           color: '#c43c39',
  //           opacity: 1,
  //           width: 10,
  //           zIndex: 1000,
  //           clampToGround: true, // 开启贴地
  //         }
  //       });
  //     },*/
  //     nameValidate(rule, value, callback) {
  //       if (value) {
  //         railList({name:value}).then(res=>{
  //           if(res.data.length>0){
  //             callback(new Error("名称已经存在"))
  //           }else{
  //             callback()
  //           }
  //         })
  //       }
  //     },
  //     chanagetype() {
  //       this.graphicLayer_map3D.remove(true)
  //       this.graphicLayer_3D.remove(true)
  //       this.addTileLayer()
  //       location.reload()
  //     },
  //     updateCarDrive(list){
  //       this.moveCarDirection(this.graphicLayer_3D, this.$route.query, this.listsetmor(list))
  //     },
  //     updateDate(list) {
  //       this.moveCarDirection(this.graphicLayer_3D, this.$route.query, this.listsetmor(list))
  //     },
  //     removeG() {
  //       this.graphicLayer_3D.clear()
  //     },
  //     clearData() {
  //       this.zList = []
  //     },
  //     getPeopleList(list,flag) {
  //       this.removeG()
  //       if(flag){
  //         this.showRail(this.polygonEntityList)
  //       }
  //       if(list){
  //         list.forEach(item => {
  //           this.addPersonList(item)
  //         })
  //       }
  //     },
  //     showRail(list){
  //       if (list.length > 0) {
  //         for (let i = 0; i <list.length; i++) {
  //           this.addPolygonEntityList(this.graphicLayer_3D, list[i])
  //         }
  //       }
  //     },
    
  //     async save() {
  //       this.$refs['form'].validate((valid) => {
  //         if (valid) {
  //           this.poData.name = this.form.name
  //           this.zList.push(this.poData)
  //           if (this.$parent.getPolygonEntityList) {
  //             this.$parent.getPolygonEntityList(this.zList)
  //           }
  //           this.open = false
  //         }
  //       })
  //     },



  //     tiles3D() {
  //       // 引用3D模型切片
  //       this.graphicLayer_map3D = new mars3d.layer.TilesetLayer({
  //         pid: 2024,
  //         name: '鱼嘴货场',
  //         type: '3dtiles',
  //         url: this.ModelMapYuzui,
  //         luminanceAtZenith: 0.3,
  //         position: {
  //           alt: 35
  //         }
  //       })
  //       this.map.addLayer(this.graphicLayer_map3D)
  //     },


  //     removePolygonEntity(list){
  //       list.forEach(item=>{
  //         item.remove()
  //       })
  //     },
  //     //添加围栏
  //     addPolygonEntityList(graphicLayer, position) {
  //       const graphic = new mars3d.graphic.PolygonEntity({
  //         positions: position,
  //         style: {
  //           color: "#c43c39",
  //           opacity: 0.7,
  //           zIndex: 1000,
  //           clampToGround:true
  //         },
  //       })
  //       this.initGraphicManager(graphic, graphicLayer)
  //       graphicLayer.addGraphic(graphic)
  //       this.$parent.getGList(graphic)
  //     },


  //     //右键删除对象
  //     initGraphicManager(graphic, graphicLayer) {
  //       // 绑定右键菜单
  //       graphic.bindContextMenu([
  //         {
  //           text: "开始编辑对象",
  //           icon: "fa fa-edit",
  //           show: function (e) {
  //             const graphic = e.graphic
  //             if (!graphic || !graphic.hasEdit) {
  //               return false
  //             }
  //             return !graphic.isEditing
  //           },
  //           callback: (e) => {
  //             const graphic = e.graphic
  //             if (!graphic) {
  //               return false
  //             }
  //             if (graphic) {
  //               graphicLayer.startEditing(graphic)
  //             }
  //           }
  //         },
  //         {
  //           text: "删除对象",
  //           icon: "fa fa-trash-o",
  //           callback: (e) => {
  //             const graphic = e.graphic
  //             if (graphic) {
  //               graphic.remove()
  //             }
  //           }
  //         }
  //       ])
  //     },

  //     //定位
  //     addPersonList(row) {
  //       const graphic = new mars3d.graphic.BillboardEntity({
  //         positions: row.list,
  //         viewFrom: new Cesium.Cartesian3(-500, -500, 200),
  //         style: {
  //           image: position,
  //           width: 35,
  //           height: 35,
  //           horizontalOrigin: Cesium.HorizontalOrigin.CENTER,
  //           verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
  //           scaleByDistance: new Cesium.NearFarScalar(10000, 1.0, 500000, 0.1),
  //           label: {
  //             combine: true,
  //             text: row.name,
  //             font_size: 16,
  //             color: "#0a0a0a",
  //             pixelOffsetY: -50,
  //             background: true,
  //             visibleDepth: true,
  //             backgroundColor: '#79a4d6',
  //             distanceDisplayCondition: true,
  //             distanceDisplayCondition_far: 50000,
  //             distanceDisplayCondition_near: 0
  //           }
  //         },
  //       })
  //       this.graphicLayer_3D.addGraphic(graphic)
  //     }
  //   }
  // }
</script>
