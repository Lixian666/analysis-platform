<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>数据导出</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="card">
        <!-- 定位卡数据导出 -->
        <el-tab-pane label="定位卡数据" name="location">
          <el-form :model="locationForm" ref="locationFormRef" :rules="locationRules" label-width="120px" style="max-width: 600px">
            <el-form-item label="类型" prop="locationType">
              <el-select
                v-model="locationForm.locationType"
                placeholder="请选择类型"
                style="width: 400px"
              >
                <el-option label="ZQ" value="zq" />
                <el-option label="XR" value="xr" disabled />
              </el-select>
            </el-form-item>
            
            <el-form-item label="卡片ID" prop="cardId">
              <el-input
                v-model="locationForm.cardId"
                placeholder="请输入卡片ID"
                clearable
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item label="开始时间" prop="startTimeStr">
              <el-date-picker
                v-model="locationForm.startTimeStr"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择开始时间"
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item label="结束时间" prop="endTimeStr">
              <el-date-picker
                v-model="locationForm.endTimeStr"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择结束时间"
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                icon="Download"
                @click="handleLocationExport"
                v-hasPermi="['experiment:locationInfo:export']"
              >导出定位卡数据</el-button>
              <el-button icon="Refresh" @click="resetLocationForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 视觉识别数据导出 -->
        <el-tab-pane label="视觉识别数据" name="vision">
          <el-form :model="visionForm" ref="visionFormRef" :rules="visionRules" label-width="120px" style="max-width: 600px">
            <el-form-item label="摄像机ID" prop="cameraIds">
              <el-input
                v-model="visionForm.cameraIds"
                type="textarea"
                :rows="3"
                placeholder="请输入摄像机ID，多个ID用英文逗号分隔，例如：camera1,camera2,camera3（留空则使用配置文件中的默认值）"
                clearable
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item label="开始时间" prop="startTimeStr">
              <el-date-picker
                v-model="visionForm.startTimeStr"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择开始时间"
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item label="结束时间" prop="endTimeStr">
              <el-date-picker
                v-model="visionForm.endTimeStr"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择结束时间"
                style="width: 400px"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button
                type="primary"
                icon="Download"
                @click="handleVisionExport"
                v-hasPermi="['experiment:locationInfo:export']"
              >导出视觉识别数据</el-button>
              <el-button icon="Refresh" @click="resetVisionForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="LocationInfo">
const { proxy } = getCurrentInstance();

const data = reactive({
  activeTab: 'location',
  locationForm: {
    locationType: 'zq',
    cardId: null,
    startTimeStr: null,
    endTimeStr: null
  },
  locationRules: {
    locationType: [
      { required: true, message: "类型不能为空", trigger: "change" }
    ],
    cardId: [
      { required: true, message: "卡片ID不能为空", trigger: "blur" }
    ],
    startTimeStr: [
      { required: true, message: "开始时间不能为空", trigger: "change" }
    ],
    endTimeStr: [
      { required: true, message: "结束时间不能为空", trigger: "change" }
    ]
  },
  visionForm: {
    cameraIds: null,
    startTimeStr: null,
    endTimeStr: null
  },
  visionRules: {
    startTimeStr: [
      { required: true, message: "开始时间不能为空", trigger: "change" }
    ],
    endTimeStr: [
      { required: true, message: "结束时间不能为空", trigger: "change" }
    ]
  }
});

const { activeTab, locationForm, locationRules, visionForm, visionRules } = toRefs(data);

/** 导出定位卡数据 */
function handleLocationExport() {
  proxy.$refs["locationFormRef"].validate(valid => {
    if (valid) {
      // 验证时间范围
      if (new Date(locationForm.value.startTimeStr) >= new Date(locationForm.value.endTimeStr)) {
        proxy.$modal.msgError("结束时间必须大于开始时间");
        return;
      }
      
      const params = {
        locationType: locationForm.value.locationType,
        cardId: locationForm.value.cardId,
        startTimeStr: locationForm.value.startTimeStr,
        endTimeStr: locationForm.value.endTimeStr
      };
      
      // 根据类型生成文件名
      const fileName = `${locationForm.value.locationType}_points_${new Date().getTime()}.json`;
      
      // 使用 proxy.download 方法下载文件
      proxy.download('experiment/locationInfo/exportPoints', params, fileName)
    }
  });
}

/** 导出视觉识别数据 */
function handleVisionExport() {
  proxy.$refs["visionFormRef"].validate(valid => {
    if (valid) {
      // 验证时间范围
      if (new Date(visionForm.value.startTimeStr) >= new Date(visionForm.value.endTimeStr)) {
        proxy.$modal.msgError("结束时间必须大于开始时间");
        return;
      }
      
      const params = {
        cameraIds: visionForm.value.cameraIds,
        startTimeStr: visionForm.value.startTimeStr,
        endTimeStr: visionForm.value.endTimeStr
      };
      
      // 生成文件名
      const fileName = `vision_events_${new Date().getTime()}.json`;
      
      // 使用 proxy.download 方法下载文件
      proxy.download('experiment/locationInfo/exportVisionEvents', params, fileName)
    }
  });
}

/** 重置定位卡表单 */
function resetLocationForm() {
  locationForm.value = {
    locationType: 'zq',
    cardId: null,
    startTimeStr: null,
    endTimeStr: null
  };
  proxy.resetForm("locationFormRef");
}

/** 重置视觉识别表单 */
function resetVisionForm() {
  visionForm.value = {
    cameraIds: null,
    startTimeStr: null,
    endTimeStr: null
  };
  proxy.resetForm("visionFormRef");
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}
</style>
