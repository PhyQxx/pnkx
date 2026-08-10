<template>
    <div class="login-workbench">
        <!-- 左侧品牌展示区 -->
        <div class="login-brand visual-card">
            <div class="brand-content">
                <img src="@/assets/logo/logo.png" class="brand-logo" alt="logo">
                <h1 class="brand-title">Pei你看雪</h1>
                <p class="brand-subtitle">博客后台管理系统</p>
                <p class="brand-description">记录生活，分享技术，沉淀思考</p>

            </div>
        </div>

        <!-- 右侧登录表单区 -->
        <div class="login-form-wrapper login-panel">
            <div class="login-card">
                <div class="form-header">
                    <h2 class="form-title">欢迎回来</h2>
                    <p class="form-subtitle">请登录您的账户</p>
                </div>

                <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
                    <el-form-item prop="userName">
                        <el-input
                            v-model="loginForm.userName"
                            type="text"
                            auto-complete="off"
                            placeholder="请输入用户名"
                            size="large"
                        >
                            <template #prefix>
                                <el-icon><User /></el-icon>
                            </template>
                        </el-input>
                    </el-form-item>

                    <el-form-item prop="password">
                        <el-input
                            v-model="loginForm.password"
                            :type="passwordVisible ? 'text' : 'password'"
                            auto-complete="off"
                            placeholder="请输入密码"
                            size="large"
                            @keyup.enter="handleLogin"
                        >
                            <template #prefix>
                                <el-icon><Lock /></el-icon>
                            </template>
                            <template #suffix>
                                <el-icon class="password-toggle" @click="passwordVisible = !passwordVisible">
                                    <View v-if="passwordVisible" />
                                    <Hide v-else />
                                </el-icon>
                            </template>
                        </el-input>
                    </el-form-item>

                    <div class="form-options">
                        <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
                    </div>

                    <el-form-item>
                        <el-button
                            :loading="loading"
                            size="large"
                            type="primary"
                            class="login-btn"
                            @click.prevent="handleLogin"
                        >
                            <span v-if="!loading">登 录</span>
                            <span v-else>登录中...</span>
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="form-footer">
                    <p>Copyright © 2018-2024 Pei你看雪 All Rights Reserved.</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import {getCodeImg} from "@/api/login";
import Cookies from "js-cookie";
import {encrypt, decrypt} from '@/utils/jsencrypt'

