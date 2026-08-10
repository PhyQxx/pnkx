<template>
    <div class="cascader">
        <el-cascader
            v-model="selectedValues"
            :options="options"
            @change="handleChange"
            :before-filter="handleFilter"
            :props="props"
            separator=" - "
            filterable
            clearable
            placeholder="请选择省市区"
        />
    </div>
</template>

<script>
import {getRegionList} from "@/api/px/blog/region";

export default {
    name: "Region",
    props: {
        address: {
            type: Array,
            default: ""
        }
    },
    async mounted() {
        // 获取省列表
        await this.getRegionList(0);
        // 获取市列表
        if (this.address[0]) await this.getRegionList(1, this.address[0]);
        // 获取区列表
        if (this.address[1]) await this.getRegionList(2, this.address[1]);
        // 赋值
        this.selectedValues = [Number(this.address[0]), Number(this.address[1]), Number(this.address[2])];
    },
    data() {
        const _this = this;
        return {
            // 选中的省市区值
            selectedValues: [],
            // 数据源，省市区的JSON数据
            options: [],
            // 映射关系
            props: {
                // value对应属性
                value: "id",
                // label对应属性
                label: "name",
                // 开启懒加载
                lazy: true,
                lazyLoad: function (node, resolve) {
                    const {level} = node;
                    getRegionList({
                        lb: level,
                        ssdqdm: node.value
                    }).then(res => {
                        resolve(res.data.filter(item => {
                            if (level === 0) {
                                // 判断是否已存在省
                                return !_this.options?.find(province => province.id === item.id)
                            } else if (level === 1) {
                                // 判断是否已存在市
                                return !_this.options.find(province => province.id === item.ssdqdm)?.children.find(city => city.id === item.id)
                            } else if (level === 2) {
                                // 找到省
                                const theProvince = _this.options.find(province => province.children.find(city => city.id === item.ssdqdm));
                                // 判断是否已存在区
                                return !theProvince?.children.find(city => city.id === item.ssdqdm)?.children.find(area => area.id === item.id)
                            }
                        }).map(item => {
                            return {
                                ...item,
                                children: [],
                                leaf: level >= 2
                            }
                        }));
                    })
                }
            }
        };
    },
    methods: {
        /**
         * 搜索
         * @param searchValue
         */
        async handleFilter(searchValue) {
            if (searchValue) {
                await this.getRegionList(undefined, undefined, searchValue);
                this.selectedValues = Number(searchValue);
            }
        },
        /**
         * 选择省市区
         * @param value
         */
        handleChange(value) {
            this.$emit("update:address", value);
        },
        /**
         * 获取地区列表
         * @param level
         * @param parentCode
         * @param name
         */
        async getRegionList(level, parentCode, name) {
            await getRegionList({
                lb: level,
                ssdqdm: parentCode,
                name
            }).then(res => {
                res.data.forEach(item => {
                    if (item.lb === 0) {
                        // 省
                        this.options.find(province => province.id === item.id) || this.options.push(item);
                    } else if (item.lb === 1) {
                        // 市
                        this.options.find(province => province.id === item.ssdqdm)?.children?.find(city => city.id === item.id) || this.options.find(province => province.id === item.ssdqdm).children.push({
                            ...item,
                            children: item.children.map(area => {
                                return {
                                    ...area,
                                    leaf: true
                                }
                            })
                        });
                    } else if (item.lb === 2) {
                        // 区
                        // 找到省
                        const theProvince = this.options.find(province => province.children.find(city => city.id === item.ssdqdm));
                        // 找到市放入区
                        theProvince.children.find(city => city.id === item.ssdqdm)?.children?.find(area => area.id === item.id) || theProvince.children.find(city => city.id === item.ssdqdm)?.children.push({
                            ...item,
                            leaf: true
                        });
                    }
                })
            })
        }
    }
}
</script>
