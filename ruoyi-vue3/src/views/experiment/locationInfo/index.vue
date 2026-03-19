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
            
            <el-form-item label="货场ID">
              <el-input
                v-model="locationForm.buildingId"
                placeholder="请输入货场ID（不填则使用默认配置）"
                clearable
                style="width: 400px"
              />
            </el-form-item>

            <el-form-item label="卡片ID" prop="cardId">
              <el-input
                v-model="locationForm.cardId"
                placeholder="请输入卡片ID，多个ID用英文逗号分隔"
                clearable
                style="width: 300px"
              />
              <el-button
                type="success"
                icon="MagicStick"
                style="margin-left: 8px"
                :loading="loadingCardIds"
                @click="handleFillEnabledCardIds"
              >自动填入</el-button>
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
import { getEnabledCardIds } from '@/api/experiment/locationInfo';
const { proxy } = getCurrentInstance();

const loadingCardIds = ref(false);

const data = reactive({
  activeTab: 'location',
  locationForm: {
    locationType: 'zq',
    buildingId: null,
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

/** 自动填入当前货场所有启用的卡ID */
function handleFillEnabledCardIds() {
  loadingCardIds.value = true;
  getEnabledCardIds(locationForm.value.buildingId).then(res => {
    const ids = res.data;
    if (!ids || ids.length === 0) {
      proxy.$modal.msgWarning('未查询到启用的卡ID');
      return;
    }
    locationForm.value.cardId = ids.join(',');
    proxy.$modal.msgSuccess(`已填入 ${ids.length} 个启用卡ID`);
  }).catch(() => {
    proxy.$modal.msgError('查询启用卡ID失败');
  }).finally(() => {
    loadingCardIds.value = false;
  });
}

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
      if (locationForm.value.buildingId) {
        params.buildingId = locationForm.value.buildingId;
      }
      
      // 根据卡片数量生成文件名（多卡打包为zip）
      const cardIdList = locationForm.value.cardId.split(',').map(s => s.trim()).filter(s => s);
      const fileName = cardIdList.length > 1
        ? `${locationForm.value.locationType}_points_${new Date().getTime()}.zip`
        : `${locationForm.value.locationType}_points_${new Date().getTime()}.json`;
      
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
    buildingId: null,
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
