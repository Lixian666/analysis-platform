<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户ID" prop="cardId">
        <el-input
            v-model="queryParams.cardId"
            placeholder="请输入用户ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="货场ID" prop="yardId">
        <el-input
            v-model="queryParams.yardId"
            placeholder="请输入货场ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="轨迹编号" prop="trackId">
        <el-input
            v-model="queryParams.trackId"
            placeholder="请输入轨迹编号"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="行为类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择行为类型" clearable :style="{width: '200px'}">
          <el-option label="火车卸车" value="0" />
          <el-option label="火车装车" value="1" />
          <el-option label="板车卸车" value="2" />
          <el-option label="板车装车" value="3" />
          <el-option label="地跑入库" value="4" />
          <el-option label="地跑出库" value="5" />
        </el-select>
      </el-form-item>
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
          v-hasPermi="['experiment:experiment:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['experiment:experiment:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['experiment:experiment:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['experiment:experiment:export']"
        >导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['experiment:experiment:import']"
        >导入</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="behaviorRecordsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" align="center" width="50" />
      <el-table-column label="用户ID" align="center" prop="cardId" />
      <el-table-column label="货场ID" align="center" prop="yardId" />
      <el-table-column label="轨迹编号" align="center" prop="trackId" />
      <el-table-column label="轨迹起始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="轨迹结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="点数量" align="center" prop="pointCount" />
      <el-table-column label="行为类型" align="center" prop="type">
        <template #default="scope">
          <span>{{ formatType(scope.row.type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="持续时间" align="center" prop="duration" />
      <el-table-column label="状态" align="center" prop="state" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['experiment:experiment:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['experiment:experiment:remove']">删除</el-button>
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

    <!-- 添加或修改行为记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="behaviorRecordsRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户ID" prop="cardId">
          <el-input v-model="form.cardId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="货场ID" prop="yardId">
          <el-input v-model="form.yardId" placeholder="请输入货场ID" />
        </el-form-item>
        <el-form-item label="轨迹编号" prop="trackId">
          <el-input v-model="form.trackId" placeholder="请输入轨迹编号" />
        </el-form-item>
        <el-form-item label="轨迹起始时间" prop="startTime">
          <el-date-picker clearable
                          v-model="form.startTime"
                          type="datetime"
                          value-format="YYYY-MM-DD HH:mm:ss"
                          placeholder="请选择轨迹起始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="轨迹结束时间" prop="endTime">
          <el-date-picker clearable
                          v-model="form.endTime"
                          type="datetime"
                          value-format="YYYY-MM-DD HH:mm:ss"
                          placeholder="请选择轨迹结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="点数量" prop="pointCount">
          <el-input v-model="form.pointCount" placeholder="请输入点数量" />
        </el-form-item>
        <el-form-item label="行为类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择行为类型" clearable :style="{width: '100%'}">
            <el-option label="到达卸车" value="0" />
            <el-option label="发运装车" value="1" />
            <el-option label="轿运车装车" value="2" />
            <el-option label="轿运车卸车" value="3" />
            <el-option label="地跑入库" value="4" />
            <el-option label="地跑出库" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="持续时间" prop="duration">
          <el-input v-model="form.duration" placeholder="请输入持续时间" />
        </el-form-item>
        <el-form-item label="状态" prop="state">
          <el-input v-model="form.state" placeholder="请输入状态" />
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
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
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

<script setup name="BehaviorRecords">
import { listBehaviorRecords, getBehaviorRecords, delBehaviorRecords, addBehaviorRecords, updateBehaviorRecords, importBehaviorRecordsTemplate, importBehaviorRecords } from "@/api/experiment/behaviorRecords";
import { getToken } from "@/utils/auth";
import { UploadFilled } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance();

const behaviorRecordsList = ref([]);
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
  title: "行为记录导入",
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: "Bearer " + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + "/experiment/experiment/importData"
});

const data = reactive({
  form: {
    id: null,
    cardId: null,
    yardId: null,
    trackId: null,
    startTime: null,
    endTime: null,
    pointCount: null,
    type: null,
    duration: null,
    state: null,
    createTime: null,
    updateTime: null
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    cardId: null,
    yardId: null,
    trackId: null,
    startTime: null,
    endTime: null,
    pointCount: null,
    type: null,
    duration: null,
    state: null
  },
  rules: {
    cardId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    yardId: [
      { required: true, message: "货场ID不能为空", trigger: "blur" }
    ],
    trackId: [
      { required: true, message: "轨迹编号不能为空", trigger: "blur" }
    ],
    type: [
      { required: true, message: "行为类型不能为空", trigger: "change" }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

// 格式化行为类型显示
function formatType(type) {
  const typeMap = {
    '0': '到达卸车',
    '1': '发运装车',
    '2': '轿运车装车',
    '3': '轿运车卸车',
    '4': '地跑入库',
    '5': '地跑出库'
  };
  return typeMap[type] || type;
}

/** 查询行为记录列表 */
function getList() {
  loading.value = true;
  listBehaviorRecords(queryParams.value).then(response => {
    behaviorRecordsList.value = response.rows;
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
function reset() {
  form.value = {
    id: null,
    cardId: null,
    yardId: null,
    trackId: null,
    startTime: null,
    endTime: null,
    pointCount: null,
    type: null,
    duration: null,
    state: null,
    createTime: null,
    updateTime: null
  };
  proxy.resetForm("behaviorRecordsRef");
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
  reset();
  open.value = true;
  title.value = "添加行为记录";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getBehaviorRecords(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改行为记录";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["behaviorRecordsRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateBehaviorRecords(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBehaviorRecords(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除行为记录编号为"' + _ids + '"的数据项？').then(function() {
    return delBehaviorRecords(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('experiment/experiment/export', {
    ...queryParams.value
  }, `behaviorRecords_${new Date().getTime()}.xlsx`)
}

/** 导入按钮操作 */
function handleImport() {
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  importBehaviorRecordsTemplate().then(res => {
    proxy.download(res.msg);
  });
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
