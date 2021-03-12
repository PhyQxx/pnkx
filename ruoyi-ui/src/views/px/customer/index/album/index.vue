<template>
    <div class="album" v-loading="loading">
        <div class="album-one pointer" v-for="item in albumList" :key="item.id">
            <div class="transition-box box" @click="goToPhotoList(item)">
                <el-image
                    class="album-cover"
                    :src="item.photo"
                    fit="scale-down">
                    <div slot="error" class="image-slot">
                        暂无照片
                    </div>
                </el-image>
                <div class="album-name">
                    {{item.dictLabel}}
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { getAlbumList } from "@/api/px/customer/photo";
    export default {
        data() {
            return {
                //相册列表
                albumList: [],
                // 遮罩层
                loading: true,
            }
        },
        created() {
            this.getAlbumList();
        },
        methods: {
            /**
             * 获取相册列表
             */
            getAlbumList() {
                getAlbumList({}).then(res => {
                    this.albumList = res.data;
                    this.loading = false;
                })
            },
            /**
             * 跳转相册详情页面
             */
            goToPhotoList(item) {
                if (item.photo !== undefined) {
                    this.$router.push({
                        name: 'photo',
                        query: {
                            type: item.dictValue
                        }
                    })
                }
            }
        },
    }
</script>

<style lang="scss" scoped>
.album{
    display: flex;
    align-items: center;
    flex-flow: wrap;
    .album-one{
        width: 25%;
        height: 12rem;
        display: flex;
        align-items: center;
        flex-flow: column wrap;
        justify-content: center;
        margin: 0 0 2rem 0;
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
                display: flex;
                justify-content: center;
                align-items: center;
                color: #999;
            }
            .album-name{
                margin-top: 1rem;
                color: #6ED3FF;
            }
        }
    }
}
</style>
