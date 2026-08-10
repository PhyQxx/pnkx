<template>
    <div class="app-container">
        <el-row :gutter="20">
            <el-col :span="6" :xs="24">
                <el-card class="box-card">
                    <div slot="header" class="clearfix">
                        <span>个人信息</span>
                    </div>
                    <div>
                        <div class="text-center">
                            <userAvatar :user="user"/>
                        </div>
                        <ul class="list-group list-group-striped">
                            <li class="list-group-item">
                                <svg-icon icon-class="user"/>
                                用户名称
                                <div class="pull-right">{{ user.userName }}</div>
                            </li>
                            <li class="list-group-item">
                                <svg-icon icon-class="phone"/>
                                手机号码
                                <div class="pull-right">{{ user.phonenumber }}</div>
                            </li>
                            <li class="list-group-item">
                                <svg-icon icon-class="email"/>
                                用户邮箱
                                <div class="pull-right">{{ user.email }}</div>
                            </li>
                            <li class="list-group-item">
                                <svg-icon icon-class="tree"/>
                                所属部门
                                <div class="pull-right" v-if="user.dept">{{ user.dept.deptName }} / {{
                                        postGroup
                                    }}
                                </div>
                            </li>
                            <li class="list-group-item">
                                <svg-icon icon-class="peoples"/>
                                所属角色
                                <div class="pull-right">{{ roleGroup }}</div>
                            </li>
                            <li class="list-group-item">
                                <svg-icon icon-class="date"/>
                                创建日期
                                <div class="pull-right">{{ user.createTime }}</div>
                            </li>
                        </ul>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="18" :xs="24">
                <el-card>
                    <div slot="header" class="clearfix">
                        <span>基本资料</span>
                    </div>
                    <el-tabs v-model="activeTab">
                        <el-tab-pane label="基本资料" name="userinfo">
                            <userInfo :user="user"/>
                        </el-tab-pane>
                        <el-tab-pane label="修改密码" name="resetPwd">
                            <resetPwd :user="user"/>
                        </el-tab-pane>
                    </el-tabs>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script>
import userAvatar from "./userAvatar.vue";
import userInfo from "./userInfo.vue";
import resetPwd from "./resetPwd.vue";
import {getUserProfile} from "@/api/system/user";

export default {
    name: "Profile",
    components: {userAvatar, userInfo, resetPwd},
    data() {
        return {
            user: {},
            roleGroup: {},
            postGroup: {},
            activeTab: "userinfo"
        };
    },
    created() {
        this.getUser();
    },
    methods: {
        getUser() {
            getUserProfile().then(response => {
                this.user = response.data;
                this.roleGroup = response.roleGroup;
                this.postGroup = response.postGroup;
            });
        }
    }
};
</script>

<style lang="scss" scoped>
.app-container {
    padding: var(--space-6);
}

::v-deep .el-card {
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    background: var(--bg-card);
    transition: box-shadow var(--duration-normal) var(--ease-default);

    &:hover {
        box-shadow: var(--shadow-md);
    }

    .el-card__header {
        padding: var(--space-4) var(--space-6);
        border-bottom: 1px solid var(--border-primary);
        font-size: var(--text-lg);
        font-weight: var(--font-semibold);
        color: var(--text-primary);
    }

    .el-card__body {
        padding: var(--space-5) var(--space-6);
    }
}

.text-center {
    text-align: center;
}

.list-group {
    padding: 0;
    margin: var(--space-4) 0 0 0;
    list-style: none;
}

.list-group-item {
    display: flex;
    align-items: center;
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-sm);
    color: var(--text-secondary);
    border-radius: var(--radius-md);
    transition: background-color var(--duration-fast) var(--ease-default);

    &:hover {
        background: var(--bg-hover);
    }

    .svg-icon {
        margin-right: var(--space-3);
        color: var(--text-tertiary);
    }

    .pull-right {
        margin-left: auto;
        color: var(--text-primary);
        font-weight: var(--font-semibold);
    }
}

.list-group-striped .list-group-item {
    border-bottom: 1px solid var(--border-primary);

    &:last-child {
        border-bottom: none;
    }
}

::v-deep .el-tabs__item {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    transition: color var(--duration-fast) var(--ease-default);

    &.is-active {
        color: var(--color-primary);
        font-weight: var(--font-semibold);
    }
}
</style>
