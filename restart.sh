#!/bin/bash
# 数据分析平台 - JAR包管理脚本

# ===== 配置参数 =====
APP_NAME="analysis-platform"
JAR_NAME="analysis-platform.jar"
APP_PORT=8020

# 部署目录配置
DEPLOY_DIR="/data/app/analysis-platform"
CONFIG_DIR="${DEPLOY_DIR}/config"
LOGS_DIR="${DEPLOY_DIR}/logs"
JAR_PATH="${DEPLOY_DIR}/${JAR_NAME}"
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"

# ===== 日志文件（按天） =====
TODAY=$(date +"%Y%m%d")
CONSOLE_LOG="${LOGS_DIR}/console-${TODAY}.log"

# JVM参数配置
JVM_OPTS="-Xms512m -Xmx8192m"
JVM_OPTS="${JVM_OPTS} -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m"
JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=${LOGS_DIR}/heapdump.hprof"

# Spring Boot参数
SPRING_OPTS="--spring.config.additional-location=${CONFIG_DIR}/"
SPRING_OPTS="${SPRING_OPTS} --logging.file.path=${LOGS_DIR}"
SPRING_OPTS="${SPRING_OPTS} --server.port=${APP_PORT}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ===== 函数定义 =====

# 获取应用PID
get_pid() {
    # 优先从PID文件读取
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if ps -p $pid > /dev/null 2>&1; then
            echo $pid
            return
        else
            rm -f "$PID_FILE"
        fi
    fi

    # 从进程列表查找
    ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}'
}

# 检查应用是否运行
is_running() {
    local pid=$(get_pid)
    if [ -z "$pid" ]; then
        return 1
    else
        return 0
    fi
}

# 启动应用
start() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}正在启动 ${APP_NAME}...${NC}"
    echo -e "${BLUE}========================================${NC}"

    if is_running; then
        local pid=$(get_pid)
        echo -e "${YELLOW}应用已在运行中 (PID: $pid)${NC}"
        return 0
    fi

    # 检查JAR文件是否存在
    if [ ! -f "$JAR_PATH" ]; then
        echo -e "${RED}错误: JAR文件不存在${NC}"
        echo -e "${RED}路径: ${JAR_PATH}${NC}"
        exit 1
    fi

    # 检查配置目录
    if [ ! -d "$CONFIG_DIR" ]; then
        echo -e "${YELLOW}警告: 配置目录不存在，创建目录: ${CONFIG_DIR}${NC}"
        mkdir -p "$CONFIG_DIR"
    fi

    # 检查日志目录
    if [ ! -d "$LOGS_DIR" ]; then
        echo -e "${YELLOW}创建日志目录: ${LOGS_DIR}${NC}"
        mkdir -p "$LOGS_DIR"
    fi

    # 显示启动参数
    echo -e "${BLUE}启动参数:${NC}"
    echo -e "  JAR文件: ${JAR_PATH}"
    echo -e "  配置目录: ${CONFIG_DIR}"
    echo -e "  日志目录: ${LOGS_DIR}"
    echo -e "  监听端口: ${APP_PORT}"
    echo -e "  日志文件: ${CONSOLE_LOG}"
    echo ""

    # 启动应用
    cd "$DEPLOY_DIR"
    nohup java $JVM_OPTS -jar $JAR_PATH $SPRING_OPTS > "${CONSOLE_LOG}" 2>&1 &

    local pid=$!
    echo $pid > "$PID_FILE"

    # 等待应用启动
    echo -n "等待应用启动"
    for i in {1..30}; do
        if is_running; then
            echo ""
            echo -e "${GREEN}========================================${NC}"
            echo -e "${GREEN}应用启动成功！${NC}"
            echo -e "${GREEN}========================================${NC}"
            echo -e "PID: ${pid}"
            echo -e "端口: ${APP_PORT}"
            echo -e "访问地址: http://localhost:${APP_PORT}"
            echo -e "实时日志: tail -f ${CONSOLE_LOG}"
            echo -e "${GREEN}========================================${NC}"
            return 0
        fi
        echo -n "."
        sleep 1
    done

    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}应用启动失败！${NC}"
    echo -e "${RED}========================================${NC}"
    echo -e "请查看日志: ${CONSOLE_LOG}"
    echo -e "查看命令: tail -50 ${CONSOLE_LOG}"
    rm -f "$PID_FILE"
    exit 1
}

