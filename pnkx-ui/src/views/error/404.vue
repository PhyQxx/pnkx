<template>
    <div class="error-page error-404">
        <div class="error-content">
            <div class="error-code">
                <span class="code-number">4</span>
                <span class="code-icon">
                    <el-icon><QuestionFilled /></el-icon>
                </span>
                <span class="code-number">4</span>
            </div>
            <h1 class="error-title">页面未找到</h1>
            <p class="error-description">
                抱歉，您访问的页面不存在或已被移除。<br>
                请检查 URL 是否正确，或返回首页继续浏览。
            </p>
            <div class="error-actions">
                <router-link to="/">
                    <el-button type="primary" size="default" icon="HomeFilled">
                        返回首页
                    </el-button>
                </router-link>
                <el-button size="default" icon="Back" @click="$router.go(-1)">
                    返回上一页
                </el-button>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'Page404'
}
</script>

<style lang="scss" scoped>
.error-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-body);
    padding: var(--space-6);

    .error-content {
        text-align: center;
        max-width: 500px;
        animation: fadeIn 0.5s ease-out;

        .error-code {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: var(--space-4);
            margin-bottom: var(--space-6);

            .code-number {
                font-size: 120px;
                font-weight: var(--font-bold);
                background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                line-height: 1;
            }

            .code-icon {
                width: 100px;
                height: 100px;
                display: flex;
                align-items: center;
                justify-content: center;
                background: var(--bg-hover);
                border-radius: var(--radius-xl);

                i {
                    font-size: 60px;
                    color: var(--color-primary);
                }
            }
        }

        .error-title {
            font-size: var(--text-3xl);
            font-weight: var(--font-bold);
            color: var(--text-primary);
            margin-bottom: var(--space-4);
        }

        .error-description {
            font-size: var(--text-base);
            color: var(--text-secondary);
            line-height: 1.6;
            margin-bottom: var(--space-8);
        }

        .error-actions {
            display: flex;
            gap: var(--space-4);
            justify-content: center;

            .el-button {
                min-width: 120px;
            }
        }
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
}

@media screen and (max-width: 768px) {
    .error-page {
        .error-content {
            .error-code {
                .code-number {
                    font-size: 80px;
                }

                .code-icon {
                    width: 70px;
                    height: 70px;

                    i {
                        font-size: 40px;
                    }
                }
            }

            .error-actions {
                flex-direction: column;
                align-items: center;
            }
        }
    }
}
</style>
