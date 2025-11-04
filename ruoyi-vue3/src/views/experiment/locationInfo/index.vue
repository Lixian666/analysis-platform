<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>点位数据导出</span>
        </div>
      </template>
      
      <el-form :model="form" ref="locationInfoRef" :rules="rules" label-width="120px" style="max-width: 600px">
        <el-form-item label="类型" prop="locationType">
          <el-select
            v-model="form.locationType"
            placeholder="请选择类型"
            style="width: 400px"
          >
            <el-option label="ZQ" value="zq" />
            <el-option label="XR" value="xr" disabled />
          </el-select>
        </el-form-item>
        
        <el-form-item label="卡片ID" prop="cardId">
          <el-input
            v-model="form.cardId"
            placeholder="请输入卡片ID"
            clearable
            style="width: 400px"
          />
        </el-form-item>
        
        <el-form-item label="开始时间" prop="startTimeStr">
          <el-date-picker
            v-model="form.startTimeStr"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择开始时间"
            style="width: 400px"
          />
        </el-form-item>
        
        <el-form-item label="结束时间" prop="endTimeStr">
          <el-date-picker
            v-model="form.endTimeStr"
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
            @click="handleExport"
            v-hasPermi="['experiment:locationInfo:export']"
          >导出JSON文件</el-button>
          <el-button icon="Refresh" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="LocationInfo">
const { proxy } = getCurrentInstance();

const data = reactive({
  form: {
    locationType: 'zq',
    cardId: null,
    startTimeStr: null,
    endTimeStr: null
  },
  rules: {
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
  }
});

const { form, rules } = toRefs(data);

/** 导出按钮操作 */
function handleExport() {
  proxy.$refs["locationInfoRef"].validate(valid => {
    if (valid) {
      // 验证时间范围
      if (new Date(form.value.startTimeStr) >= new Date(form.value.endTimeStr)) {
        proxy.$modal.msgError("结束时间必须大于开始时间");
        return;
      }
      
      const params = {
        locationType: form.value.locationType,
        cardId: form.value.cardId,
        startTimeStr: form.value.startTimeStr,
        endTimeStr: form.value.endTimeStr
      };
      
      // 根据类型生成文件名
      const fileName = `${form.value.locationType}_points_${new Date().getTime()}.json`;
      
      // 使用 proxy.download 方法下载文件
      proxy.download('experiment/locationInfo/exportPoints', params, fileName)
    }
  });
}

/** 重置按钮操作 */
function resetForm() {
  form.value = {
    locationType: 'zq',
    cardId: null,
    startTimeStr: null,
    endTimeStr: null
  };
  proxy.resetForm("locationInfoRef");
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
