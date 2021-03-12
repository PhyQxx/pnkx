<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" />

    <div class="right-menu">
      <template v-if="device!=='mobile'">

          <div class="notice textBox">
              <transition name="slide" v-if="noticeArr.length > 0">
                  <el-tooltip class="item" effect="dark" :content="notice.title" placement="bottom">
                      <p class="text" :key="notice.number" @click="toNoticeDetails(notice)">
                          <el-tag :type="notice.typeValue">{{notice.typeLabel}}</el-tag>
                          <span class="notice-content">{{notice.title}}</span>
                      </p>
                  </el-tooltip>
              </transition>
              <span class="no-notice" v-else>暂无最新通知公告</span>
          </div>

<!--        <search id="header-search" class="right-menu-item" />-->

<!--        <screenfull id="screenfull" class="right-menu-item hover-effect" />-->

<!--        <el-tooltip content="布局大小" effect="dark" placement="bottom">-->
<!--          <size-select id="size-select" class="right-menu-item hover-effect" />-->
<!--        </el-tooltip>-->

      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <i class="el-icon-caret-bottom" />
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>个人中心</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <span>布局设置</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'
import { getUserProfile } from "@/api/system/user";
import { getDicts } from "@/api/system/dict/data";
import { getUnreadNoticeList } from "@/api/system/notice";


export default {
  components: {
    Breadcrumb,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    RuoYiGit,
    RuoYiDoc
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
      notice() {
          return {
              number: this.number,
              id: this.noticeArr[this.number].noticeId,
              typeValue: this.dictValueToTagType(this.noticeArr[this.number].noticeType),
              typeLabel: this.getDictOne(this.noticeArr[this.number].noticeType, this.typeOptions),
              title: this.noticeArr[this.number].noticeTitle,
          }
      }
  },
    data() {
        return {
            websocket: {},
            noticeArr: [],
            number: 0,
            //通知类型字典项
            typeOptions: []
        }
    },
    mounted () {
        this.getDictList();
        this.getUnreadNoticeList();
        this.initWebSocket();
        this.startMove();
    },
    beforeDestroy () {
        this.onbeforeunload()
    },
  methods: {
      /**
       * 跳转到通知详情页面
       */
      toNoticeDetails(row) {
          if (this.$route.query.noticeId !== row.id.toString()) {
              this.$router.push({
                  path: 'noticedetail',
                  query: {
                      noticeId: row.id
                  }
              })
          }
      },
      /**
       * 获取未读通知公告
       */
      getUnreadNoticeList() {
          getUnreadNoticeList().then(res => {
              this.noticeArr = res;
          })
      },
      /**
       * 通知类型返回标签类型
       */
      dictValueToTagType(value) {
          let tagType = '';
          if (value === '1') {
              tagType = ''
          } else if (value === '2') {
              tagType = 'success'
          } else if (value === '3') {
              tagType = 'info'
          } else if (value === '4') {
              tagType = 'warning'
          } else if (value === '5') {
              tagType = 'danger'
          }
      },
      /**
       * 字典项翻译
       */
      getDictOne(value, list) {
          let label = '';
          try {
              list.forEach(item => {
                  if (item.dictValue === value) {
                      label = item.dictLabel
                  }
              });
          } catch (e) {
              console.log('异常：' + e)
          }
          return label
      },
      /**
       * 获取字典项公告类型
       */
      getDictList() {
          getDicts('sys_notice_type').then(res => {
              this.typeOptions = res.data;
          })
      },
      /**
       * 公告滚动
       */
      startMove () {
          let timer = setTimeout(() => {
              if (this.number === this.noticeArr.length-1) {
                  this.number = 0;
              } else {
                  this.number += 1;
              }
              this.startMove();
          }, 5000); // 滚动不需要停顿则将2000改成动画持续时间
      },
      /**
       * 初始化webSocket
       */
      initWebSocket () {
          // WebSocket
          getUserProfile().then(response => {
              if ('WebSocket' in window) {
                  this.websocket = new WebSocket('ws://192.168.20.218:8068/websocket/' + response.data.userId);
                  // 连接错误
                  this.websocket.onerror = this.setErrorMessage;
                  // 连接成功
                  this.websocket.onopen = this.setOnopenMessage;
                  // 收到消息的回调
                  this.websocket.onmessage = this.setOnmessageMessage;
                  // 连接关闭的回调
                  this.websocket.onclose = this.setOncloseMessage;
              } else {
                  alert('当前浏览器 Not support websocket')
              }
          });
          // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
          window.onbeforeunload = this.onbeforeunload;
      },
      setErrorMessage () {
          console.log('WebSocket连接发生错误   状态码：' + this.websocket.readyState)
      },
      setOnopenMessage () {
          console.log('WebSocket连接成功    状态码：' + this.websocket.readyState)
      },
      setOnmessageMessage (event) {
          console.log(event.data);
          // 根据服务器推送的消息做自己的业务处理
          this.noticeArr.push(JSON.parse(event.data))
      },
      setOncloseMessage () {
          console.log('WebSocket连接关闭    状态码：' + this.websocket.readyState)
      },
      onbeforeunload () {
          this.closeWebSocket()
      },
      closeWebSocket () {
          this.websocket.close()
      },
      toggleSideBar() {
          this.$store.dispatch('app/toggleSideBar')
      },
    async logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/homepage';
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
      display: flex;
      justify-content: center;
      align-items: center;
      .notice{
          width: 24rem!important;
          cursor: pointer;
      }
      .textBox {
          width: 100%;
          height: 40px;
          margin: 0 auto;
          overflow: hidden;
          position: relative;
          text-align: center;
      }
      .text {
          width: 100%;
          position: absolute;
          bottom: 0;
          height: 100%;
          margin-bottom: 0;
          display: flex;
          justify-content: center;
          align-items: center;
          .el-tag{
              width: 6rem!important;
          }
          .notice-content{
              width: calc(100% - 6rem);
              height: 1.5rem;
              line-height: 1.5rem;
              align-items: center;
              justify-content: center;
              text-align: left;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              text-indent: 1rem;
          }
      }
      .slide-enter-active, .slide-leave-active {
          transition: all 0.5s linear;
      }
      .slide-enter{
          transform: translateY(20px) scale(1);
          opacity: 1;
      }
      .slide-leave-to {
          transform: translateY(-20px) scale(0.8);
          opacity: 0;
      }
    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
