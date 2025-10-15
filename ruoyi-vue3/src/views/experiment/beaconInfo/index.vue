<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input
            v-model="queryParams.name"
            placeholder="请输入名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <!-- 修改此处：给 el-select 添加宽度样式 -->
        <el-select v-model="queryParams.type" placeholder="请选择类型" clearable :style="{width: '200px'}">
          <el-option
              v-for="dict in tracker_beacon_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="建筑名称" prop="buildName">
        <el-input
            v-model="queryParams.buildName"
            placeholder="请输入建筑名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="建筑ID" prop="buildId">
        <el-input
            v-model="queryParams.buildId"
            placeholder="请输入建筑ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="位置" prop="location">
        <el-input
            v-model="queryParams.location"
            placeholder="请输入位置"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="创建时间" prop="createTime">-->
<!--        <el-date-picker clearable-->
<!--                        v-model="queryParams.createTime"-->
<!--                        type="date"-->
<!--                        value-format="YYYY-MM-DD"-->
<!--                        placeholder="请选择创建时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
<!--      <el-form-item label="更新时间" prop="updateTime">-->
<!--        <el-date-picker clearable-->
<!--                        v-model="queryParams.updateTime"-->
<!--                        type="date"-->
<!--                        value-format="YYYY-MM-DD"-->
<!--                        placeholder="请选择更新时间">-->
<!--        </el-date-picker>-->
<!--      </el-form-item>-->
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['experiment:beaconInfo:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['experiment:beaconInfo:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['experiment:beaconInfo:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['experiment:beaconInfo:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['experiment:beaconInfo:import']"
        >导入</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="beaconInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" align="center" width="50" />
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="tracker_beacon_type" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="区域（靠近铁路为A）" align="center" prop="area" />
      <el-table-column label="建筑名称" align="center" prop="buildName" />
      <el-table-column label="建筑ID" align="center" prop="buildId" />
      <el-table-column label="信标ID" align="center" prop="beaconId" />
      <el-table-column label="位置" align="center" prop="location" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['experiment:beaconInfo:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['experiment:beaconInfo:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改信标信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="beaconInfoRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option
              v-for="dict in tracker_beacon_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="区域（靠近铁路为A）" prop="area">
          <el-select v-model="form.area" placeholder="请选择区域">
            <el-option
                v-for="area in areaOptions"
                :key="area"
                :label="area"
                :value="area"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="建筑名称" prop="buildName">
          <el-input v-model="form.buildName" placeholder="请输入建筑名称" />
        </el-form-item>
        <el-form-item label="建筑ID" prop="buildId">
          <el-input v-model="form.buildId" placeholder="请输入建筑ID" />
        </el-form-item>
        <el-form-item label="信标ID" prop="beaconId">
          <el-select v-model="form.beaconId" placeholder="请选择信标ID">
            <el-option
                v-for="beaconId in beaconIdList"
                :key="beaconId"
                :label="beaconId"
                :value="beaconId"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" clearable :style="{width: '100%'}">
            <el-option v-for="(item, index) in statusOptions" :key="index" :label="item.label"
                       :value="item.value" :disabled="item.disabled"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />覆盖已有数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="downloadImportTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BeaconInfo">
import { listBeaconInfo, getBeaconInfo, delBeaconInfo, addBeaconInfo, updateBeaconInfo, listBeaconId, importTemplate } from "@/api/experiment/beaconInfo";
import { getToken } from "@/utils/auth";
import { UploadFilled } from '@element-plus/icons-vue';

const { proxy } = getCurrentInstance();
const { tracker_beacon_type } = proxy.useDict('tracker_beacon_type');

const beaconIdList = ref([]);
const beaconInfoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const upload = reactive({
  open: false,
  title: "信标信息导入",
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: "Bearer " + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + "/experiment/beaconInfo/importData"
});

const statusOptions = ref([{
  "label": "启用",
  "value": 0
}, {
  "label": "禁用",
  "value": 1
}])

const areaOptions = ref(["A","B","C","D","E","F"]);

const data = reactive({
  form: {
    name: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    status: null,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    status: null,
    createTime: null,
    updateTime: null
  },
  rules: {
    name: [
      { required: true, message: "名称不能为空", trigger: "blur" }
    ],
    type: [
      { required: true, message: "类型不能为空", trigger: "change" }
    ],
    area: [
      { required: true, message: "区域不能为空", trigger: "blur" }
    ],
    beaconId: [
      { required: true, message: "信标ID不能为空", trigger: "change" }
    ],
    location: [
      { required: true, message: "位置不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态，0-启用，1-禁用不能为空", trigger: "change" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

onMounted(() => {
  // 在这里初始化 beaconIdList
  // 示例：beaconIdList.value = [1, 2, 3];
  // 或者调用 API 获取数据
  getBeaconIds();
});

/** 查询信标信息列表 */
function getList() {
  loading.value = true;
  listBeaconInfo(queryParams.value).then(response => {
    beaconInfoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** 查询信标ID列表 */
function getBeaconIds() {
  // 如果已经加载过数据，则不再重复加载
  if (beaconIdList.value && beaconIdList.value.length > 0) {
    return Promise.resolve();
  }
  return listBeaconId().then(response => {
    console.log("完整 response：", response);
    console.log("信标ID接口返回：", response.data);
    beaconIdList.value = response;
  }).catch(error => {
    console.error('获取信标ID列表失败:', error);
    proxy.$modal.message({ message: '获取信标ID列表失败', type: 'error' });
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function newReset() {
  form.value.id = null;
  form.value.createTime = null;
  form.value.updateTime = null;
  // proxy.resetForm("beaconInfoRef");
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    status: null,
    createTime: null,
    updateTime: null
  };
  proxy.resetForm("beaconInfoRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  newReset();
// 确保 beaconIdList 已经加载
  if (!beaconIdList.value || beaconIdList.value.length === 0) {
    getBeaconIds();
  }
  open.value = true;
  title.value = "添加信标信息";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getBeaconInfo(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改信标信息";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["beaconInfoRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateBeaconInfo(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBeaconInfo(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除信标信息编号为"' + _ids + '"的数据项？').then(function() {
    return delBeaconInfo(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('experiment/beaconInfo/export', {
    ...queryParams.value
  }, `beaconInfo_${new Date().getTime()}.xlsx`)
}

/** 导入按钮操作 */
function handleImport() {
  upload.open = true;
}

/** 下载模板操作 */
function downloadImportTemplate() {
  // 直接使用window.location.href来下载，确保使用GET方法
  window.location.href = import.meta.env.VITE_APP_BASE_API + "/experiment/beaconInfo/importTemplate";
}

// 文件上传中处理
function handleFileUploadProgress() {
  upload.isUploading = true;
}

// 文件上传成功处理
function handleFileSuccess(response) {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].clearFiles();
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
}

// 提交上传文件
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

getList();
</script>
