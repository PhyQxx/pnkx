<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px">
            <el-form-item label="图片名称" prop="name">
                <el-input
                    v-model="queryParams.name"
                    placeholder="请输入图片名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
                <el-input
                    v-model="queryParams.remark"
                    placeholder="请输入备注"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <div class="photo-list" v-loading="loading">
            <div class="photo-one" v-for="photo in photoList" :key="photo.id">
                <div class="box">
                    <el-image
                        class="album-cover"
                        :src="photo.photoBase64"
                        :preview-src-list="srcList"
                        fit="scale-down">
                    </el-image>
                    <div class="photo-name">
                        {{photo.name}}
                    </div>
                </div>
            </div>
        </div>

        <pagination
            v-show="total>0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
        />

        <message-board messageType="3" :articleId="$route.query.type"/>
    </div>
</template>

<script>
    import { listPhoto } from "@/api/px/customer/photo";
    import messageBoard from '@/components/MessageBoard/index'

    export default {
        name: "Photo",
        components: {
            messageBoard
        },
        data() {
            return {
                // 遮罩层
                loading: true,
                // 总条数
                total: 0,
                // 相册表格数据
                photoList: [],
                //照片地址数组
                srcList: [],
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                    name: null,
                    photoAddress: null,
                    type: this.$route.query.type,
                    remark: null
                },
            };
        },
        created() {
            this.getList();
        },
        methods: {
            /**
             * 查询相册列表
             */
            getList() {
                this.loading = true;
                listPhoto(this.queryParams).then(response => {
                    this.photoList = response.rows;
                    this.srcList = [];
                    this.photoList.forEach(item => {
                       this.srcList.push(item.photoBase64)
                    });
                    this.total = response.total;
                    this.loading = false;
                });
            },
            // 表单重置
            reset() {
                this.form = {
                    id: null,
                    name: null,
                    photoBase64: null,
                    photoAddress: null,
                    type: null,
                    version: null,
                    createBy: null,
                    createTime: null,
                    updateBy: null,
                    updateTime: null,
                    remark: null
                };
                this.resetForm("form");
            },
            /** 搜索按钮操作 */
            handleQuery() {
                this.queryParams.pageNum = 1;
                this.getList();
            },
            /** 重置按钮操作 */
            resetQuery() {
                this.resetForm("queryForm");
                this.handleQuery();
            },
        }
    };
</script>

<style lang="scss" scoped>
    .message{
        .message-label{
            font-size: 0.9rem;
            font-weight: bold;
        }
        .leave-message{
            display: flex;
            align-items: center;
            padding: 1rem;
            align-items: flex-start;
            .message-left{
                margin-right: 1rem;
                width: 4rem;
                display: flex;
                flex-flow: column;
                align-items: center;
                .header-photo{
                    width: 4rem;
                    height: 4rem;
                    border-radius: 50%;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    img{
                        width: 100%;
                        height: 100%;
                    }
                }
                .author-name{
                    color: #999;
                    margin-top: 0.5rem;
                    text-align: center;
                    text-shadow: 1px 1px 2px #0CF;
                }
            }
            .message-right{
                width: calc(100% - 5rem);
                background: #C6F1FD;
                padding: 1rem;
                border-radius: 10px;
                .message-right-top{
                    display: flex;
                    justify-content: space-between;
                    .leave-message-content{
                        width: calc(100% - 5rem);
                        font-size: 0.9rem;
                        color: rgb(34, 32, 32);
                    }
                    .floor{
                        font-size: 1.2rem;
                        width: 5rem;
                        display: flex;
                        justify-content: flex-end;
                    }
                }
                .leave-message-time{
                    display: flex;
                    justify-content: flex-end;
                    margin-top: 1rem;
                    font-size: 0.9rem;
                    color: rgb(78, 75, 75);
                }
            }
            .message-right:before{
                position: absolute;
                content: "";
                width: 0;
                height: 0;
                border-top: 0.8rem solid transparent;
                border-right: 1rem solid #C6F1FD;
                border-bottom: 0.8rem solid transparent;
                margin: -0.1rem 0 0 -2rem;
            }
        }
    }
    .message-board{
        margin: 1rem;
        padding: 1rem;
        border: 3px dashed #ACEBFF;
        background-color: #fdfdfd;
        border-radius: 10px;
        box-shadow: 0 0 0.8rem #ccc;
        height: 300px;
        box-shadow: inset 0 0 1rem #0CF;
        display: flex;
        align-items: center;
        .message-board-left{
            width: 60%;
            .message-board-left-top{
                display: flex;
                .message-logo{
                    width: 5rem;
                    height: 3rem;
                    margin: 1rem;
                    background-size: 100%;
                    background-image: url('../../../../../../assets/images/mc.jpg');
                    background-position: center;
                }
                .message-text{
                    display: flex;
                    align-items: center;
                    .text-left{
                        font-size: 1.2rem;
                        color: #ccc;
                    }
                    .text-right{
                        font-weight: bold;
                    }
                }
            }
            .message-board-left-bottom{
                .textarea{
                    width: 100%;
                    height: 10rem;
                    padding: 1rem;
                    border: 1px solid #ccc;
                    border-radius: 5px;
                    overflow: scroll;
                }
                .tips{
                    font-size: 0.9rem;
                    color: #999;
                    padding: 0.5rem 0;
                    .text-number{
                        color: #FF399A;
                        font-style: italic;
                    }
                }
            }
        }
        .message-board-right{
            width: 40%;
            display: flex;
            padding-left: 1rem;
            height: 100%;
            flex-flow: column;
            align-items: center;
            justify-content: space-around;
            .customer-name, .customer-mail{
                display: flex;
                align-items: center;
                .label{
                    white-space: nowrap;
                }
            }
            .button{
                width: 6rem;
                .el-button{
                    width: 100%;
                }
            }
            .your-header{
                width: 6rem;
                height: 6rem;
                border: 1px solid #999;
                border-radius: 5px;
                display: flex;
                flex-flow: column;
                align-items: center;
                justify-content: center;
                .close-icon{
                    position: absolute;
                    margin: -5rem -5rem 0 0;
                    color: red;
                }
                #headerPhoto{
                    width: 4.5rem;
                    position: absolute;
                }
                .header-picture{
                    width: 100%;
                    height: 100%;
                    padding: 0.5rem;
                    display: flex;
                    justify-content: center;
                    white-space: nowrap;
                    img{
                        width: 100%;
                        height: 100%;
                    }
                }
            }
        }
    }
    .photo-list{
        display: flex;
        flex-flow: wrap;
        .photo-one{
            width: 25%;
            height: 13rem;
            display: flex;
            align-items: center;
            flex-flow: column wrap;
            justify-content: center;
            .box{
                display: flex;
                align-items: center;
                flex-flow: column wrap;
                justify-content: center;
                border: 1px solid #6ED3FF;
                padding: 1rem;
                border-radius: 5px;
                box-shadow:4px 4px 5px 3px #6ED3FF;
                .album-cover{
                    width: 8rem;
                    height: 8rem;
                }
                .photo-name{
                    margin-top: 0.5rem;
                    color: #6ED3FF;
                    height: 1rem;
                }
            }
        }
    }
</style>
