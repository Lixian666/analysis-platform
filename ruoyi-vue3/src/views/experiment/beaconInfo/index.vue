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
      <el-form-item label="信标ID" prop="beaconId">
        <el-input
            v-model="queryParams.beaconId"
            placeholder="请输入信标ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="RFID名称" prop="rfidName" label-width="80px">
        <el-input
            v-model="queryParams.rfidName"
            placeholder="请输入RFID名称"
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
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable :style="{width: '200px'}">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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

    <el-table ref="beaconInfoTableRef" v-loading="loading" :data="beaconInfoList" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" align="center" width="50" />
      <el-table-column label="名称" align="center" prop="name" sortable="custom" :sort-orders="['descending', 'ascending']" />
      <el-table-column label="RFID名称" align="center" prop="rfidName" />
      <el-table-column label="类型" align="center" prop="type" sortable="custom" :sort-orders="['descending', 'ascending']">
        <template #default="scope">
          <dict-tag :options="tracker_beacon_type" :value="scope.row.type"/>
        </template>
      </el-table-column>
      <el-table-column label="区域（靠近铁路或出入口为A）" align="center" prop="area" sortable="custom" :sort-orders="['descending', 'ascending']"/>
      <el-table-column label="建筑名称" align="center" prop="buildName" />
      <el-table-column label="建筑ID" align="center" prop="buildId" />
      <el-table-column label="信标ID" align="center" prop="beaconId" />
      <el-table-column label="位置" align="center" prop="location" />
      <el-table-column label="经度" align="center" prop="longitude" />
      <el-table-column label="纬度" align="center" prop="latitude" />
      <el-table-column label="感应距离(m)" align="center" prop="distance" />
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="0"
            :inactive-value="1"
            @change="handleStatusChange(scope.row)"
            :disabled="!canEdit"
          />
        </template>
      </el-table-column>
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
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改信标信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="beaconInfoFormRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="RFID名称" prop="rfidName">
          <el-input v-model="form.rfidName" placeholder="请输入RFID名称" />
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
          <el-input v-model="form.beaconId" placeholder="请输入信标ID" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input v-model="form.longitude" placeholder="请输入经度" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input v-model="form.latitude" placeholder="请输入纬度" />
        </el-form-item>
        <el-form-item label="感应距离(m)" prop="distance">
          <el-input-number v-model="form.distance" :precision="2" :step="0.1" :min="0" placeholder="请输入感应距离" style="width: 100%" />
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
import { computed } from "vue";
import { listBeaconInfo, getBeaconInfo, delBeaconInfo, addBeaconInfo, updateBeaconInfo, changeBeaconStatus, importTemplate } from "@/api/experiment/beaconInfo";
import { getToken } from "@/utils/auth";
import { UploadFilled } from '@element-plus/icons-vue';

const { proxy } = getCurrentInstance();
const { tracker_beacon_type } = proxy.useDict('tracker_beacon_type');
const canEdit = computed(() => proxy.$auth.hasPermi('experiment:beaconInfo:edit'));

const beaconInfoList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ prop: "name", order: "ascending" });

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
    rfidName: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    longitude: null,
    latitude: null,
    status: null,
    distance: null,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    rfidName: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    status: 0,
    distance: null,
    createTime: null,
    updateTime: null,
    orderByColumn: undefined,
    isAsc: undefined
  },
  rules: {
    name: [
      { required: true, message: "名称不能为空", trigger: "blur" }
    ],
    rfidName: [
      { required: false, message: "", trigger: "blur" }
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
    longitude: [
      { required: true, message: "经度不能为空", trigger: "blur" }
    ],
    latitude: [
      { required: true, message: "纬度不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态，0-启用，1-禁用不能为空", trigger: "change" }
    ],
    distance: [
      { required: true, message: "感应距离不能为空", trigger: "blur" },
      { type: "number", min: 0, message: "感应距离不能小于0", trigger: ["blur", "change"] }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);


/** 查询信标信息列表 */
function getList() {
  loading.value = true;
  listBeaconInfo(queryParams.value).then(response => {
    beaconInfoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
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
    rfidName: null,
    type: null,
    area: null,
    buildName: null,
    buildId: null,
    beaconId: null,
    location: null,
    status: null,
    distance: null,
    createTime: null,
    updateTime: null
  };
  proxy.resetForm("beaconInfoFormRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  queryParams.value.pageNum = 1;
  queryParams.value.status = 0;
  queryParams.value.orderByColumn = undefined;
  queryParams.value.isAsc = undefined;
  proxy.$refs["beaconInfoTableRef"].sort(defaultSort.value.prop, defaultSort.value.order);
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

/** 新增按钮操作 */
function handleAdd() {
  newReset();
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
  proxy.$refs["beaconInfoFormRef"].validate(valid => {
    if (valid) {
      // 将空字符串转换为null，确保能够清空RFID名称
      const submitData = { ...form.value };
      if (submitData.rfidName === '') {
        submitData.rfidName = null;
      }
      if (form.value.id != null) {
        updateBeaconInfo(submitData).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBeaconInfo(submitData).then(response => {
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

/** 状态修改 */
function handleStatusChange(row) {
  const status = row.status;
  const original = status === 1 ? 0 : 1;
  const text = status === 1 ? "禁用" : "启用";
  proxy.$modal
    .confirm('确认要' + text + '名称为"' + row.name + '"的信标信息吗？')
    .then(() => {
      return changeBeaconStatus({ id: row.id, status: status });
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
    })
    .catch(() => {
      row.status = original;
    });
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
