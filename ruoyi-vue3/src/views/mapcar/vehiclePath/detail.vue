<template>
  <div class="home">
    <line1 ref="lineRef1" @rest="resent" v-if="showline == 0"/>
    <line2 ref="lineRef2" @rest="resent" v-if="showline == 1"/>
  </div>
 </template>
 
 <script setup>
  import line1 from '@/views/mapcar/vehiclePath/components/line1.vue'
  import line2 from '@/views/mapcar/vehiclePath/components/line2.vue'
  import { onMounted, ref, nextTick, computed } from "vue"

  //data return start
  const showline = ref(0)
  //data return end

  //computed start
  //computed end
  //methods start
  async function resent(id){
    showline.value = id
  }
  //methods end
 </script>
 
 <style scoped lang="scss">
  .home{
    width: 100%;
  }
 </style>
 
 