export default {
    name: "Login",
    data() {
        return {
            codeUrl: "",
            cookiePassword: "",
            passwordVisible: false,
            loginForm: {
                userName: "",
                password: "",
                rememberMe: false,
                code: "",
                uuid: ""
            },
            loginRules: {
                userName: [
                    {required: true, trigger: "blur", message: "用户名不能为空"}
                ],
                password: [
                    {required: true, trigger: "blur", message: "密码不能为空"}
                ],
                code: [{required: true, trigger: "change", message: "验证码不能为空"}]
            },
            loading: false,
            redirect: undefined
        };
    },
    watch: {
        $route: {
            handler: function (route) {
                this.redirect = route.query && route.query.redirect;
            },
            immediate: true
        }
    },
    created() {
        this.getCookie();
    },
    methods: {
        getCode() {
            getCodeImg().then(res => {
                this.codeUrl = "data:image/gif;base64," + res.img;
                this.loginForm.uuid = res.uuid;
            });
        },
        getCookie() {
            const userName = Cookies.get("userName");
            const password = Cookies.get("password");
            const rememberMe = Cookies.get('rememberMe')
            this.loginForm = {
                userName: userName === undefined ? this.loginForm.userName : userName,
                password: password === undefined ? this.loginForm.password : decrypt(password),
                rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
            };
        },
        handleLogin() {
            this.$refs.loginForm.validate(valid => {
                if (valid) {
                    this.loading = true;
                    if (this.loginForm.rememberMe) {
                        Cookies.set("userName", this.loginForm.userName, {expires: 30});
                        Cookies.set("password", encrypt(this.loginForm.password), {expires: 30});
                        Cookies.set('rememberMe', this.loginForm.rememberMe, {expires: 30});
                    } else {
                        Cookies.remove("userName");
                        Cookies.remove("password");
                        Cookies.remove('rememberMe');
                    }
                    this.$store.dispatch("Login", this.loginForm).then(() => {
                        this.$router.push({path: '/index'}).catch(() => {
                        });
                    }).catch(() => {
                        this.loading = false;
                    });
                }
            });
        }
    }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login-workbench {
    display: flex;
    min-height: 100vh;
    background: var(--pnkx-bg);
    color: var(--pnkx-text);

    // 左侧品牌展示区
    .login-brand {
        flex: 0 0 60%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--pnkx-bg-soft);
        border-right: 1px solid var(--pnkx-border);
        position: relative;
        overflow: hidden;

        .brand-content {
            text-align: center;
            color: var(--pnkx-text);
            z-index: 1;
            padding: var(--space-8);

            .brand-logo {
                width: 100px;
                height: 100px;
                border-radius: var(--pnkx-radius-lg);
                margin-bottom: var(--space-6);
                box-shadow: var(--pnkx-shadow-2);
                background: var(--pnkx-surface);
            }

            .brand-title {
                font-size: 44px;
                font-weight: var(--font-bold);
                margin-bottom: var(--space-2);
                letter-spacing: 0;
                color: var(--pnkx-text);
            }

            .brand-subtitle {
                font-size: var(--text-xl);
                font-weight: var(--font-medium);
                margin-bottom: var(--space-4);
                color: var(--pnkx-primary);
            }

            .brand-description {
                font-size: var(--text-base);
                color: var(--pnkx-text-secondary);
                max-width: 400px;
            }
        }
    }

    // 右侧登录表单区
    .login-form-wrapper {
        flex: 0 0 40%;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: var(--space-8);
        background: var(--pnkx-bg);

        .login-card {
            width: 100%;
            max-width: 420px;
            background: var(--pnkx-surface);
            border-radius: var(--pnkx-radius-lg);
            padding: var(--space-10);
            box-shadow: var(--pnkx-shadow-2);
            border: 1px solid var(--pnkx-border);

            .form-header {
                text-align: center;
                margin-bottom: var(--space-8);

                .form-title {
                    font-size: var(--text-2xl);
                    font-weight: var(--font-bold);
                    color: var(--pnkx-text);
                    margin-bottom: var(--space-2);
                }

                .form-subtitle {
                    font-size: var(--text-base);
                    color: var(--pnkx-text-secondary);
                }
            }

            .login-form {
                .el-form-item {
                    margin-bottom: var(--space-5);

                    .el-input {
                        &__inner {
                            height: 48px;
                            line-height: 48px;
                            padding-left: 45px;
                            font-size: var(--text-base);
                        }

                        &__prefix {
                            left: 15px;
                            font-size: var(--text-lg);
                            color: var(--pnkx-text-muted);
                        }

                        &__suffix {
                            right: 15px;

                            .password-toggle {
                                cursor: pointer;
                                color: var(--pnkx-text-muted);
                                font-size: var(--text-lg);

                                &:hover {
                                    color: var(--pnkx-primary);
                                }
                            }
                        }
                    }
                }

                .form-options {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: var(--space-6);

                    .el-checkbox {
                        &__label {
                            font-size: var(--text-sm);
                            color: var(--pnkx-text-secondary);
                        }
                    }
                }

                .login-btn {
                    width: 100%;
                    height: 48px;
                    font-size: var(--text-base);
                    font-weight: var(--font-semibold);
                    border-radius: var(--pnkx-radius-md);
                }
            }

            .form-footer {
                margin-top: var(--space-8);
                text-align: center;

                p {
                    font-size: var(--text-xs);
                    color: var(--pnkx-text-muted);
                }
            }
        }
    }

    // 移动端适配
    @media screen and (max-width: 992px) {
        flex-direction: column;

        .login-brand {
            flex: 0 0 auto;
            padding: var(--space-12) var(--space-6);

            .brand-content {
                .brand-logo {
                    width: 60px;
                    height: 60px;
                }

                .brand-title {
                    font-size: 32px;
                }

                .brand-description {
                    display: none;
                }
            }

        }

        .login-form-wrapper {
            flex: 1;
            padding: var(--space-6);

            .login-card {
                padding: var(--space-6);
                box-shadow: none;
                border: none;
                background: transparent;
            }
        }
    }
}
</style>
