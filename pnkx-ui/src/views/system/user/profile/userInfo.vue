<template>
    <el-form ref="form" :model="user" :rules="rules" label-width="80px">
        <el-form-item label="用户昵称" prop="nickName">
            <el-input v-model="user.nickName"/>
        </el-form-item>
        <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="user.phonenumber" maxlength="11"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
            <el-input v-model="user.email" maxlength="50"/>
        </el-form-item>
        <el-form-item label="性别">
            <el-radio-group v-model="user.sex">
                <el-radio label="0">男</el-radio>
                <el-radio label="1">女</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item label="个性签名" prop="remark">
            <el-input v-model="user.remark" maxlength="100"/>
        </el-form-item>
        <el-form-item>
            <el-button type="primary" size="small" @click="submit">保存</el-button>
            <el-button type="danger" size="small" @click="close">关闭</el-button>
        </el-form-item>
    </el-form>
</template>

<script>
import {updateUserProfile} from "@/api/system/user";

export default {
    props: {
        user: {
            type: Object
        }
    },
    data() {
        return {
            // 表单校验
            rules: {
                nickName: [
                    {required: true, message: "用户昵称不能为空", trigger: "blur"}
                ],
                email: [
                    {required: true, message: "邮箱地址不能为空", trigger: "blur"},
                    {
                        type: "email",
                        message: "'请输入正确的邮箱地址",
                        trigger: ["blur", "change"]
                    }
                ],
                phonenumber: [
                    {required: true, message: "手机号码不能为空", trigger: "blur"},
                    {
                        pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
                        message: "请输入正确的手机号码",
                        trigger: "blur"
                    }
                ]
            }
        };
    },
    methods: {
        submit() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    updateUserProfile(this.user).then(response => {
                        this.msgSuccess("修改成功");
                    });
                }
            });
        },
        close() {
            this.$store.dispatch("tagsView/delView", this.$route);
            this.$router.push({path: "/index"});
        }
    }
};
</script>

<style lang="scss" scoped>
::v-deep .el-form {
    padding: var(--space-4) var(--space-2);

    .el-form-item__label {
        color: var(--text-secondary);
        font-size: var(--text-sm);
    }

    .el-input__inner {
        border-radius: var(--radius-md);
        transition: border-color var(--duration-fast) var(--ease-default),
                    box-shadow var(--duration-fast) var(--ease-default);

        &:focus {
            border-color: var(--color-primary);
            box-shadow: 0 0 0 2px rgba(var(--color-primary), 0.15);
        }
    }

    .el-radio__input.is-checked .el-radio__inner {
        border-color: var(--color-primary);
        background: var(--color-primary);
    }
}

::v-deep .el-button--primary {
    border-radius: var(--radius-md);
    transition: all var(--duration-fast) var(--ease-default);
}

::v-deep .el-button--danger {
    border-radius: var(--radius-md);
    transition: all var(--duration-fast) var(--ease-default);
}
</style>
