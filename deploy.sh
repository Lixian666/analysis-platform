#!/bin/bash
set -e

echo "========== 开始一键部署 analysis-platform =========="

# ===== 路径定义 =====
ZIP_FILE="/data/app/deploy.zip"
DEPLOY_DIR="/data/app/deploy"
BASE_DIR="/data/app/analysis-platform"

# ===== 校验 deploy.zip =====
if [ ! -f "$ZIP_FILE" ]; then
    echo "!! 未找到 $ZIP_FILE"
    exit 1
fi

# ===== 解压 deploy.zip =====
echo ">> 解压 deploy.zip..."
mkdir -p ${DEPLOY_DIR}
unzip -oq ${ZIP_FILE} -d /data/app

# ===== 创建目录结构 =====
echo ">> 创建业务目录..."
mkdir -p \
${BASE_DIR}/config \
${BASE_DIR}/data/img \
${BASE_DIR}/data/shp \
${BASE_DIR}/data/tiles \
${BASE_DIR}/logs

# ===== 拷贝 jar & restart.sh &  & =====
echo ">> 拷贝核心文件..."
cp -f ${DEPLOY_DIR}/analysis-platform.jar ${BASE_DIR}/
cp -f ${DEPLOY_DIR}/restart.sh ${BASE_DIR}/
cp -f ${DEPLOY_DIR}/application.yml ${BASE_DIR}/config
cp -f ${DEPLOY_DIR}/application-lr.yml ${BASE_DIR}/config

chmod +x ${BASE_DIR}/restart.sh

# ===== 解压 dist.zip =====
echo ">> 处理 dist.zip..."
if [ -f "${DEPLOY_DIR}/dist.zip" ]; then
    unzip -oq ${DEPLOY_DIR}/dist.zip -d ${BASE_DIR}
else
    echo "!! dist.zip 不存在"
fi

# ===== 解压 luorong.zip =====
echo ">> 处理 luorong.zip..."
if [ -f "${DEPLOY_DIR}/luorong.zip" ]; then
    unzip -oq ${DEPLOY_DIR}/luorong.zip -d ${BASE_DIR}/data/shp
else
    echo "!! luorong.zip 不存在"
fi

# ===== 解压 tiles.zip =====
echo ">> 处理 tiles.zip..."
if [ -f "${DEPLOY_DIR}/tiles.zip" ]; then
    unzip -oq ${DEPLOY_DIR}/tiles.zip -d ${BASE_DIR}/data
else
    echo "!! tiles.zip 不存在"
fi

# ===== 设置 tiles 权限 =====
echo ">> 设置 tiles 目录权限..."
sudo chmod -R 777 ${BASE_DIR}/data/tiles

echo "========== 部署完成 =========="
echo "启动命令："
echo "cd ${BASE_DIR} && sudo ./restart.sh restart && sudo ./restart.sh logs"
