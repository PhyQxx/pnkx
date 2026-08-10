import { createRouter, createWebHistory } from 'vue-router'

/* Layout */
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'

/**
 * Note: 路由配置项
 *
 * hidden: true                   // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true               // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect           // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'             // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * meta : {
    noCache: true                // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'               // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'             // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false            // 如果设置为false，则不会在breadcrumb面包屑中显示
  }
 */

// 公共路由
export const constantRoutes = [
    {
        path: '/',
        name: 'index',
        hidden: true,
        redirect: '/login',
        component: () => import('@/views/index.vue'),
        children: []
    },
    {
        path: '/redirect',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '/redirect/:path(.*)',
                component: () => import('@/views/redirect.vue')
            }
        ]
    },
    {
        path: '/login',
        component: () => import('@/views/login.vue'),
        hidden: true
    },
    {
        path: '/404',
        component: () => import('@/views/error/404.vue'),
        hidden: true
    },
    {
        path: '/401',
        component: () => import('@/views/error/401.vue'),
        hidden: true
    },
    {
        path: '',
        component: Layout,
        redirect: 'index',
        children: [
            {
                path: 'index',
                component: () => import('@/views/index.vue'),
                name: '首页',
                meta: { title: '首页', icon: 'dashboard', noCache: true, affix: true }
            }
        ]
    },
    {
        path: '/user',
        component: Layout,
        hidden: true,
        redirect: 'noredirect',
        children: [
            {
                path: 'profile',
                component: () => import('@/views/system/user/profile/index.vue'),
                name: 'Profile',
                meta: { title: '个人中心', icon: 'user' }
            }
        ]
    },
    {
        path: '/system/file-manager',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/system/fileManager/index.vue'),
                name: 'FileManager',
                meta: { title: '文件管理器', icon: 'folder-opened' }
            }
        ]
    },
    {
        path: '/wallpaper-folder',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/px/life/wallpaper/folder.vue'),
                name: 'WallpaperFolder',
                meta: { title: '壁纸文件夹管理', icon: 'folder-opened', noCache: true }
            }
        ]
    },
    {
        path: '/wallpaper-record',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/px/life/wallpaper/record.vue'),
                name: 'WallpaperRecord',
                meta: { title: '壁纸操作记录', icon: 'data-line', noCache: true }
            }
        ]
    },
    {
        path: '/myapp',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'book/chapter',
                component: () => import('@/views/px/app/book/chapter.vue'),
                name: 'MyBookChapter',
                meta: { title: '章节列表', icon: 'document', noCache: true }
            },
            {
                path: 'book/reader/:chapterId(\\d+)',
                component: () => import('@/views/px/app/book/reader.vue'),
                name: 'MyBookReader',
                meta: { title: '章节阅读', icon: 'reading', noCache: true }
            }
        ]
    },
    {
        path: '/blog/friendlink/message',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/px/blog/friendlink/message.vue'),
                name: 'FriendlinkMessage',
                meta: {title: '友链留言管理', icon: 'message', noCache: true}
            }
        ]
    },
    {
        path: '/blog/video/message',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/px/blog/video/message.vue'),
                name: 'VideoMessage',
                meta: {title: '视频评论管理', icon: 'message', noCache: true}
            }
        ]
    },
    {
        path: '/blog/article/details/message',
        component: Layout,
        hidden: true,
        children: [
            {
                path: '',
                component: () => import('@/views/px/blog/article/details/message.vue'),
                name: 'ArticleMessage',
                meta: {title: '文章评论管理', icon: 'message', noCache: true}
            }
        ]
    },
    {
        path: '/job',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'log',
                component: () => import('@/views/monitor/job/log.vue'),
                name: 'JobLog',
                meta: { title: '调度日志' }
            }
        ]
    },
    {
        path: '/gen',
        component: Layout,
        hidden: true,
        children: [
            {
                path: 'edit/:tableId(\\d+)',
                component: () => import('@/views/tool/gen/editTable.vue'),
                name: 'GenEdit',
                meta: { title: '修改生成配置' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes: constantRoutes,
    scrollBehavior() {
        return {
            left: 0,
            top: 0
        }
    }
})

export default router
