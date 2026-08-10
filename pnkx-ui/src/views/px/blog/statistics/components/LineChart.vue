<template>
    <div :class="className" :style="{height:height,width:width}"/>
</template>

<script>
import echarts from '@/utils/echarts'

import 'echarts/theme/macarons'; // echarts theme
import resize from '@/views/dashboard/mixins/resize'

export default {
    mixins: [resize],
    props: {
        className: {
            type: String,
            default: 'chart'
        },
        width: {
            type: String,
            default: '100%'
        },
        height: {
            type: String,
            default: '350px'
        },
        autoResize: {
            type: Boolean,
            default: true
        },
        chartData: {
            type: [Array, Object],
            required: true,
            default: []
        },
    },
    data() {
        return {
            chart: null,
        }
    },
    watch: {
        chartData: {
            deep: true,
            handler(val) {
                try {
                    this.setOptions(val)
                } catch (e) {
                    console.error('加载折线图异常：' + e)
                }
            }
        }
    },
    mounted() {
        this.$nextTick(() => {
            this.initChart()
        })
    },
    beforeUnmount() {
        if (!this.chart) {
            return
        }
        this.chart.dispose()
        this.chart = null
    },
    methods: {
        initChart() {
            this.chart = echarts.init(this.$el, 'macarons')
            this.setOptions(this.chartData)
        },
        setOptions(data) {
            let legend = (data[0] && data[0].number) ? ['访问量'] : ['裴大头', '秦小丑'];
            if (!Array.isArray(data)) {
                data = [{date: '', number: ''}]
            }
            this.chart.setOption({
                xAxis: {
                    data: data.map(item => item.date),
                    boundaryGap: false,
                    axisTick: {
                        show: false
                    }
                },
                grid: {
                    left: 10,
                    right: 10,
                    bottom: 20,
                    top: 30,
                    containLabel: true
                },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'cross'
                    },
                    padding: [5, 10]
                },
                yAxis: {
                    axisTick: {
                        show: false
                    }
                },
                legend: {
                    data: legend
                },
                series: [{
                    name: legend[0], itemStyle: {
                        normal: {
                            color: '#3888fa',
                            lineStyle: {
                                color: '#3888fa',
                                width: 2
                            }
                        }
                    },
                    smooth: true,
                    type: 'line',
                    data: data.map(item => item.number || item.phy),
                    animationDuration: 2800,
                    animationEasing: 'cubicInOut'
                },
                    {
                        name: legend[1],
                        smooth: true,
                        type: 'line',
                        itemStyle: {
                            normal: {
                                color: '#FF005A',
                                lineStyle: {
                                    color: '#FF005A',
                                    width: 2
                                },
                                areaStyle: {
                                    color: '#f3f8ff'
                                }
                            }
                        },
                        data: data.map(item => item.qxx),
                        animationDuration: 2800,
                        animationEasing: 'quadraticOut'
                    }]
            })
        }
    }
}
</script>
