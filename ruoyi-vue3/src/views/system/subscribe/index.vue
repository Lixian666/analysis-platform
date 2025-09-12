<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
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
          v-hasPermi="['system:subscribe:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:subscribe:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:subscribe:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:subscribe:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="subscribeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="序号" align="center" width="50" />
      <el-table-column label="订阅名称" align="center" prop="name" />
      <el-table-column label="订阅类型" align="center" prop="type" />
      <el-table-column label="订阅模式" align="center" prop="mode" />
      <el-table-column label="URL/topic" align="center" prop="endpoint" />
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="创建时间" align="center" prop="createTime" />
      <el-table-column label="更新时间" align="center" prop="updateTime" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:subscribe:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:subscribe:remove']">删除</el-button>
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

    <!-- 添加或修改订阅信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="subscribeRef" :model="form" :rules="rules" label-width="80px">
        <el-row gutter="15">
          <el-col :span="12">
            <el-form-item label="订阅名称" prop="name">
              <el-input v-model="form.name" type="text" placeholder="请输入订阅名称订阅名称" clearable
                        :style="{width: '100%'}"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订阅类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择订阅类型" clearable :style="{width: '100%'}">
                <el-option v-for="(item, index) in typeOptions" :key="index" :label="item.label"
                           :value="item.value" :disabled="item.disabled"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订阅模式" prop="mode">
              <el-select v-model="form.mode" placeholder="订阅模式订阅模式" clearable :style="{width: '100%'}">
                <el-option v-for="(item, index) in modeOptions" :key="index" :label="item.label"
                           :value="item.value" :disabled="item.disabled"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源" prop="source">
              <el-select v-model="form.source" placeholder="来源" clearable :style="{width: '100%'}">
                <el-option v-for="(item, index) in sourceOptions" :key="index" :label="item.label"
                           :value="item.value" :disabled="item.disabled"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-form-item label="URL/topic" prop="field105">
            <el-input v-model="form.field105" type="text" placeholder="请输入URL/topic" clearable
                      :style="{width: '100%'}"></el-input>
          </el-form-item>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Subscribe">
import { listSubscribe, getSubscribe, delSubscribe, addSubscribe, updateSubscribe } from "@/api/system/subscribe";

const { proxy } = getCurrentInstance();

const subscribeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const subscribeRef = ref()

const data = reactive({
  form: {
    name: undefined,
    type: undefined,
    mode: undefined,
    source: undefined,
    endpoint: undefined,
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    type: null,
    mode: null,
    endpoint: null,
    status: null,
    source: null,
    createTime: null,
    updateTime: null
  },
  rules: {
    name: [{
      required: true,
      message: '请输入订阅名称订阅名称',
      trigger: 'blur'
    }],
    type: [{
      required: true,
      message: '请选择订阅类型',
      trigger: 'change'
    }],
    mode: [{
      required: true,
      message: '订阅模式',
      trigger: 'change'
    }],
    source: [{
      required: true,
      message: '来源',
      trigger: 'change'
    }],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询订阅信息列表 */
function getList() {
  loading.value = true;
  listSubscribe(queryParams.value).then(response => {
    subscribeList.value = response.rows;
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
    name: null,
    type: null,
    mode: null,
    endpoint: null,
    status: null,
    source: null,
    createTime: null,
    updateTime: null
  };
  proxy.resetForm("subscribeRef");
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
  ids.value = selection.map(item => item.ID);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加订阅信息";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _ID = row.ID || ids.value
  getSubscribe(_ID).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改订阅信息";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["subscribeRef"].validate(valid => {
    if (valid) {
      if (form.value.ID != null) {
        updateSubscribe(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addSubscribe(form.value).then(response => {
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
  const _IDs = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除订阅信息编号为"' + _IDs + '"的数据项？').then(function() {
    return delSubscribe(_IDs);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/subscribe/export', {
    ...queryParams.value
  }, `subscribe_${new Date().getTime()}.xlsx`)
}

const typeOptions = ref([{
  "label": "tagScanUwbBeacon",
  "value": "tagScanUwbBeacon"
}])
const modeOptions = ref([{
  "label": "HTTP",
  "value": "HTTP"
}, {
  "label": "MQ",
  "value": "MQ"
}])
const sourceOptions = ref([{
  "label": "ZQ",
  "value": "ZQ"
},])
// 弹窗设置
const dialogVisible = defineModel()
// 弹窗确认回调
const emit = defineEmits(['confirm'])
/**
 * @name: 弹窗打开后执行
 * @description: 弹窗打开后执行方法
 * @return {*}
 */
function onOpen() {}
/**
 * @name: 弹窗关闭时执行
 * @description: 弹窗关闭方法，重置表单
 * @return {*}
 */
function onClose() {
  subscribeRef.value.resetFields()
}
/**
 * @name: 弹窗取消
 * @description: 弹窗取消方法
 * @return {*}
 */
function close() {
  dialogVisible.value = false
}
/**
 * @name: 弹窗表单提交
 * @description: 弹窗表单提交方法
 * @return {*}
 */
function handelConfirm() {
  subscribeRef.value.validate((valid) => {
    if (!valid) return
    // TODO 提交表单
    close()
    // 回调父级组件
    emit('confirm')
  })
}

getList();
</script>