# 停止应用
stop() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}正在停止 ${APP_NAME}...${NC}"
    echo -e "${BLUE}========================================${NC}"

    if ! is_running; then
        echo -e "${YELLOW}应用未运行${NC}"
        rm -f "$PID_FILE"
        return 0
    fi

    local pid=$(get_pid)
    echo "停止进程 PID: $pid"

    # 优雅停止
    kill $pid

    # 等待进程结束
    echo -n "等待进程结束"
    for i in {1..30}; do
        if ! is_running; then
            echo ""
            echo -e "${GREEN}========================================${NC}"
            echo -e "${GREEN}应用已停止${NC}"
            echo -e "${GREEN}========================================${NC}"
            rm -f "$PID_FILE"
            return 0
        fi
        echo -n "."
        sleep 1
    done

    # 强制停止
    echo ""
    echo -e "${YELLOW}进程未能正常停止，尝试强制终止...${NC}"
    kill -9 $pid 2>/dev/null

    sleep 2

    if ! is_running; then
        echo -e "${GREEN}应用已强制停止${NC}"
        rm -f "$PID_FILE"
    else
        echo -e "${RED}无法停止应用，请手动检查${NC}"
        exit 1
    fi
}

# 重启应用
restart() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}正在重启 ${APP_NAME}...${NC}"
    echo -e "${BLUE}========================================${NC}"

    stop
    sleep 3
    start
}

# 查看状态
status() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}${APP_NAME} 运行状态${NC}"
    echo -e "${BLUE}========================================${NC}"

    if is_running; then
        local pid=$(get_pid)
        echo -e "${GREEN}状态: 运行中 ✓${NC}"
        echo -e "PID: ${pid}"
        echo -e "端口: ${APP_PORT}"
        echo -e "JAR: ${JAR_PATH}"
        echo -e "配置: ${CONFIG_DIR}"
        echo -e "日志: ${LOGS_DIR}"
        echo -e "当日日志: ${CONSOLE_LOG}"
        echo ""
        echo -e "${BLUE}进程信息:${NC}"
        ps -f -p $pid
        echo ""
        echo -e "${BLUE}端口监听:${NC}"
        netstat -tlnp 2>/dev/null | grep ":${APP_PORT}" || lsof -i:${APP_PORT} 2>/dev/null
    else
        echo -e "${RED}状态: 未运行 ✗${NC}"
    fi

    echo -e "${BLUE}========================================${NC}"
}

# 查看日志
logs() {
    if [ -f "${CONSOLE_LOG}" ]; then
        echo -e "${BLUE}========================================${NC}"
        echo -e "${BLUE}实时日志输出 (Ctrl+C 退出)${NC}"
        echo -e "${BLUE}========================================${NC}"
        tail -f "${CONSOLE_LOG}"
    else
        echo -e "${RED}日志文件不存在: ${CONSOLE_LOG}${NC}"
    fi
}

# 查看最近的日志
tail_logs() {
    local lines=${1:-100}
    if [ -f "${CONSOLE_LOG}" ]; then
        echo -e "${BLUE}========================================${NC}"
        echo -e "${BLUE}最近 ${lines} 行日志${NC}"
        echo -e "${BLUE}========================================${NC}"
        tail -n $lines "${CONSOLE_LOG}"
    else
        echo -e "${RED}日志文件不存在: ${CONSOLE_LOG}${NC}"
    fi
}

# 显示帮助信息
usage() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}数据分析平台 - JAR包管理脚本${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${YELLOW}用法:${NC}"
    echo -e "  $0 {start|stop|restart|status|logs|tail}"
    echo ""
    echo -e "${YELLOW}命令说明:${NC}"
    echo -e "  ${GREEN}start${NC}    - 启动应用"
    echo -e "  ${GREEN}stop${NC}     - 停止应用"
    echo -e "  ${GREEN}restart${NC}  - 重启应用"
    echo -e "  ${GREEN}status${NC}   - 查看运行状态"
    echo -e "  ${GREEN}logs${NC}     - 查看实时日志"
    echo -e "  ${GREEN}tail${NC}     - 查看最近日志 (默认100行，可指定: $0 tail 200)"
    echo ""
    echo -e "${YELLOW}配置信息:${NC}"
    echo -e "  部署目录: ${DEPLOY_DIR}"
    echo -e "  JAR文件:  ${JAR_PATH}"
    echo -e "  配置目录: ${CONFIG_DIR}"
    echo -e "  日志目录: ${LOGS_DIR}"
    echo -e "  监听端口: ${APP_PORT}"
    echo ""
    echo -e "${YELLOW}使用示例:${NC}"
    echo -e "  启动应用:    sudo $0 start"
    echo -e "  停止应用:    sudo $0 stop"
    echo -e "  重启应用:    sudo $0 restart"
    echo -e "  查看状态:    sudo $0 status"
    echo -e "  查看日志:    sudo $0 logs"
    echo -e "  查看最近日志: sudo $0 tail 50"
    echo -e "${BLUE}========================================${NC}"
}

# ===== 主程序 =====

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    tail)
        tail_logs $2
        ;;
    *)
        usage
        exit 1
        ;;
esac

exit 0
