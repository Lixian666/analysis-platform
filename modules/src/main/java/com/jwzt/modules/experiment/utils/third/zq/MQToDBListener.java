package com.jwzt.modules.experiment.utils.third.zq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MQToDBListener {

    // MQ 配置（failover 协议支持自动重连）
    private static final String BROKER_URL =
            "failover:(tcp://api.joysuch.com:41616)?initialReconnectDelay=2000&maxReconnectAttempts=-1&useExponentialBackOff=true";
    private static final String QUEUE_NAME = "Consumer.JoySuchOpenTest.VirtualTopic.T-SUBS-209885";

    // 数据库配置
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    private static volatile boolean running = true;

    // JMS 对象
    private static javax.jms.Connection mqConnection;
    private static Session session;
    private static MessageConsumer consumer;

    // 心跳检测线程池
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    // 批量插入队列
    private static final List<String> messageBuffer = new CopyOnWriteArrayList<>();
    private static final int BATCH_SIZE = 100; // 批量条数
    private static final ScheduledExecutorService batchInsertScheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        startMQListener();
        startBatchInsertTask();
    }

    /**
     * 启动 MQ 监听
     */
    private static void startMQListener() {
        while (running) {
            try {
                System.out.println("🔄 正在连接到 MQ...");
                ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
                mqConnection = factory.createConnection();
                mqConnection.start();

                // 监听连接异常
                mqConnection.setExceptionListener(exception -> {
                    System.err.println("⚠ MQ 连接异常: " + exception.getMessage());
                    reconnect();
                });

                // 创建 Session
                session = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(QUEUE_NAME);
                consumer = session.createConsumer(destination);

                // 设置消息监听
                consumer.setMessageListener(message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String text = ((TextMessage) message).getText();
                            System.out.println("[MQ] 收到消息: " + text);
                            messageBuffer.add(text);
                            if (messageBuffer.size() >= BATCH_SIZE) {
                                flushMessages();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                System.out.println("✅ MQ 消息监听器已启动，等待消息...");
                startHeartbeat(); // 启动心跳检测
                break; // 连接成功，退出重连循环

            } catch (Exception e) {
                System.err.println("❌ 连接 MQ 失败: " + e.getMessage());
                sleep(3000); // 等待后重试
            }
        }
    }

    /**
     * 启动批量插入任务
     */
    private static void startBatchInsertTask() {
        batchInsertScheduler.scheduleAtFixedRate(() -> {
            try {
                flushMessages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS); // 每 5 秒执行一次
    }

    /**
     * 批量写入 MySQL
     */
    private static synchronized void flushMessages() {
        if (messageBuffer.isEmpty()) {
            return;
        }
        List<String> batch = new ArrayList<>(messageBuffer);
        messageBuffer.clear();

        StringBuilder sql = new StringBuilder("INSERT INTO mq_messages(content) VALUES ");
        for (int i = 0; i < batch.size(); i++) {
            sql.append("(?)");
            if (i < batch.size() - 1) {
                sql.append(",");
            }
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < batch.size(); i++) {
                ps.setString(i + 1, batch.get(i));
            }

            int inserted = ps.executeUpdate();
            System.out.println("[DB] 批量插入成功: " + inserted + " 条");

        } catch (Exception e) {
            e.printStackTrace();
            // 如果批量失败，把数据放回去，防止丢失
            messageBuffer.addAll(batch);
        }
    }

    /**
     * 启动心跳检测
     */
    private static void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (session != null && mqConnection != null) {
                    MessageProducer producer = session.createProducer(session.createQueue("heartbeat"));
                    producer.send(session.createTextMessage("ping"));
                    producer.close();
                    System.out.println("💓 MQ 心跳正常");
                }
            } catch (Exception e) {
                System.err.println("💔 MQ 心跳失败，准备重连...");
                reconnect();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 重连 MQ
     */
    private static synchronized void reconnect() {
        closeMQ();
        startMQListener();
    }

    /**
     * 关闭 MQ 连接
     */
    private static void closeMQ() {
        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (mqConnection != null) mqConnection.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 线程休眠
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
