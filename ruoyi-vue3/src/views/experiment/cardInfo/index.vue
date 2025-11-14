<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" label-width="80px" :inline="true" class="mb-10">
      <el-form-item label="卡ID" prop="cardId">
        <el-input v-model="queryParams.cardId" placeholder="请输入卡ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="货场ID" prop="yardId">
        <el-input v-model="queryParams.yardId" placeholder="请输入货场ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="卡类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="请选择卡类型" clearable :style="{ width: '200px' }">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="请选择业务类型" clearable :style="{ width: '200px' }">
          <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="enabled">
        <el-select v-model="queryParams.enabled" placeholder="请选择状态" clearable :style="{ width: '200px' }">
          <el-option v-for="item in enabledOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['experiment:cardInfo:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['experiment:cardInfo:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['experiment:cardInfo:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['experiment:cardInfo:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['experiment:cardInfo:import']"
        >导入</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="cardList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="卡ID" align="center" prop="cardId" />
      <el-table-column label="货场ID" align="center" prop="yardId" />
      <el-table-column label="货场名称" align="center" prop="yardName" />
      <el-table-column label="卡类型" align="center" prop="type">
        <template #default="scope">
          <dict-tag :options="typeOptions" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column label="业务类型" align="center" prop="bizType">
        <template #default="scope">
          <dict-tag :options="bizTypeOptions" :value="scope.row.bizType != null ? String(scope.row.bizType) : ''" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="enabled" width="120">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            :active-value="0"
            :inactive-value="1"
            @change="handleStatusChange(scope.row)"
            :disabled="!canEdit"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['experiment:cardInfo:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['experiment:cardInfo:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="卡ID" prop="cardId">
          <el-input v-model="form.cardId" placeholder="请输入卡ID" />
        </el-form-item>
        <el-form-item label="货场ID" prop="yardId">
          <el-input v-model="form.yardId" placeholder="请输入货场ID" />
        </el-form-item>
        <el-form-item label="货场名称" prop="yardName">
          <el-input v-model="form.yardName" placeholder="请输入货场名称" />
        </el-form-item>
        <el-form-item label="卡类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择卡类型">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="form.bizType" placeholder="请选择业务类型">
            <el-option v-for="item in bizTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

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

<script setup>
import { ref, reactive, toRefs, getCurrentInstance, computed } from "vue";
import { getToken } from "@/utils/auth";
import { UploadFilled } from '@element-plus/icons-vue';
import {
  listCardInfo,
  getCardInfo,
  delCardInfo,
  addCardInfo,
  updateCardInfo,
  changeCardStatus
} from "@/api/experiment/cardInfo";

defineOptions({
  name: "CardInfo"
});

const { proxy } = getCurrentInstance();
const cardList = ref([]);
const open = ref(false);
const loading = ref(false);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const typeOptions = [
  { label: "真趣", value: "zq" },
  { label: "新锐科创", value: "xrkc" }
];
const bizTypeOptions = [
  { label: "板车", value: "0" },
  { label: "火车", value: "1" }
];
const enabledOptions = [
  { label: "启用", value: 0 },
  { label: "禁用", value: 1 }
];
const upload = reactive({
  open: false,
  title: "定位卡导入",
  isUploading: false,
  updateSupport: 0,
  headers: { Authorization: "Bearer " + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + "/experiment/cardInfo/importData"
});

const data = reactive({
  form: {
    id: undefined,
    cardId: null,
    yardId: null,
    yardName: null,
    type: null,
    bizType: null,
    enabled: 0
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    cardId: null,
    yardId: null,
    yardName: null,
    type: null,
    bizType: null,
    enabled: null
  },
  rules: {
    cardId: [{ required: true, message: "卡ID不能为空", trigger: "blur" }],
    yardId: [{ required: true, message: "货场ID不能为空", trigger: "blur" }],
    yardName: [{ required: true, message: "货场名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "请选择卡类型", trigger: "change" }],
    bizType: [{ required: true, message: "请选择业务类型", trigger: "change" }],
    enabled: [{ required: true, message: "请选择状态", trigger: "change" }]
  }
});

const { form, queryParams, rules } = toRefs(data);
const ids = ref([]);
const canEdit = computed(() => proxy.$auth.hasPermi('experiment:cardInfo:edit'));

function getList() {
  loading.value = true;
  const params = { ...queryParams.value };
  Object.keys(params).forEach(key => {
    if (params[key] === null || params[key] === "") {
      delete params[key];
    }
  });
  if (params.bizType !== undefined) {
    params.bizType = Number(params.bizType);
  }
  if (params.enabled !== undefined) {
    params.enabled = Number(params.enabled);
  }
  listCardInfo(params).then(response => {
    cardList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection) {
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
  ids.value = selection.map(item => item.id);
}

function reset() {
  form.value = {
    id: undefined,
    cardId: null,
    yardId: null,
    yardName: null,
    type: null,
    bizType: null,
    enabled: 0
  };
  proxy.resetForm("formRef");
}

function handleAdd() {
  // 再次新增时，优先回填上一次提交的数据
  if (lastForm.value) {
    form.value = { ...lastForm.value, id: undefined };
  } else {
    reset();
  }
  open.value = true;
  title.value = "新增定位卡";
}

function handleUpdate(row = {}) {
  reset();
  const id = row.id || ids.value[0];
  getCardInfo(id).then(response => {
    const data = response.data || {};
    form.value = {
      ...data,
      bizType: data.bizType != null ? String(data.bizType) : "",
      enabled: data.enabled != null ? data.enabled : 0
    };
    open.value = true;
    title.value = "修改定位卡";
  });
}

function handleDelete(row) {
  const deleteIds = row.id ? [row.id] : ids.value;
  proxy.$modal
    .confirm('是否确认删除定位卡编号为"' + deleteIds.join(",") + '"的数据项？')
    .then(function () {
      return delCardInfo(deleteIds.join(","));
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (!valid) {
      return false;
    }
    const payload = {
      ...form.value,
      bizType: form.value.bizType !== null && form.value.bizType !== "" ? Number(form.value.bizType) : null,
      enabled: form.value.enabled != null ? Number(form.value.enabled) : 0
    };
    if (form.value.id != null) {
      updateCardInfo(payload).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        // 关闭弹窗后清空表单，避免再次新增时残留
        reset();
        getList();
      });
    } else {
      addCardInfo(payload).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
      // 记录本次提交的表单用于下次新增时回填
      lastForm.value = { ...form.value };
        // 新增后回到第一页并刷新列表，确保看到最新数据
        queryParams.value.pageNum = 1;
        getList();
      });
    }
  });
}

function handleStatusChange(row) {
  const status = row.enabled;
  const original = status === 1 ? 0 : 1;
  const text = status === 1 ? "启用" : "禁用";
  proxy.$modal
    .confirm('确认要' + text + '卡ID为"' + row.cardId + '"的定位卡吗？')
    .then(() => {
      return changeCardStatus({ id: row.id, enabled: status });
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
    })
    .catch(() => {
      row.enabled = original;
    });
}

function cancel() {
  open.value = false;
  // 取消时也清空表单，避免下次打开残留上次数据
  reset();
}

// 上次新增成功的表单快照，用于回填
const lastForm = ref(null);

function handleExport() {
  proxy.download(
    "experiment/cardInfo/export",
    { ...queryParams.value },
    `cardInfo_${new Date().getTime()}.xlsx`
  );
}

function handleImport() {
  upload.open = true;
}

function downloadImportTemplate() {
  window.location.href = import.meta.env.VITE_APP_BASE_API + "/experiment/cardInfo/importTemplate";
}

function handleFileUploadProgress() {
  upload.isUploading = true;
}

function handleFileSuccess(response) {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].clearFiles();
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
}

function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

getList();
</script>
