<template>
    <div class="app-container">
        <el-row>
            <el-col :span="24" class="card-box">
                <el-card>
                    <div slot="header"><span>基本信息</span></div>
                    <div class="el-table el-table--enable-row-hover el-table--medium">
                        <table cellspacing="0" style="width: 100%">
                            <tbody>
                            <tr>
                                <td>
                                    <div class="cell">Redis版本</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.redis_version }}</div>
                                </td>
                                <td>
                                    <div class="cell">运行模式</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">
                                        {{ cache.info.redis_mode == "standalone" ? "单机" : "集群" }}
                                    </div>
                                </td>
                                <td>
                                    <div class="cell">端口</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.tcp_port }}</div>
                                </td>
                                <td>
                                    <div class="cell">客户端数</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.connected_clients }}</div>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <div class="cell">运行时间(天)</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.uptime_in_days }}</div>
                                </td>
                                <td>
                                    <div class="cell">使用内存</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.used_memory_human }}</div>
                                </td>
                                <td>
                                    <div class="cell">使用CPU</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">
                                        {{ parseFloat(cache.info.used_cpu_user_children).toFixed(2) }}
                                    </div>
                                </td>
                                <td>
                                    <div class="cell">内存配置</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.maxmemory_human }}</div>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <div class="cell">AOF是否开启</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{
                                            cache.info.aof_enabled == "0" ? "否" : "是"
                                        }}
                                    </div>
                                </td>
                                <td>
                                    <div class="cell">RDB是否成功</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{ cache.info.rdb_last_bgsave_status }}</div>
                                </td>
                                <td>
                                    <div class="cell">Key数量</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.dbSize">{{ cache.dbSize }}</div>
                                </td>
                                <td>
                                    <div class="cell">网络入口/出口</div>
                                </td>
                                <td>
                                    <div class="cell" v-if="cache.info">{{
                                            cache.info.instantaneous_input_kbps
                                        }}kps/{{ cache.info.instantaneous_output_kbps }}kps
                                    </div>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </el-card>
            </el-col>

            <el-col :span="12" class="card-box">
                <el-card>
                    <div slot="header"><span>命令统计</span></div>
                    <div class="el-table el-table--enable-row-hover el-table--medium">
                        <div ref="commandstats" style="height: 420px"/>
                    </div>
                </el-card>
            </el-col>

            <el-col :span="12" class="card-box">
                <el-card>
                    <div slot="header">
                        <span>内存信息</span>
                    </div>
                    <div class="el-table el-table--enable-row-hover el-table--medium">
                        <div ref="usedmemory" style="height: 420px"/>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script>
import {getCache} from "@/api/monitor/cache";
import echarts from '@/utils/echarts';

export default {
    name: "Server",
    data() {
        return {
            // 加载层信息
            loading: [],
            // 统计命令信息
            commandstats: null,
            // 使用内存
            usedmemory: null,
            // cache信息
            cache: [],
        };
    },
    created() {
        this.getList();
        this.openLoading();
    },
    methods: {
        /** 查缓存询信息 */
        getList() {
            getCache().then((response) => {
                this.cache = response.data;
                this.loading.close();

                this.commandstats = echarts.init(this.$refs.commandstats, "macarons");
                this.commandstats.setOption({
                    tooltip: {
                        trigger: "item",
                        formatter: "{a} <br/>{b} : {c} ({d}%)",
                    },
                    series: [
                        {
                            name: "命令",
                            type: "pie",
                            roseType: "radius",
                            radius: [15, 95],
                            center: ["50%", "38%"],
                            data: response.data.commandStats,
                            animationEasing: "cubicInOut",
                            animationDuration: 1000,
                        },
                    ],
                });
                this.usedmemory = echarts.init(this.$refs.usedmemory, "macarons");
                this.usedmemory.setOption({
                    tooltip: {
                        formatter: "{b} <br/>{a} : " + this.cache.info.used_memory_human,
                    },
                    series: [
                        {
                            name: "峰值",
                            type: "gauge",
                            min: 0,
                            max: 1000,
                            detail: {
                                formatter: this.cache.info.used_memory_human,
                            },
                            data: [
                                {
                                    value: parseFloat(this.cache.info.used_memory_human),
                                    name: "内存消耗",
                                },
                            ],
                        },
                    ],
                });
            });
        },
        // 打开加载层
        openLoading() {
            this.loading = this.$loading({
                lock: true,
                text: "拼命读取中",
                spinner: "el-icon-loading",
                background: "rgba(0, 0, 0, 0.7)",
            });
        },
    },
};
</script>

<style scoped>
.app-container {
    padding: var(--space-6);
}

.card-box {
    padding: var(--space-3);
    margin-bottom: var(--space-4);
    transition: all var(--duration-normal) var(--ease-default);
}

.card-box >>> .el-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-primary);
    box-shadow: var(--shadow-sm);
    background: var(--bg-card);
    transition: box-shadow var(--duration-normal) var(--ease-default);
}

.card-box >>> .el-card:hover {
    box-shadow: var(--shadow-md);
}

.card-box >>> .el-card__header {
    padding: var(--space-4) var(--space-6);
    border-bottom: 1px solid var(--border-primary);
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
}

.card-box >>> .el-card__body {
    padding: var(--space-4) var(--space-6);
}

/* Info table styling */
.card-box table {
    border-collapse: separate;
    border-spacing: 0;
    width: 100%;
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.card-box table td,
.card-box table th {
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--border-primary);
    font-size: var(--text-sm);
    color: var(--text-secondary);
    transition: background-color var(--duration-fast) var(--ease-default);
}

.card-box table th {
    background: var(--bg-body);
    color: var(--text-primary);
    font-weight: var(--font-semibold);
    font-size: var(--text-sm);
}

.card-box table tbody tr:last-child td {
    border-bottom: none;
}

.card-box table tbody tr:hover td {
    background: var(--bg-hover);
}

.card-box table td .cell {
    font-size: var(--text-sm);
    line-height: 1.6;
}

/* Chart containers */
.card-box >>> .el-table {
    border-radius: var(--radius-md);
    overflow: hidden;
}

.card-box >>> .el-table th {
    background: var(--bg-body);
    color: var(--text-primary);
    font-weight: var(--font-semibold);
    font-size: var(--text-sm);
}

.card-box >>> .el-table td {
    color: var(--text-secondary);
    font-size: var(--text-sm);
}

.card-box >>> .el-table--enable-row-hover tbody tr:hover > td {
    background: var(--bg-hover);
    transition: background-color var(--duration-fast) var(--ease-default);
}
</style>
